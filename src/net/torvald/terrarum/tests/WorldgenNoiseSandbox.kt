package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.DefaultGL32Shaders
import net.torvald.terrarum.FlippingSpriteBatch
import net.torvald.terrarum.concurrent.RunnableFun
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.worldgenerator.BiomegenParams
import net.torvald.terrarum.modulebasegame.worldgenerator.OregenParams
import net.torvald.terrarum.modulebasegame.worldgenerator.TerragenParams
import net.torvald.terrarum.modulebasegame.worldgenerator.shake
import net.torvald.terrarum.worlddrawer.toRGBA
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import java.util.concurrent.Future
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

const val NOISEBOX_WIDTH = 1024
const val NOISEBOX_HEIGHT = 768
const val TWO_PI = Math.PI * 2

/**
 * Created by minjaesong on 2019-07-23.
 */
class WorldgenNoiseSandbox : ApplicationAdapter() {

    private val threadExecutor = ThreadExecutor()

    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var font: BitmapFont

    private lateinit var testTex: Pixmap
    private lateinit var tempTex: Texture

    private val RNG = HQRNG()
    private var seed = 10000L

    private var initialGenDone = false
    private var generateKeyLatched = false

    private var generationTimeInMeasure = false
    private var generationStartTime = 0L
    private var genSlices: Int = 0
    private var genFutures: Array<Future<*>?> = arrayOfNulls(genSlices)

    override fun create() {
        font = TerrarumSansBitmap("assets/graphics/fonts/terrarum-sans-bitmap")

        batch = FlippingSpriteBatch(1000)
        camera = OrthographicCamera(NOISEBOX_WIDTH.toFloat(), NOISEBOX_HEIGHT.toFloat())
        camera.setToOrtho(true) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined

        testTex = Pixmap(NOISEBOX_WIDTH, NOISEBOX_HEIGHT, Pixmap.Format.RGBA8888)
        testTex.blending = Pixmap.Blending.None
        tempTex = Texture(1, 1, Pixmap.Format.RGBA8888)

        genSlices = max(threadExecutor.threadCount, testTex.width / 8)

        println("Init done")
    }

    private var generationTime = 0f

    override fun render() {
        if (!initialGenDone) {
            renderNoise()
        }

        // draw using pixmap
        batch.inUse {
            tempTex.dispose()
            tempTex = Texture(testTex)
            batch.draw(tempTex, 0f, 0f, NOISEBOX_WIDTH.toFloat(), NOISEBOX_HEIGHT.toFloat())
        }

        // read key input
        if (!generateKeyLatched && Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            generateKeyLatched = true
            seed = RNG.nextLong()
            renderNoise()
        }

        val coroutineExecFinished = genFutures.fold(true) { acc, it -> acc and (it?.isDone ?: true) }
        // check if generation is done
        if (coroutineExecFinished) {
            generateKeyLatched = false
        }

        // finish time measurement
        if (coroutineExecFinished && generationTimeInMeasure) {
            generationTimeInMeasure = false
            val time = System.nanoTime() - generationStartTime
            generationTime = time / 1000000000f
        }

        // draw timer
        batch.inUse {
            if (!generationTimeInMeasure) {
                font.draw(batch, "Generation time: ${generationTime} seconds", 8f, 8f)
            }
            else {
                font.draw(batch, "Generating...", 8f, 8f)
            }
        }
    }

//    private val NOISE_MAKER = AccidentalCave
//    private val NOISE_MAKER = BiomeMaker
    private val NOISE_MAKER = Oregen

    private fun getNoiseGenerator(SEED: Long): List<Joise> {
//        return NOISE_MAKER.getGenerator(SEED, TerragenParams())
//        return NOISE_MAKER.getGenerator(SEED, BiomegenParams())
        return NOISE_MAKER.getGenerator(SEED, OregenParams())
    }

    val colourNull = Color(0x1b3281ff)

    private val sampleOffset = NOISEBOX_WIDTH / 8.0

    private val testColSet = arrayOf(
            Color(0xff0000ff.toInt()),
            Color(0xffff00ff.toInt()),
            Color(0x00ff00ff.toInt()),
            Color(0x00ffffff.toInt()),
            Color(0x0000ffff.toInt()),
            Color(0xff00ffff.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff)
    )
    private val testColSet2 = arrayOf(
            0xff0000ff.toInt(),
            0xffff00ff.toInt(),
            0x00ff00ff.toInt(),
            0x00ffffff.toInt(),
            0x0000ffff.toInt(),
            0xff00ffff.toInt(),
            0xffffffff.toInt(),
            0xff
    )

