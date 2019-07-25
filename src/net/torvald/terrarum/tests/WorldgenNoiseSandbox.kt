package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleFractal
import com.sudoplay.joise.module.ModuleScaleDomain
import net.torvald.random.HQRNG
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.inUse
import kotlin.math.cos
import kotlin.math.sin

const val WIDTH = 1536
const val HEIGHT = 512
const val TWO_PI = Math.PI * 2

/**
 * Created by minjaesong on 2019-07-23.
 */
class WorldgenNoiseSandbox : ApplicationAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var testTex: Pixmap
    private lateinit var tempTex: Texture

    private lateinit var joise: Joise

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera(WIDTH.toFloat(), HEIGHT.toFloat())
        camera.setToOrtho(false) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined
        Gdx.gl20.glViewport(0, 0, WIDTH, HEIGHT)

        testTex = Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888)
        tempTex = Texture(1, 1, Pixmap.Format.RGBA8888)

        joise = generateNoise()
        renderNoise()

        println("Init done")
    }

    override fun render() {
        // draw using pixmap
        batch.inUse {
            tempTex.dispose()
            tempTex = Texture(testTex)
            batch.draw(tempTex, 0f, 0f)
        }
    }

    private val RNG = HQRNG()
    private var seed = RNG.nextLong()

    private fun generateNoise(): Joise {
        //val biome = ModuleBasisFunction()
        //biome.setType(ModuleBasisFunction.BasisType.SIMPLEX)

        // simplex AND fractal for more noisy edges, mmmm..!
        val fractal = ModuleFractal()
        fractal.setType(ModuleFractal.FractalType.MULTI)
        fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
        fractal.setNumOctaves(4)
        fractal.setFrequency(1.0)

        val autocorrect = ModuleAutoCorrect()
        autocorrect.setSource(fractal)
        autocorrect.setRange(0.0, 1.0)

        val scale = ModuleScaleDomain()
        scale.setSource(autocorrect)
        scale.setScaleX(0.3)
        scale.setScaleY(0.3)
        scale.setScaleZ(0.3)

        val last = scale

        return Joise(last)
    }

    // with this method, only TWO distinct (not bland) biomes are possible. CLUT order is important here.
    private val biomeColors = intArrayOf(
            //0x2288ccff.toInt(), // ísland
            0x229944ff.toInt(), // woodlands
            0x77bb77ff.toInt(), // shrubland
            0x88bb66ff.toInt(), // plains
            0x888888ff.toInt() // rockyland
    )

    private fun renderNoise() {
        // render noisemap to pixmap
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val sampleDensity = 48.0 / 2 // 48.0: magic number from old code
                val sampleTheta = (x.toDouble() / WIDTH) * TWO_PI
                val sampleOffset = (WIDTH / sampleDensity) / 8.0
                val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                val sampleY = y / sampleDensity
                val noise: Float = joise.get(sampleX, sampleY, sampleZ).toFloat()

                val control = noise.times(biomeColors.size).minus(0.00001f).toInt().fmod(biomeColors.size)

                testTex.setColor(biomeColors[control])
                //testTex.setColor(RNG.nextFloat(), RNG.nextFloat(), RNG.nextFloat(), 1f)
                testTex.drawPixel(x, y)
            }
        }
    }
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false
    appConfig.width = WIDTH
    appConfig.height = HEIGHT
    appConfig.backgroundFPS = 10
    appConfig.foregroundFPS = 10
    appConfig.forceExit = false

    LwjglApplication(WorldgenNoiseSandbox(), appConfig)
}