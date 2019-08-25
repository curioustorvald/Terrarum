package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.concurrent.mapToThreadPoolDirectly
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.inUse
import kotlin.math.cos
import kotlin.math.sin

const val WIDTH = 1536
const val HEIGHT = 1024
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

    private val RNG = HQRNG()
    private var seed = 10000L

    private var generationDone = false
    private var generateKeyLatched = false

    private var generationTimeInMeasure = false
    private var generationStartTime = 0L

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera(WIDTH.toFloat(), HEIGHT.toFloat())
        camera.setToOrtho(false) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined
        Gdx.gl20.glViewport(0, 0, WIDTH, HEIGHT)

        testTex = Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888)
        testTex.blending = Pixmap.Blending.None
        tempTex = Texture(1, 1, Pixmap.Format.RGBA8888)

        println("Init done")
    }

    override fun render() {
        if (!generationDone) {
            joise = getNoiseGenerator(seed)
            renderNoise()
        }

        // draw using pixmap
        batch.inUse {
            tempTex.dispose()
            tempTex = Texture(testTex)
            batch.draw(tempTex, 0f, 0f)
        }

        // read key input
        if (!generateKeyLatched && Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            generateKeyLatched = true
            seed = RNG.nextLong()
            joise = getNoiseGenerator(seed)
            renderNoise()
        }

        // check if generation is done
        if (ThreadParallel.allFinished()) {
            generateKeyLatched = false
        }

        // finish time measurement
        if (ThreadParallel.allFinished() && generationTimeInMeasure) {
            generationTimeInMeasure = false
            val time = System.nanoTime() - generationStartTime
            println("Generation time in sec: ${time / 1000000000f}")
        }
    }

    private val NOISE_MAKER = AccidentalCave

    private fun getNoiseGenerator(SEED: Long): Joise {
        return NOISE_MAKER.getGenerator(SEED)
    }

    val colourNull = Color(0x1b3281ff)

    private fun renderNoise() {
        generationStartTime = System.nanoTime()
        generationTimeInMeasure = true

        // erase first
        testTex.setColor(colourNull)
        testTex.fill()

        // render noisemap to pixmap
        (0 until WIDTH).mapToThreadPoolDirectly("NoiseGen") { range ->
            for (y in 0 until HEIGHT) {
                for (x in range) {
                    val sampleDensity = NOISE_MAKER.sampleDensity
                    val sampleTheta = (x.toDouble() / WIDTH) * TWO_PI
                    val sampleOffset = (WIDTH / sampleDensity) / 8.0
                    val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                    val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                    val sampleY = y / sampleDensity
                    val noise = joise.get(sampleX, sampleY, sampleZ)

                    NOISE_MAKER.draw(x, y, noise, testTex)
                }
            }
        }

        ThreadParallel.startAll()

        generationDone = true
    }
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false
    appConfig.width = WIDTH
    appConfig.height = HEIGHT
    appConfig.backgroundFPS = 60
    appConfig.foregroundFPS = 60
    appConfig.forceExit = false

    LwjglApplication(WorldgenNoiseSandbox(), appConfig)
}

interface NoiseMaker {
    fun draw(x: Int, y: Int, noiseValue: Double, outTex: Pixmap)
    fun getGenerator(seed: Long): Joise
    val sampleDensity: Double // bigger: larger features. IT IS RECOMMENDED TO SET THIS VALUE SAME AS THE CANVAS SIZE
                              //         so that the whole world can have noise coord of 0.0 to 1.0 on Y-axis
                              //         attempting to adjust the feature size with this is a dirty hack and may not be
                              //         supported by the world generator.
}

object BiomeMaker : NoiseMaker {

    override val sampleDensity = HEIGHT.toDouble() // 24: magic number from old code

    override fun draw(x: Int, y: Int, noiseValue: Double, outTex: Pixmap) {
        val colPal = biomeColors
        val control = noiseValue.times(colPal.size).minus(0.00001f).toInt().fmod(colPal.size)

        outTex.setColor(colPal[control])
        //testTex.setColor(RNG.nextFloat(), RNG.nextFloat(), RNG.nextFloat(), 1f)
        outTex.drawPixel(x, y)
    }