    fun <T> Array<T>.shuffle() {
        val rng = Random(System.nanoTime())
        for (k in this.size - 1 downTo 1) {
            val r = rng.nextInt(k + 1)

            val t = this[r]
            this[r] = this[k]
            this[k] = t
        }
    }


    private fun renderNoise() {
        generationStartTime = System.nanoTime()
        generationTimeInMeasure = true

        // erase first
        testTex.setColor(colourNull)
        testTex.fill()

        testColSet.shuffle()
        testColSet2.shuffle()

        // render noisemap to pixmap

        /*
        I've got two ideas to resolve noisy artefact when noise generation runs concurrently:

        1) 1 block = 1 coroutine
        2) 1 thread has its own copy of Joise (threads have different INSTANCEs of Joise with same params)

        Method 1) seemingly works but may break if the operation is more complex
        Method 2) also works

        --CuriousTorvald, 2020-04-29
         */

        // 0. naive concurrent approach
        // CULPRIT: one global instance of Joise that all the threads try to access (and modify local variables) at the same time
        /*val runnables: List<RunnableFun> = xSlices.map { range ->
            {
                for (x in range) {
                    for (y in 0 until HEIGHT) {
                        val sampleTheta = (x.toDouble() / WIDTH) * TWO_PI
                        val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                        val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                        val sampleY = y.toDouble()

                        NOISE_MAKER.draw(x, y, joise.map { it.get(sampleX, sampleY, sampleZ) }, testTex)
                    }
                }
            }
        }*/

        // 1. stupid one-block-is-one-coroutine approach (seemingly works?)
        /*val joise = getNoiseGenerator(seed)
        val runnables: List<RunnableFun> = runs.map { i -> {
            val (x, y) = (i % WIDTH) to (i / WIDTH)
            val sampleTheta = (x.toDouble() / WIDTH) * TWO_PI
            val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
            val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
            val sampleY = y.toDouble()

            NOISE_MAKER.draw(x, y, joise.map { it.get(sampleX, sampleY, sampleZ) }, testTex)
        } }*/

        // 2. each runner gets their own copy of Joise
        val runnables: List<RunnableFun> = (0 until testTex.width).sliceEvenly(genSlices).map { range -> {
                val localJoise = getNoiseGenerator(seed)
                for (x in range) {
                    for (y in 0 until NOISEBOX_HEIGHT) {
                        val sampleTheta = (x.toDouble() / NOISEBOX_WIDTH) * TWO_PI
                        val sampleX = sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                        val sampleZ = cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                        val sampleY = y.toDouble()

                        NOISE_MAKER.draw(x, y, localJoise.map { it.get(sampleX, sampleY, sampleZ) }, testTex)
                    }
                }
        } }


        threadExecutor.renew()
        runnables.forEach {
            threadExecutor.submit(it)
        }

        threadExecutor.join()

        initialGenDone = true
    }

    override fun dispose() {
        testTex.dispose()
        tempTex.dispose()
    }
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(NOISEBOX_WIDTH, NOISEBOX_HEIGHT)
    appConfig.setForegroundFPS(60)
    appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)

    Lwjgl3Application(WorldgenNoiseSandbox(), appConfig)
}

internal interface NoiseMaker {
    fun draw(x: Int, y: Int, noiseValue: List<Double>, outTex: Pixmap)
    fun getGenerator(seed: Long, params: Any): List<Joise>
}

val locklock = java.lang.Object()

internal object BiomeMaker : NoiseMaker {

    override fun draw(x: Int, y: Int, noiseValue: List<Double>, outTex: Pixmap) {
        val colPal = biomeColors
        val control = noiseValue[0].coerceIn(0.0, 0.99999).times(colPal.size).toInt().coerceIn(colPal.indices)

        outTex.setColor(colPal[control])
        outTex.drawPixel(x, y)
    }

    override fun getGenerator(seed: Long, params: Any): List<Joise> {
        val params = params as BiomegenParams
        //val biome = ModuleBasisFunction()
        //biome.setType(ModuleBasisFunction.BasisType.SIMPLEX)

        // simplex AND fractal for more noisy edges, mmmm..!
        val fractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.MULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setNumOctaves(4)
            it.setFrequency(1.0)
            it.seed = seed shake 0x7E22A
        }

