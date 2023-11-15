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
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.jme3.math.FastMath
import com.sudoplay.joise.Joise
import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.RunnableFun
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.modulebasegame.worldgenerator.*
import net.torvald.terrarum.worlddrawer.toRGBA
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import java.util.concurrent.Future
import kotlin.math.*
import kotlin.random.Random
import net.torvald.terrarum.modulebasegame.worldgenerator.Terragen
import java.io.PrintStream

const val NOISEBOX_WIDTH = 1200
const val NOISEBOX_HEIGHT = 2400
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

    private val NM_TERR = TerragenTest to TerragenParams()
    private val NM_BIOME = BiomeMaker to BiomegenParams()

    private val NOISEMAKER = NM_TERR

    override fun render() {

        if (!initialGenDone) {
            renderNoise(NOISEMAKER)
            initialGenDone = true
        }

        // draw using pixmap
        batch.inUse {
            tempTex.dispose()
            tempTex = Texture(testTex)
            batch.draw(tempTex, 0f, 0f, NOISEBOX_WIDTH.toFloat(), NOISEBOX_HEIGHT.toFloat())
        }

        // read key input
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && worldgenDone) {
            seed = RNG.nextLong()
            renderNoise(NOISEMAKER)
        }


        // draw timer
        batch.inUse {
            if (worldgenDone) {
                font.draw(batch, "Generation time: ${generationTime} seconds", 8f, 8f)
            }
            else {
                font.draw(batch, "Generating...", 8f, 8f)
            }
        }
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

    private fun printStackTrace(obj: Any, out: PrintStream = System.out) {
        val indentation = " ".repeat(obj.javaClass.simpleName.length + 4)
        Thread.currentThread().stackTrace.forEachIndexed { index, it ->
            if (index == 1)
                out.println("[${obj.javaClass.simpleName}]> $it")
            else if (index > 1)
                out.println("$indentation$it")
        }
    }

    private fun renderNoise(noiseMaker: Pair<NoiseMaker, Any>, callback: () -> Unit = {}) {
        generationStartTime = System.nanoTime()

        // erase first
        testTex.setColor(colourNull)
        testTex.fill()

        testColSet.shuffle()
        testColSet2.shuffle()

        worldgenDone = false

        Thread {
            val runnables: List<RunnableFun> = (0 until testTex.width).sliceEvenly(genSlices).map { range ->
                {
                    val localJoise = noiseMaker.first.getGenerator(seed, noiseMaker.second)
                    for (x in range) {
                        for (y in 0 until NOISEBOX_HEIGHT) {
                            val sampleTheta = (x.toDouble() / NOISEBOX_WIDTH) * TWO_PI
                            val sampleX =
                                sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                            val sampleZ =
                                cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                            val sampleY =
                                y - (NOISEBOX_HEIGHT - Terragen.YHEIGHT_MAGIC) * Terragen.YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant

                            noiseMaker.first.draw(x, y, localJoise.mapIndexed { index, it ->
                                it.get(sampleX, sampleY, sampleZ)
                            }, testTex)
                        }
                    }
                }
            }

            threadExecutor.renew()
            runnables.forEach {
                threadExecutor.submit(it)
            }

            threadExecutor.join()

            worldgenDone = true

            val time = System.nanoTime() - generationStartTime
            generationTime = time / 1000000000f
            callback()
        }.start()

    }

    var worldgenDone = true; private set

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

interface NoiseMaker {
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
            it.setScaleX(1.0 / params.featureSize1) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize1)
            it.setScaleZ(1.0 / params.featureSize1)
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
internal object TerragenTest : NoiseMaker {

    private infix fun Color.mul(other: Color) = this.mul(other)

    private val notationColours = arrayOf(
            Color.WHITE,
            Color.MAGENTA,
            Color(0f, 186f/255f, 1f, 1f),
            Color(.5f, 1f, .5f, 1f),
            Color(1f, 0.93f, 0.07f, 1f),
            Color(0.97f, 0.6f, 0.56f, 1f)
    )

