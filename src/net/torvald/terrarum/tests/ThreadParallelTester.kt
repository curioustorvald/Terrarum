package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
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
import com.sudoplay.joise.module.ModuleScaleOffset
import net.torvald.random.HQRNG
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.concurrent.ThreadableFun
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.inUse
import net.torvald.terrarum.roundInt
import kotlin.math.absoluteValue
import kotlin.system.measureNanoTime

/**
 * Created by minjaesong on 2019-06-17.
 */
class ThreadParallelTester : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var camera: OrthographicCamera
    lateinit var pixmap: Pixmap
    lateinit var texture: Texture
    private val IMAGE_SIZE = 1024
    private val IMAGE_SIZEF = IMAGE_SIZE.toFloat()
    private val IMAGE_SIZED = IMAGE_SIZE.toDouble()
    private val RNG = HQRNG()

    override fun create() {
        Gdx.input.inputProcessor = ThreadParallelTesterController(this)

        batch = SpriteBatch()
        camera = OrthographicCamera(IMAGE_SIZEF, IMAGE_SIZEF)

        camera.setToOrtho(true, IMAGE_SIZEF, IMAGE_SIZEF)
        camera.update()
        Gdx.gl20.glViewport(0, 0, IMAGE_SIZE, IMAGE_SIZE)

        pixmap = Pixmap(IMAGE_SIZE, IMAGE_SIZE, Pixmap.Format.RGBA8888)
        texture = Texture(1, 1, Pixmap.Format.RGBA8888)

        batch.projectionMatrix = camera.combined

        println("[ThreadParallelTester] Hello, world!")
    }

    var regenerate = true
    var generateDone = true

    override fun render() {
        Gdx.graphics.setTitle("F: ${Gdx.graphics.framesPerSecond}")

        if (regenerate) {
            // fill pixmap with slow-to-calculate noises.
            // create parallel job
            // run parallel job, wait all of them to die
            // render the pixmap

            // expected result: app freezes (or nothing is drawn) until all the parallel job is done

            println("Noise regenerating...")
            regenerate = false
            generateDone = false
            val time = measureNanoTime {
                setupParallelJobs()
                //ThreadParallel.startAll()
                ThreadParallel.startAllWaitForDie()
            }
            generateDone = true
            println("Noise generation complete, took ${time.toDouble() / 1_000_000} ms\n")
        }

        // render
        texture.dispose()
        texture = Texture(pixmap)

        batch.inUse {
            batch.projectionMatrix = camera.combined
            batch.color = Color.WHITE
            batch.draw(texture, 0f, 0f)
        }
    }

    private fun setupParallelJobs() {
        val seed = RNG.nextLong()
        for (i in 0 until ThreadParallel.threadCount) {
            ThreadParallel.map(i, "NoiseGen", makeGenFun(seed, i))
        }
    }

    private val scanlineNumbers: List<IntProgression> = (0 until IMAGE_SIZE).sliceEvenly(ThreadParallel.threadCount)
    private fun makeGenFun(seed: Long, index: Int): ThreadableFun = { i ->
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

        for (y in scanlineNumbers[index]) {
            for (x in 0 until IMAGE_SIZE) {
                val uvX = x / IMAGE_SIZED
                val uvY = y / IMAGE_SIZED

                val noiseValue = noiseModule.get(uvX, uvY).absoluteValue
                val rgb = (noiseValue * 255.0).roundInt()

                pixmap.drawPixel(x, y, (rgb shl 24) or (rgb shl 16) or (rgb shl 8) or 0xFF)
            }
        }
    }

    override fun dispose() {
        pixmap.dispose()
        texture.dispose()
    }
}

class ThreadParallelTesterController(val host: ThreadParallelTester) : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.SPACE && host.generateDone) {
            host.regenerate = true
        }
        return true
    }
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false
    appConfig.width = 1024
    appConfig.height = 1024
    appConfig.backgroundFPS = 9999
    appConfig.foregroundFPS = 9999

    appConfig.forceExit = true

    //LwjglApplication(AppLoader(appConfig, ThreadParallelTester()), appConfig)
    LwjglApplication(ThreadParallelTester(), appConfig)
}