        val scaleDomain = ModuleScaleDomain().also {
            it.setSource(fractal)
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
        }

        val scale = ModuleScaleOffset().also {
            it.setSource(scaleDomain)
            it.setOffset(1.0)
            it.setScale(1.0)
        }

        val last = scale

        return listOf(Joise(last))
    }

    // with this method, only TWO distinct (not bland) biomes are possible. CLUT order is important here.
    val biomeColors = intArrayOf(
        //0x2288ccff.toInt(), // ísland
        0x229944ff.toInt(), // woodlands
        0x77bb77ff.toInt(), // shrubland
        0xbbdd99ff.toInt(), // plains
        0xbbdd99ff.toInt(), // plains
//        0xeeddbbff.toInt(), // sands
        0x888888ff.toInt() // rockyland
    )
}

// http://accidentalnoise.sourceforge.net/minecraftworlds.html
internal object AccidentalCave : NoiseMaker {

    private infix fun Color.mul(other: Color) = this.mul(other)

    private val notationColours = arrayOf(
            Color.WHITE,
            Color.MAGENTA,
            Color(0f, 186f/255f, 1f, 1f),
            Color(.5f, 1f, .5f, 1f),
            Color(1f, 0.93f, 0.07f, 1f),
            Color(0.97f, 0.6f, 0.56f, 1f)
    )

    override fun draw(x: Int, y: Int, noiseValue: List<Double>, outTex: Pixmap) {
        // simple one-source draw
        /*val c = noiseValue[0].toFloat()
        val selector = c.minus(0.0001).floorToInt() fmod notationColours.size
        val selecteeColour = Color(c - selector, c - selector, c - selector, 1f)
        if (c < 0) {
            outTex.setColor(-c, 0f, 0f, 1f)
        }
        else {
            outTex.setColor(selecteeColour.mul(notationColours[selector]))
        }
        outTex.drawPixel(x, y)*/


        fun Double.tiered(vararg tiers: Double): Int {
            tiers.reversed().forEachIndexed { index, it ->
                if (this >= it) return (tiers.lastIndex - index) // why??
            }
            return tiers.lastIndex
        }

        val groundDepthCol = listOf(
                Color(0f, 0f, 0f, 1f),
                Color(0.55f, 0.4f, 0.24f, 1f),
                Color(.6f, .6f, .6f, 1f)
        )
        val n1 = noiseValue[0].tiered(.0, .5, .88)
        //var n2 = noiseValue[1].toFloat()
        //if (n2 != 1f) n2 = 0.5f
        val c1 = groundDepthCol[n1]
        //val c2 = Color(n2, n2, n2, 1f)
        //val cout = c1 mul c23
        val cout = c1

        outTex.drawPixel(x, y, cout.toRGBA())
    }