    override fun getGenerator(seed: Long): Joise {
        //val biome = ModuleBasisFunction()
        //biome.setType(ModuleBasisFunction.BasisType.SIMPLEX)

        // simplex AND fractal for more noisy edges, mmmm..!
        val fractal = ModuleFractal()
        fractal.setType(ModuleFractal.FractalType.MULTI)
        fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
        fractal.setNumOctaves(4)
        fractal.setFrequency(1.0)
        fractal.seed = seed shake 0x7E22A

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
    val biomeColors = intArrayOf(
            //0x2288ccff.toInt(), // ísland
            0x229944ff.toInt(), // woodlands
            0x77bb77ff.toInt(), // shrubland
            0x88bb66ff.toInt(), // plains
            0x888888ff.toInt() // rockyland
    )
}

// http://accidentalnoise.sourceforge.net/minecraftworlds.html
object AccidentalCave : NoiseMaker {

    override val sampleDensity = 333.0 // fixed value for fixed size of features

    private val notationColours = arrayOf(
            Color.WHITE,
            Color.MAGENTA,
            Color(0f, 186f/255f, 1f, 1f),
            Color(.5f, 1f, .5f, 1f),
            Color(1f, 0.93f, 0.07f, 1f),
            Color(0.97f, 0.6f, 0.56f, 1f)
    )

    override fun draw(x: Int, y: Int, noiseValue: Double, outTex: Pixmap) {
        val c = noiseValue.toFloat()
        val selector = c.minus(0.0001).floorInt() fmod notationColours.size
        val selecteeColour = Color(c - selector, c - selector, c - selector, 1f)
        if (noiseValue < 0) {
            outTex.setColor(-c, 0f, 0f, 1f)
        }
        else {
            outTex.setColor(selecteeColour.mul(notationColours[selector]))
        }
        outTex.drawPixel(x, y)
    }