    private val groundDepthBlock = listOf(
        Block.AIR, Block.DIRT, Block.STONE, Block.STONE_SLATE
    )

    private fun Double.tiered(tiers: List<Double>): Int {
        tiers.reversed().forEachIndexed { index, it ->
            if (this >= it) return (tiers.lastIndex - index) // why??
        }
        return tiers.lastIndex
    }

    private val BACK = Color(0.6f, 0.66f, 0.78f, 1f).toRGBA()

    private val blockToCol = hashMapOf(
        Block.AIR to Color(0f, 0f, 0f, 1f),
        Block.DIRT to Color(0.588f, 0.45f, 0.3f, 1f),
        Block.STONE to Color(0.4f, 0.4f, 0.4f, 1f),
        Block.STONE_SLATE to Color(0.2f, 0.2f, 0.2f, 1f),
        Block.STONE_MARBLE to Color(0.8f, 0.8f, 0.8f, 1f)
    )

    private val COPPER_ORE = 0x00e9c8ff
    private val IRON_ORE = 0xff7e74ff.toInt()
    private val COAL_ORE = 0x383314ff.toInt()
    private val ZINC_ORE = 0xefde76ff.toInt()
    private val TIN_ORE = 0xcd8b62ff.toInt()
    private val GOLD_ORE = 0xffcc00ff.toInt()
    private val SILVER_ORE = 0xd5d9f9ff.toInt()
    private val LEAD_ORE = 0xff9300ff.toInt()

    private val oreCols = listOf(
        COPPER_ORE, IRON_ORE, COAL_ORE, ZINC_ORE, TIN_ORE, GOLD_ORE, SILVER_ORE, LEAD_ORE
    )

    private val terragenTiers = listOf(.0, .5, 1.0, 2.5).map { it * (NOISEBOX_HEIGHT / 2400.0).pow(0.75) } // pow 1.0 for 1-to-1 scaling; 0.75 is used to make deep-rock layers actually deep for huge world size

    private fun Double.dither() = Math.random() < this

    override fun draw(x: Int, y: Int, noiseValue: List<Double>, outTex: Pixmap) {
        val terr = noiseValue[0].tiered(terragenTiers)
        val cave = if (noiseValue[1] < 0.5) 0 else 1
        val ore = (noiseValue.subList(2, noiseValue.size)).zip(oreCols).firstNotNullOfOrNull { (n, colour) -> if (n > 0.5) colour else null }

        val isMarble = noiseValue[10] > 0.5

        val wallBlock =  if (isMarble) Block.STONE_MARBLE else groundDepthBlock[terr]
        val terrBlock = if (cave == 0) Block.AIR else if (isMarble) Block.STONE_MARBLE else wallBlock


        outTex.drawPixel(x, y,
            if (ore != null && (terrBlock == Block.STONE || terrBlock == Block.STONE_SLATE)) ore
            else if (wallBlock == Block.AIR && terrBlock == Block.AIR) BACK
            else blockToCol[terrBlock]!!.toRGBA()
        )

//        outTex.drawPixel(x, y, noiseValue[2].toColour())
    }

