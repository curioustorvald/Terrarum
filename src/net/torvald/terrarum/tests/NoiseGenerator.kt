package net.torvald.terrarum.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleFractal
import com.sudoplay.joise.module.ModuleScaleDomain
import com.sudoplay.joise.module.ModuleScaleOffset
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.concurrent.BlockingThreadPool
import net.torvald.terrarum.concurrent.RunnableFun
import net.torvald.terrarum.concurrent.ParallelUtils.sliceEvenly
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.roundInt
import kotlin.math.absoluteValue
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Created by minjaesong on 2018-12-14.
 */
class NoiseGenerator : ScreenAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var camera: OrthographicCamera
    lateinit var pixmap: Pixmap
    lateinit var texture: Texture
    private val IMAGE_SIZE = 1024
    private val IMAGE_SIZEF = IMAGE_SIZE.toFloat()
    private val IMAGE_SIZED = IMAGE_SIZE.toDouble()
    private val RNG = HQRNG()

    override fun show() {
        Gdx.input.inputProcessor = NoiseGeneratorController(this)

        batch = SpriteBatch()
        camera = OrthographicCamera(AppLoader.appConfig.width.toFloat(), AppLoader.appConfig.height.toFloat())

        camera.setToOrtho(true, AppLoader.appConfig.width.toFloat(), AppLoader.appConfig.height.toFloat())
        camera.update()
        Gdx.gl20.glViewport(0, 0, AppLoader.appConfig.width, AppLoader.appConfig.height)

        pixmap = Pixmap(IMAGE_SIZE, IMAGE_SIZE, Pixmap.Format.RGBA8888)
        texture = Texture(1, 1, Pixmap.Format.RGBA8888)
    }

    var regenerate = true

    private var pixelsInSingleJob = (IMAGE_SIZE * IMAGE_SIZE) / 16 // CHANGE THIS VALUE HERE

    private val jobsCount: Int
        get() = (IMAGE_SIZE * IMAGE_SIZE) / pixelsInSingleJob
    private val rawPixelsList: List<IntRange>
        get() = (0 until IMAGE_SIZE * IMAGE_SIZE).sliceEvenly(jobsCount)
    private fun makeGenFun(seed: Long, index: Int) = { //i: Int ->
        val module = ModuleFractal()
        module.setType(ModuleFractal.FractalType.BILLOW)
        module.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        module.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        module.setNumOctaves(10)
        module.setFrequency(8.0)
        module.seed = seed

        val moduleScale = ModuleScaleOffset()
        moduleScale.setSource(module)
        moduleScale.setScale(0.5)
        moduleScale.setOffset(0.0)

        val noiseModule = Joise(moduleScale)

        for (c in rawPixelsList[index]) {
            val x = c % IMAGE_SIZE
            val y = c / IMAGE_SIZE
            val uvX = x / IMAGE_SIZED
            val uvY = y / IMAGE_SIZED

            val noiseValue = noiseModule.get(uvX, uvY).absoluteValue
            val rgb = (noiseValue * 255.0).roundInt()

            pixmap.drawPixel(x, y, (rgb shl 24) or (rgb shl 16) or (rgb shl 8) or 0xFF)
        }
    }

    private var timerStart = 0L
    private var timerFired = false

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(Ingame.getCanonicalTitle())

        // regen
        if (timerFired && BlockingThreadPool.allFinished()) {
            val timeTook = System.currentTimeMillis() - timerStart
            timerFired = false

            printdbg(this, "> $timeTook ms")
        }

        if (regenerate && BlockingThreadPool.allFinished()) {
            printdbg(this, "Reticulating splines...")

            regenerate = false
            // don't join while rendering noise

            timerStart = System.currentTimeMillis()
            timerFired = true

            val seed = RNG.nextLong()
            val jobs = List(jobsCount) { makeGenFun(seed, it) }
            BlockingThreadPool.setTasks(jobs, "")
            BlockingThreadPool.startAllWaitForDie()
        }


        // render
        texture.dispose()
        texture = Texture(pixmap)

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(texture, 0f, 0f)
        }

    }

    override fun pause() {
        super.pause()
    }

    override fun resume() {
        super.resume()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        pixmap.dispose()
        texture.dispose()
    }
}

class NoiseGeneratorController(val host: NoiseGenerator) : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.SPACE) {
            host.regenerate = true
        }
        return true
    }
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false//true;
    appConfig.width = 1024
    appConfig.height = 1024
    appConfig.backgroundFPS = 9999
    appConfig.foregroundFPS = 9999
    appConfig.forceExit = false

    LwjglApplication(AppLoader(appConfig, NoiseGenerator()), appConfig)
}