    override fun getGenerator(seed: Long): Joise {
        val lowlandMagic: Long = 0x41A21A114DBE56 // Maria Lindberg
        val highlandMagic: Long = 0x0114E091      // Olive Oyl
        val mountainMagic: Long = 0x115AA4DE2504  // Lisa Anderson
        val selectionMagic: Long = 0x41E10D9B100  // Melody Blue

        val caveMagic: Long = 0x00215741CDF // Urist McDF
        val cavePerturbMagic: Long = 0xA2410C // Armok
        val caveBlockageMagic: Long = 0xD15A57E5 // Disaster


        val groundGradient = ModuleGradient()
        groundGradient.setGradient(0.0, 0.0, 0.0, 1.0)

        /* lowlands */

        val lowlandShapeFractal = ModuleFractal()
        lowlandShapeFractal.setType(ModuleFractal.FractalType.BILLOW)
        lowlandShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        lowlandShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        lowlandShapeFractal.setNumOctaves(2)
        lowlandShapeFractal.setFrequency(0.25)
        lowlandShapeFractal.seed = seed shake lowlandMagic

        val lowlandAutoCorrect = ModuleAutoCorrect()
        lowlandAutoCorrect.setSource(lowlandShapeFractal)
        lowlandAutoCorrect.setLow(0.0)
        lowlandAutoCorrect.setHigh(1.0)

        val lowlandScale = ModuleScaleOffset()
        lowlandScale.setScale(0.125)
        lowlandScale.setOffset(-0.65) // TODO linearly alters the height

        val lowlandYScale = ModuleScaleDomain()
        lowlandYScale.setSource(lowlandScale)
        lowlandYScale.setScaleY(0.02) // greater = more distortion, overhangs

        val lowlandTerrain = ModuleTranslateDomain()
        lowlandTerrain.setSource(groundGradient)
        lowlandTerrain.setAxisYSource(lowlandYScale)

        /* highlands */

        val highlandShapeFractal = ModuleFractal()
        highlandShapeFractal.setType(ModuleFractal.FractalType.FBM)
        highlandShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        highlandShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        highlandShapeFractal.setNumOctaves(4)
        highlandShapeFractal.setFrequency(2.0)
        highlandShapeFractal.seed = seed shake highlandMagic

        val highlandAutocorrect = ModuleAutoCorrect()
        highlandAutocorrect.setSource(highlandShapeFractal)
        highlandAutocorrect.setLow(-1.0)
        highlandAutocorrect.setHigh(1.0)

        val highlandScale = ModuleScaleOffset()
        highlandScale.setSource(highlandAutocorrect)
        highlandScale.setScale(0.25)
        highlandScale.setOffset(-0.2) // TODO linearly alters the height

        val highlandYScale = ModuleScaleDomain()
        highlandYScale.setSource(highlandScale)
        highlandYScale.setScaleY(0.14) // greater = more distortion, overhangs

        val highlandTerrain = ModuleTranslateDomain()
        highlandTerrain.setSource(groundGradient)
        highlandTerrain.setAxisYSource(highlandYScale)

        /* mountains */

        val mountainShapeFractal = ModuleFractal()
        mountainShapeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        mountainShapeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        mountainShapeFractal.setNumOctaves(8)
        mountainShapeFractal.setFrequency(1.0)
        mountainShapeFractal.seed = seed shake mountainMagic

        val mountainAutocorrect = ModuleAutoCorrect()
        mountainAutocorrect.setSource(mountainShapeFractal)
        mountainAutocorrect.setLow(-1.0)
        mountainAutocorrect.setHigh(1.0)

        val mountainScale = ModuleScaleOffset()
        mountainScale.setSource(mountainAutocorrect)
        mountainScale.setScale(0.45)
        mountainScale.setOffset(-0.1) // TODO linearly alters the height

        val mountainYScale = ModuleScaleDomain()
        mountainYScale.setSource(mountainScale)
        mountainYScale.setScaleY(0.7) // greater = more distortion, overhangs

        val mountainTerrain = ModuleTranslateDomain()
        mountainTerrain.setSource(groundGradient)
        mountainTerrain.setAxisYSource(mountainYScale)

        /* selection */

        val terrainTypeFractal = ModuleFractal()
        terrainTypeFractal.setType(ModuleFractal.FractalType.FBM)
        terrainTypeFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        terrainTypeFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        terrainTypeFractal.setNumOctaves(3)
        terrainTypeFractal.setFrequency(0.125)
        terrainTypeFractal.seed = seed shake selectionMagic

        val terrainAutocorrect = ModuleAutoCorrect()
        terrainAutocorrect.setSource(terrainTypeFractal)
        terrainAutocorrect.setLow(0.0)
        terrainAutocorrect.setHigh(1.0)

        val terrainTypeYScale = ModuleScaleDomain()
        terrainTypeYScale.setSource(terrainAutocorrect)
        terrainTypeYScale.setScaleY(0.0)

        val terrainTypeCache = ModuleCache()
        terrainTypeCache.setSource(terrainTypeYScale)

        val highlandMountainSelect = ModuleSelect()
        highlandMountainSelect.setLowSource(highlandTerrain)
        highlandMountainSelect.setHighSource(mountainTerrain)
        highlandMountainSelect.setControlSource(terrainTypeCache)
        highlandMountainSelect.setThreshold(0.55)
        highlandMountainSelect.setFalloff(0.2)

        val highlandLowlandSelect = ModuleSelect()
        highlandLowlandSelect.setLowSource(lowlandTerrain)
        highlandLowlandSelect.setHighSource(highlandMountainSelect)
        highlandLowlandSelect.setControlSource(terrainTypeCache)
        highlandLowlandSelect.setThreshold(0.25)
        highlandLowlandSelect.setFalloff(0.15)

        val highlandLowlandSelectCache = ModuleCache()
        highlandLowlandSelectCache.setSource(highlandLowlandSelect)

        val groundSelect = ModuleSelect()
        groundSelect.setLowSource(0.0)
        groundSelect.setHighSource(1.0)
        groundSelect.setThreshold(0.5)
        groundSelect.setControlSource(highlandLowlandSelectCache)

        /* caves */

        val caveShape = ModuleFractal()
        caveShape.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveShape.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveShape.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveShape.setNumOctaves(1)
        caveShape.setFrequency(7.4) // TODO adjust the "density" of the caves
        caveShape.seed = seed shake caveMagic

        val caveAttenuateBias = ModuleBias()
        caveAttenuateBias.setSource(highlandLowlandSelectCache)
        caveAttenuateBias.setBias(0.90) // TODO (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids

        val caveShapeAttenuate = ModuleCombiner()
        caveShapeAttenuate.setType(ModuleCombiner.CombinerType.MULT)
        caveShapeAttenuate.setSource(0, caveShape)
        caveShapeAttenuate.setSource(1, caveAttenuateBias)

        val cavePerturbFractal = ModuleFractal()
        cavePerturbFractal.setType(ModuleFractal.FractalType.FBM)
        cavePerturbFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        cavePerturbFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        cavePerturbFractal.setNumOctaves(6)
        cavePerturbFractal.setFrequency(3.0)
        cavePerturbFractal.seed = seed shake cavePerturbMagic

        val cavePerturbScale = ModuleScaleOffset()
        cavePerturbScale.setSource(cavePerturbFractal)
        cavePerturbScale.setScale(0.45)
        cavePerturbScale.setOffset(0.0)

        val cavePerturb = ModuleTranslateDomain()
        cavePerturb.setSource(caveShapeAttenuate)
        cavePerturb.setAxisXSource(cavePerturbScale)

        val caveSelect = ModuleSelect()
        caveSelect.setLowSource(1.0)
        caveSelect.setHighSource(0.0)
        caveSelect.setControlSource(cavePerturb)
        caveSelect.setThreshold(0.89) // TODO also adjust this if you've touched the bias value. Number can be greater than 1.0
        caveSelect.setFalloff(0.0)

        val caveBlockageFractal = ModuleFractal()
        caveBlockageFractal.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveBlockageFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveBlockageFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveBlockageFractal.setNumOctaves(2)
        caveBlockageFractal.setFrequency(8.88) // TODO same as caveShape frequency?
        caveBlockageFractal.seed = seed shake caveBlockageMagic

        // will only close-up deeper caves. Shallow caves will be less likely to be closed up
        val caveBlockageAttenuate = ModuleCombiner()
        caveBlockageAttenuate.setType(ModuleCombiner.CombinerType.MULT)
        caveBlockageAttenuate.setSource(0, caveBlockageFractal)
        caveBlockageAttenuate.setSource(1, caveAttenuateBias)

        val caveBlockageSelect = ModuleSelect()
        caveBlockageSelect.setLowSource(0.0)
        caveBlockageSelect.setHighSource(1.0)
        caveBlockageSelect.setControlSource(caveBlockageAttenuate)
        caveBlockageSelect.setThreshold(1.40) // TODO adjust cave cloing-up strength. Larger = more closing
        caveBlockageSelect.setFalloff(0.0)

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner()
        caveInMix.setType(ModuleCombiner.CombinerType.ADD)
        caveInMix.setSource(0, caveSelect)
        caveInMix.setSource(1, caveBlockageSelect)

        val groundCaveMult = ModuleCombiner()
        groundCaveMult.setType(ModuleCombiner.CombinerType.MULT)
        groundCaveMult.setSource(0, caveInMix)
        //groundCaveMult.setSource(0, caveSelect) // disables the cave-in for quick cavegen testing
        groundCaveMult.setSource(1, groundSelect)

        // this noise tree WILL generate noise value greater than 1.0
        // they should be treated properly when you actually generate the world out of the noisemap
        // for the visualisation, no treatment will be done in this demo app.

        val finalClamp = ModuleClamp()
        finalClamp.setRange(0.0, 1.0)
        finalClamp.setSource(groundCaveMult)

        //return Joise(caveInMix)
        return Joise(finalClamp)
    }
}



infix fun Long.shake(other: Long): Long {
    var s0 = this
    var s1 = other

    s1 = s1 xor s0
    s0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14)
    s1 = s1 shl 36 or s1.ushr(28)

    return s0 + s1
}