    private fun Double.toColour(): Int {
        val d = if (this.isNaN()) 0.0 else this.absoluteValue
        val b = d.toFloat()

        val c = if (b >= 2f)
            // 1 0 1 S
            // 0 1 1 E
            Color(
                FastMath.interpolateLinear(b - 2f, 1f, 0f),
                FastMath.interpolateLinear(b - 2f, 0f, 1f),
                1f, 1f
            )
        else if (b >= 1f)
            Color(1f, 1f - (b - 1f), 1f, 1f)
        else if (b >= 0f)
            Color(b, b, b, 1f)
        else
            Color(b, 0f, 0f, 1f)

        return c.toRGBA()
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

        val oreMagic = 0x023L
        val orePerturbMagic = 12345L

        val groundGradient = ModuleGradient().also {
            it.setGradient(0.0, 0.0, 0.0, 1.0)
        }

        /* lowlands */

        val lowlandShapeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.BILLOW)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(0.25)
            it.seed = seed shake lowlandMagic
        }

        val lowlandScale = ModuleScaleOffset().also {
            it.setSource(lowlandShapeFractal)
            it.setScale(0.22)
            it.setOffset(params.lowlandScaleOffset) // linearly alters the height
        }

        val lowlandYScale = ModuleScaleDomain().also {
            it.setSource(lowlandScale)
            it.setScaleY(0.02) // greater = more distortion, overhangs
        }

        val lowlandTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(lowlandYScale)
        }

        /* highlands */

        val highlandShapeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(4)
            it.setFrequency(2.0)
            it.seed = seed shake highlandMagic
        }

        val highlandScale = ModuleScaleOffset().also {
            it.setSource(highlandShapeFractal)
            it.setScale(0.5)
            it.setOffset(params.highlandScaleOffset) // linearly alters the height
        }

        val highlandYScale = ModuleScaleDomain().also {
            it.setSource(highlandScale)
            it.setScaleY(0.14) // greater = more distortion, overhangs
        }

        val highlandTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(highlandYScale)
        }

        /* mountains */

        val mountainShapeFractal = ModuleFractal().also {
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(8)
            it.setFrequency(1.0)
            it.seed = seed shake mountainMagic
        }

        val mountainScale = ModuleScaleOffset().also {
            it.setSource(mountainShapeFractal)
            it.setScale(1.0)
            it.setOffset(params.mountainScaleOffset) // linearly alters the height
        }

        val mountainYScale = ModuleScaleDomain().also {
            it.setSource(mountainScale)
            it.setScaleY(params.mountainDisturbance) // greater = more distortion, overhangs
        }

        val mountainTerrain = ModuleTranslateDomain().also {
            it.setSource(groundGradient)
            it.setAxisYSource(mountainYScale)
        }

        /* selection */

        val terrainTypeFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(3)
            it.setFrequency(0.125)
            it.seed = seed shake selectionMagic
        }

        val terrainScaleOffset = ModuleScaleOffset().also {
            it.setSource(terrainTypeFractal)
            it.setOffset(0.5)
            it.setScale(0.666666) // greater = more dynamic terrain
        }

        val terrainTypeYScale = ModuleScaleDomain().also {
            it.setSource(terrainScaleOffset)
            it.setScaleY(0.0)
        }

        val terrainTypeCache = ModuleCache().also {
            it.setSource(terrainTypeYScale)
        }

        val highlandMountainSelect = ModuleSelect().also {
            it.setLowSource(highlandTerrain)
            it.setHighSource(mountainTerrain)
            it.setControlSource(terrainTypeCache)
            it.setThreshold(0.55)
            it.setFalloff(0.2)
        }

        val highlandLowlandSelect = ModuleSelect().also {
            it.setLowSource(lowlandTerrain)
            it.setHighSource(highlandMountainSelect)
            it.setControlSource(terrainTypeCache)
            it.setThreshold(0.25)
            it.setFalloff(0.15)
        }

        val highlandLowlandSelectCache = ModuleCache().also {
            it.setSource(highlandLowlandSelect)
        }

        val groundSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setThreshold(0.5)
            it.setControlSource(highlandLowlandSelectCache)
        }

        val groundSelect2 = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setThreshold(0.8)
            it.setControlSource(highlandLowlandSelectCache)
        }

        /* caves */

        val caveShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(1)
            it.setFrequency(params.caveShapeFreq) // adjust the "density" of the caves
            it.seed = seed shake caveMagic
        }

        val caveAttenuateBias = ModuleCache().also { it.setSource(ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        })}

        val caveShapeAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, caveShape)
            it.setSource(1, caveAttenuateBias)
        }

        val cavePerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(params.caveShapeFreq * 3.0 / 4.0)
            it.seed = seed shake cavePerturbMagic
        }

        val cavePerturbScale = ModuleScaleOffset().also {
            it.setSource(cavePerturbFractal)
            it.setScale(0.45)
            it.setOffset(0.0)
        }

        val cavePerturb = ModuleTranslateDomain().also {
            it.setSource(caveShapeAttenuate)
            it.setAxisXSource(cavePerturbScale)
        }

        val caveSelect = ModuleSelect().also {
            it.setLowSource(1.0)
            it.setHighSource(0.0)
            it.setControlSource(cavePerturb)
            it.setThreshold(params.caveSelectThre) // also adjust this if you've touched the bias value. Number can be greater than 1.0
            it.setFalloff(0.0)
        }

        val caveBlockageFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(params.caveBlockageFractalFreq) // same as caveShape frequency?
            it.seed = seed shake caveBlockageMagic
        }

        // will only close-up deeper caves. Shallow caves will be less likely to be closed up
        val caveBlockageAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, caveBlockageFractal)
            it.setSource(1, caveAttenuateBias)
        }

        val caveBlockageSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(caveBlockageAttenuate)
            it.setThreshold(params.caveBlockageSelectThre) // adjust cave cloing-up strength. Larger = more closing
            it.setFalloff(0.0)
        }

        // note: gradient-multiply DOESN'T generate "naturally cramped" cave entrance

        val caveInMix = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.ADD)
            it.setSource(0, caveSelect)
            it.setSource(1, caveBlockageSelect)
        }

        // this noise tree WILL generate noise value greater than 1.0
        // they should be treated properly when you actually generate the world out of the noisemap
        // for the visualisation, no treatment will be done in this demo app.

        val groundClamp = ModuleClamp().also {
            it.setRange(0.0, 100.0)
            it.setSource(highlandLowlandSelectCache)
        }

        val groundScaling = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(groundClamp)
        }

        val caveClamp = ModuleClamp().also {
            it.setRange(0.0, 1.0)
            it.setSource(caveInMix)
        }

        val caveScaling = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveClamp)
        }

        val caveAttenuateBiasScaled = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveAttenuateBias)
        }



        val groundScalingCached = ModuleCache().also { it.setSource(groundScaling) }
        val caveAttenuateBiasScaledCache = ModuleCache().also { it.setSource(caveAttenuateBiasScaled) }


        //return Joise(caveInMix)
        return listOf(
            Joise(groundScalingCached),
            Joise(caveScaling),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:1", 0.026, 0.010, 0.517, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:2", 0.045, 0.011, 0.517, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:3", 0.017, 0.070, 0.511, 3.8)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:4", 0.019, 0.011, 0.511, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:5", 0.017, 0.017, 0.511, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:6", 0.009, 0.300, 0.474, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:7", 0.013, 0.300, 0.476, 1.0)),
            Joise(generateOreVeinModule(caveAttenuateBiasScaledCache, seed shake "ores@basegame:8", 0.017, 0.020, 0.511, 1.0)),

            Joise(generateRockLayer(groundScalingCached, seed shake 10, params, listOf(
                0.02 to 2.6,
                0.03 to 2.9,
                0.04 to 3.2,
                0.03 to 3.5,
                0.02 to 3.8,
            ))),
        )
    }

    private fun generateRockLayer(ground: ModuleCache, seed: Long, params: TerragenParams, thicknessAndRange: List<Pair<Double, Double>>): Module {

        val occlusion = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(params.rockBandCutoffFreq / params.featureSize) // adjust the "density" of the veins
            it.seed = seed
        }

        val occlusionScale = ModuleScaleDomain().also {
            it.setScaleX(0.5)
            it.setScaleZ(0.5)
            it.setSource(occlusion)
        }

        val occlusionBinary = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(occlusionScale)
            it.setThreshold(1.1)
            it.setFalloff(0.0)
        }

        val occlusionCache = ModuleCache().also {
            it.setSource(occlusionBinary)
        }

        val bands = thicknessAndRange.map { (thickness, rangeStart) ->
            val thresholdLow = ModuleSelect().also {
                it.setLowSource(0.0)
                it.setHighSource(1.0)
                it.setControlSource(ground)
                it.setThreshold(rangeStart)
                it.setFalloff(0.0)
            }

            val thresholdHigh = ModuleSelect().also {
                it.setLowSource(1.0)
                it.setHighSource(0.0)
                it.setControlSource(ground)
                it.setThreshold(rangeStart + thickness)
                it.setFalloff(0.0)
            }

            ModuleCombiner().also {
                it.setSource(0, thresholdLow)
                it.setSource(1, thresholdHigh)
                it.setSource(2, occlusionCache)
                it.setType(ModuleCombiner.CombinerType.MULT)
            }
        }


        val combinedBands = ModuleCombiner().also {
            bands.forEachIndexed { index, module ->
                it.setSource(index, module)
            }
            it.setType(ModuleCombiner.CombinerType.ADD)
        }

        return combinedBands
    }

    private fun applyPowMult(joiseModule: Module, pow: Double, mult: Double): Module {
        return ModuleScaleOffset().also {
            it.setSource(ModulePow().also {
                it.setSource(joiseModule)
                it.setPower(pow)
            })
            it.setScale(mult)
        }
    }

    private fun generateOreVeinModule(caveAttenuateBiasScaledCache: ModuleCache, seed: Long, freq: Double, pow: Double, scale: Double, ratio: Double): Module {
        val oreShape = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.RIDGEMULTI)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(2)
            it.setFrequency(freq) // adjust the "density" of the caves
            it.seed = seed
        }

        val oreShape2 = ModuleScaleOffset().also {
            it.setSource(oreShape)
            it.setScale(1.0)
            it.setOffset(-0.5)
        }

        val caveAttenuateBias3 = applyPowMult(caveAttenuateBiasScaledCache, pow, scale)

        val oreShapeAttenuate = ModuleCombiner().also {
            it.setType(ModuleCombiner.CombinerType.MULT)
            it.setSource(0, oreShape2)
            it.setSource(1, caveAttenuateBias3)
        }

        val orePerturbFractal = ModuleFractal().also {
            it.setType(ModuleFractal.FractalType.FBM)
            it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
            it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
            it.setNumOctaves(6)
            it.setFrequency(freq * 3.0 / 4.0)
            it.seed = seed shake 0x5721CE_76E_EA276L // strike the earth
        }

        val orePerturbScale = ModuleScaleOffset().also {
            it.setSource(orePerturbFractal)
            it.setScale(20.0)
            it.setOffset(0.0)
        }

        val orePerturb = ModuleTranslateDomain().also {
            it.setSource(oreShapeAttenuate)
            it.setAxisXSource(orePerturbScale)
        }

        val oreStrecth = ModuleScaleDomain().also {
            val xratio = if (ratio >= 1.0) ratio else 1.0
            val yratio = if (ratio < 1.0) 1.0 / ratio else 1.0
            val k = sqrt(2.0 / (xratio.sqr() + yratio.sqr()))
            val xs = xratio * k
            val ys = yratio * k

            it.setSource(orePerturb)
            it.setScaleX(1.0 / xs)
            it.setScaleZ(1.0 / xs)
            it.setScaleY(1.0 / ys)
        }

        val oreSelect = ModuleSelect().also {
            it.setLowSource(0.0)
            it.setHighSource(1.0)
            it.setControlSource(oreStrecth)
            it.setThreshold(0.5)
            it.setFalloff(0.0)
        }

        return oreSelect
    }

    private object DummyModule : Module() {
        override fun get(x: Double, y: Double) = 0.0

        override fun get(x: Double, y: Double, z: Double) = 0.0

        override fun get(x: Double, y: Double, z: Double, w: Double) = 0.0

        override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double) = 0.0

        override fun _writeToMap(map: ModuleMap?) {
        }

        override fun buildFromPropertyMap(props: ModulePropertyMap?, map: ModuleInstanceMap?): Module {
            return this
        }

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