    override fun getGenerator(seed: Long, params: Any): List<Joise> {
        val params = params as TerragenParams

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
        lowlandScale.setOffset(params.lowlandScaleOffset) // TODO linearly alters the height

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
        highlandScale.setOffset(params.highlandScaleOffset) // TODO linearly alters the height

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
        mountainScale.setOffset(params.mountainScaleOffset) // TODO linearly alters the height

        val mountainYScale = ModuleScaleDomain()
        mountainYScale.setSource(mountainScale)
        mountainYScale.setScaleY(params.mountainDisturbance) // greater = more distortion, overhangs

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

        val groundSelect2 = ModuleSelect()
        groundSelect2.setLowSource(0.0)
        groundSelect2.setHighSource(1.0)
        groundSelect2.setThreshold(0.8)
        groundSelect2.setControlSource(highlandLowlandSelectCache)

        /* caves */

        val caveShape = ModuleFractal()
        caveShape.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveShape.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveShape.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveShape.setNumOctaves(1)
        caveShape.setFrequency(params.caveShapeFreq) // TODO adjust the "density" of the caves
        caveShape.seed = seed shake caveMagic

        val caveAttenuateBias = ModuleBias()
        caveAttenuateBias.setSource(highlandLowlandSelectCache)
        caveAttenuateBias.setBias(params.caveAttenuateBias) // TODO (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids

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
        caveSelect.setThreshold(params.caveSelectThre) // TODO also adjust this if you've touched the bias value. Number can be greater than 1.0
        caveSelect.setFalloff(0.0)

        val caveBlockageFractal = ModuleFractal()
        caveBlockageFractal.setType(ModuleFractal.FractalType.RIDGEMULTI)
        caveBlockageFractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        caveBlockageFractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        caveBlockageFractal.setNumOctaves(2)
        caveBlockageFractal.setFrequency(params.caveBlockageFractalFreq) // TODO same as caveShape frequency?
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
        caveBlockageSelect.setThreshold(params.caveBlockageSelectThre) // TODO adjust cave cloing-up strength. Larger = more closing
        caveBlockageSelect.setFalloff(0.0)

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner()
        caveInMix.setType(ModuleCombiner.CombinerType.ADD)
        caveInMix.setSource(0, caveSelect)
        caveInMix.setSource(1, caveBlockageSelect)

        /*val groundCaveMult = ModuleCombiner()
        groundCaveMult.setType(ModuleCombiner.CombinerType.MULT)
        groundCaveMult.setSource(0, caveInMix)
        //groundCaveMult.setSource(0, caveSelect) // disables the cave-in for quick cavegen testing
        groundCaveMult.setSource(1, groundSelect)*/

        // this noise tree WILL generate noise value greater than 1.0
        // they should be treated properly when you actually generate the world out of the noisemap
        // for the visualisation, no treatment will be done in this demo app.

        val groundClamp = ModuleClamp()
        groundClamp.setRange(0.0, 100.0)
        groundClamp.setSource(highlandLowlandSelectCache)

        val groundScaling = ModuleScaleDomain()
        groundScaling.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
        groundScaling.setScaleY(1.0 / params.featureSize)
        groundScaling.setScaleZ(1.0 / params.featureSize)
        groundScaling.setSource(groundClamp)


        val caveClamp = ModuleClamp()
        caveClamp.setRange(0.0, 1.0)
        caveClamp.setSource(caveInMix)

        val caveScaling = ModuleScaleDomain()
        caveScaling.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
        caveScaling.setScaleY(1.0 / params.featureSize)
        caveScaling.setScaleZ(1.0 / params.featureSize)
        caveScaling.setSource(caveClamp)

        //return Joise(caveInMix)
        return listOf(
                Joise(groundScaling),
                Joise(caveScaling)
        )
    }
}


internal object Oregen : NoiseMaker {
    override fun draw(x: Int, y: Int, noiseValue: List<Double>, outTex: Pixmap) {
        var n = noiseValue[0]

//        if (n in 0.0..1.0) n = 1.0 - n

        val cout = if (n >= 0.0)
            Color(n.toFloat(), n.toFloat(), n.toFloat(), 1f)
        else
            Color(-n.toFloat(), 0f, 1f, 1f)

        outTex.drawPixel(x, y, cout.toRGBA())
    }

    override fun getGenerator(seed: Long, params: Any): List<Joise> {
        val params = params as OregenParams

        val oreMagic = 0x023L
        val orePerturbMagic = 12345L

        val oreShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(params.oreShapeFreq) // adjust the "density" of the caves
            it.seed = seed shake oreMagic
        }

        val oreShape2 = ModuleScaleOffset().also {
            it.setSource(oreShape)
            it.setScale(1.0)
            it.setOffset(-0.5)
        }

        val orePerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(params.oreShapeFreq * 3.0 / 4.0)
            it.seed = seed shake orePerturbMagic
        }

        val orePerturbScale = ModuleScaleOffset().also {
            it.setSource(orePerturbFractal)
            it.setScale(20.0)
            it.setOffset(0.0)
        }

        val orePerturb = ModuleTranslateDomain().also {
            it.setSource(oreShape2)
            it.setAxisXSource(orePerturbScale)
        }

        val oreSelectAttenuate = ModulePow().also {
            it.setSource(ModuleGradient().also {
                it.setGradient(0.0, 0.0, NOISEBOX_HEIGHT.toDouble() * 4, 100.0)
            })
            it.setPower(1.0 / 4.0)
        }

        val oreSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(orePerturb)
            it.setThreshold(oreSelectAttenuate)
            it.setFalloff(0.0)
        }

        return listOf(
            Joise(oreSelect)
        )
    }

}


/*infix fun Long.shake(other: Long): Long {
    var s0 = this
    var s1 = other

    s1 = s1 xor s0
    s0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14)
    s1 = s1 shl 36 or s1.ushr(28)

    return s0 + s1
}*/