package net.torvald.terrarum.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
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
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.concurrent.BlockingThreadPool
import net.torvald.terrarum.concurrent.ParallelUtils.sliceEvenly
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.roundInt
import kotlin.math.absoluteValue

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
        camera = OrthographicCamera(AppLoader.setWindowWidth.toFloat(), AppLoader.setWindowHeight.toFloat())

        camera.setToOrtho(true, AppLoader.setWindowWidth.toFloat(), AppLoader.setWindowHeight.toFloat())
        camera.update()
        Gdx.gl20.glViewport(0, 0, AppLoader.setWindowWidth, AppLoader.setWindowHeight)

        pixmap = Pixmap(IMAGE_SIZE, IMAGE_SIZE, Pixmap.Format.RGBA8888)
        texture = Texture(1, 1, Pixmap.Format.RGBA8888)

        batch.projectionMatrix = camera.combined

        println("Test runs: ${testSets.size * samplingCount}")
        println("Warmup runs: $warmupTries")
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


        updateTestGovernor(delta)


        // regen
        if (timerFired && BlockingThreadPool.allFinished()) {
            timerFired = false

            totalTestsDone += 1
        }

        if (regenerate && BlockingThreadPool.allFinished()) {
            //printdbg(this, "Reticulating splines...")

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

            batch.color = Color.CYAN
            Terrarum.fontGame.draw(batch, "Tests: $totalTestsDone / ${testSets.size * samplingCount}", 10f, 10f)
        }

    }

    //private val testSets = listOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144, 8192)//, 12288, 16384, 24576, 32768, 49152, 65536)
    private val testSets = listOf(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16)
    private val samplingCount = 20
    private var totalTestsDone = 0
    private val rawTimerRecords = ArrayList<Long>()

    private var warmupDone = false
    private val warmupTries = (testSets.size * samplingCount) / 4
    private var constructOnce = false

    private val timeWhenTestBegun = System.currentTimeMillis()

    private var wholeJobTimer = 0L

    private fun updateTestGovernor(delta: Float) {
        // cut the warm-up {
        if (!warmupDone && totalTestsDone >= warmupTries) {
            println("######## WARMUP DONE, THE TEST BEGINS HERE ########")
            totalTestsDone = 0
            //rawTimerRecords.clear()
            warmupDone = true
        }


        // time to end the test
        if (totalTestsDone == testSets.size * samplingCount) {
            println("Test completed:")
            println("Total tests done = $totalTestsDone")

            // print a table
            println("Timer raw:")
            rawTimerRecords.forEachIndexed { index, l ->
                if (index < rawTimerRecords.size - testSets.size) {
                    println("* $l")
                }
                else {
                    println("$l")
                }
            }

            // k thx bye
            val timeWasted = (System.currentTimeMillis() - timeWhenTestBegun) / 1000
            println("Total testing time: " +
                    "${timeWasted.div(60).toString().padStart(2, '0')}:" +
                    "${timeWasted.rem(60).toString().padStart(2, '0')}")

            System.exit(0)
        }
        // time to construct a new test
        if (totalTestsDone % samplingCount == 0 && BlockingThreadPool.allFinished()) {
            pixelsInSingleJob = (IMAGE_SIZE * IMAGE_SIZE) / testSets[totalTestsDone / samplingCount]


            if (!constructOnce) {
                if (warmupDone)
                    println("Preparing test for ${testSets[totalTestsDone / samplingCount]} task sets")
                else
                    println("This is warm-up, task sets: $${testSets[totalTestsDone / samplingCount]}")

                val endTime = System.currentTimeMillis()
                rawTimerRecords.add(endTime - wholeJobTimer)
                println("> Timer end: $endTime")
                wholeJobTimer = endTime
                println("> Timer start: $wholeJobTimer")

                constructOnce = true
            }
        }

        // auto-press SPACE
        if (BlockingThreadPool.allFinished()) {
            regenerate = true
            constructOnce = false
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

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(1024, 1024)

    Lwjgl3Application(AppLoader(appConfig, NoiseGenerator(), 1024, 1024), appConfig)
}