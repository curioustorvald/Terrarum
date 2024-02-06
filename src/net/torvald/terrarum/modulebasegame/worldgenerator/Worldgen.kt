package net.torvald.terrarum.modulebasegame.worldgenerator

import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.XXHash64
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * New world generator.
 *
 * Created by minjaesong on 2019-09-02.
 */
object Worldgen {

    private lateinit var world: GameWorld
    lateinit var params: WorldgenParams
        private set

    val threadExecutor = TerrarumIngame.worldgenThreadExecutor

    private val threadLock = java.lang.Object()

    fun attachMap(world: GameWorld, genParams: WorldgenParams) {
        this.world = world
        params = genParams

        highlandLowlandSelectCache = getHighlandLowlandSelectCache(params.terragenParams, params.seed)
        caveAttenuateBiasScaledCache = getCaveAttenuateBiasScaled(highlandLowlandSelectCache, params.terragenParams)
        biomeMap = HashMap()
    }

    internal lateinit var highlandLowlandSelectCache: ModuleCache
    internal lateinit var caveAttenuateBiasScaledCache: ModuleCache
    internal lateinit var biomeMap: HashMap<BlockAddress, Byte>


    /**
     * Other modules are free to add their own ores to the world generator.
     */
    fun registerOre(oreInfo: OregenParams) {
        oreRegistry.add(oreInfo)
    }

    private val oreRegistry = ArrayList<OregenParams>()

    fun getJobs(tags: List<String> = emptyList()): List<Work> {
        val oreTilingModes = HashMap<ItemID, String>().also {
            it.putAll(oreRegistry.map { it.tile to it.tiling })
        }

        val tagFilter = if (tags.isEmpty()) { { work: Work -> true } }
        else {
            { work: Work ->
                (work.tags union tags).isNotEmpty()
            }
        }
        return listOf(
            Work(Lang["MENU_IO_WORLDGEN_RETICULATING_SPLINES"], Terragen(world, false, highlandLowlandSelectCache, params.seed, params.terragenParams), listOf("TERRAIN")),
            Work(Lang["MENU_IO_WORLDGEN_GROWING_MINERALS"], Oregen(world, false, caveAttenuateBiasScaledCache, params.seed, oreRegistry), listOf("ORES")),
            Work(Lang["MENU_IO_WORLDGEN_POSITIONING_ROCKS"], OregenAutotiling(world, false, params.seed, oreTilingModes), listOf("ORES")),
            // TODO generate rock veins
            // TODO generate gemstones
            Work(Lang["MENU_IO_WORLDGEN_CARVING_EARTH"], Cavegen(world, false, highlandLowlandSelectCache, params.seed, params.terragenParams), listOf("TERRAIN", "CAVE")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Biomegen(world, false, params.seed, params.biomegenParams, biomeMap), listOf("BIOME")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Treegen(world, true, params.seed, params.terragenParams, params.treegenParams, biomeMap), listOf("TREES")),
        ).filter(tagFilter)
    }

    fun generateMap(loadscreen: LoadScreenBase) {
        val jobs = getJobs()


        for (i in jobs.indices) {
            printdbg(this, "Worldgen: job #${i+1}")

            val it = jobs[i]

            App.getLoadScreen().addMessage(it.loadingScreenName)
            it.theWork.getDone(loadscreen)
        }


        // determine spawn point
        val (spawnX, spawnY) = tryForSpawnPoint(world)
        world.spawnX = spawnX
        world.spawnY = spawnY

        printdbg(this, "Generation job finished")

    }

    /**
     * Chunk flags will be set automatically
     */
    fun generateChunkIngame(cx: Int, cy: Int, callback: (Int, Int) -> Unit) {
        val jobs = getJobs()
        printdbg(this, "Generating chunk on ($cx, $cy)")
        Thread {
            world.chunkFlags[cy][cx] = GameWorld.CHUNK_GENERATING

            for (i in jobs.indices) {
                val it = jobs[i]
                it.theWork.getChunkDone(cx, cy)
            }

            world.chunkFlags[cy][cx] = GameWorld.CHUNK_LOADED
            callback(cx, cy)
        }.let {
            it.priority = 2
            it.start()
        }
    }

    data class Work(val loadingScreenName: String, val theWork: Gen, val tags: List<String>)

    fun getEstimationSec(width: Int, height: Int): Long {
        val testMachineBogoFlops = 48000000
        val testMachineThreads = 32
        val dataPoints = intArrayOf(9, 15, 23, 32)
        val eqMult = 0.0214 // use google sheet to get trend line equation
        val eqPow = 0.396 // use google sheet to get trend line equation

        val f = eqMult * (width.toDouble() * height).pow(eqPow)
        return (1.3 * (testMachineBogoFlops.toDouble() / bogoflops) * f * (testMachineThreads.toDouble() / THREAD_COUNT)).roundToLong()
    }

    /**
     * @return starting chunk Y index, ending chunk Y index (inclusive)
     */
    fun getChunkGenStrip(world: GameWorld): Pair<Int, Int> {
        val start = (0.00342f * world.height - 3.22f).floorToInt().coerceAtLeast(1)
        // this value has to extend up, otherwise the player may spawn into the chopped-off mountaintop
        // this value has to extend down into the rock layer, otherwise, if the bottom of the bottom chunk is dirt, they will turn into grasses
        return start - 1 to start + 7
    }

    private val rockScoreMin = 40
    private val treeScoreMin = 25

    private fun tryForSpawnPoint(world: GameWorld): Point2i {
        val yInit = getChunkGenStrip(world).first
        val tallies = ArrayList<Pair<Point2i, Vector3f>>() // xypos, score (0..1+)
        var tries = 0
        var found = false
        while (tries < 99) {
            val posX = (Math.random() * world.width).toInt()
            var posY = yInit * CHUNK_H
            // go up?
            if (BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                // go up!
                while (BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                    posY -= 1
                }
            }
            else {
                // go down!
                while (!BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                    posY += 1
                }
            }

            printdbg(this, "Trying spawn point #${tries + 1} at ($posX, $posY)")

            var rocks = 0
            var trees = 0
            var flatness = 0
            // make survey
            for (sx in -80..80) {
                for (sy in -30..30) {
                    val x = posX + sx
                    val y = posY + sy
                    val tile = BlockCodex[world.getTileFromTerrain(x, y)]
                    if (tile.hasTag("ROCK")) {
                        rocks += 1
                    }
                    else if (tile.hasTag("TREETRUNK")) {
                        trees += 1
                    }
                }
            }

            for (sx in -2..2) {
                val x = posX + sx
                val y = posY
                val yUp = posY - 1
                val tile = BlockCodex[world.getTileFromTerrain(x, y)]
                val tileUp = world.getTileFromTerrain(x, yUp)
                if (tile.isSolid && tileUp == Block.AIR)
                    flatness += 1
            }

            val rockScore = rocks / rockScoreMin.toFloat()
            val treeScore = trees / treeScoreMin.toFloat()
            val flatScore = flatness / 5f
            val score = Vector3f(rockScore, treeScore, flatScore)

            tallies.add(Point2i(posX, posY) to score)

            printdbg(this, "...Survey says: $rocks/$rockScoreMin rocks, $trees/$treeScoreMin trees, flatness $flatScore")

            if (score.x >= 1f && score.y >= 1f && flatScore >= 1f) {
                found = true
                break
            }

            tries += 1
        }

        if (found)
            return tallies.last().first

        return tallies.toTypedArray().also {
            it.map { it.second.let {
                it.x = (it.x).coerceAtMost(1f)
                it.y = (it.y).coerceAtMost(1f)
            } }

            it.shuffle()
            it.sortByDescending { it.second.lengthSquared() }

            it.first().let {
                printdbg(this, "Final answer: ${it.first} with score ${it.second}")
            }
        }.first().first
    }

    private fun getHighlandLowlandSelectCache(params: TerragenParams, seed: Long): ModuleCache {
        val lowlandMagic: Long = 0x41A21A114DBE56 // Maria Lindberg
        val highlandMagic: Long = 0x0114E091      // Olive Oyl
        val mountainMagic: Long = 0x115AA4DE2504  // Lisa Anderson
        val selectionMagic: Long = 0x41E10D9B100  // Melody Blue

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

        return highlandLowlandSelectCache
    }

    private fun getCaveAttenuateBiasScaled(highlandLowlandSelectCache: ModuleCache, params: TerragenParams): ModuleCache {
        val caveAttenuateBias = ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        }

        val scale =  ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveAttenuateBias)
        }

        return ModuleCache().also {
            it.setSource(scale)
        }
    }

}

abstract class Gen(val world: GameWorld, val isFinal: Boolean, val seed: Long, val params: Any? = null) {

    open fun getDone(loadscreen: LoadScreenBase?) { } // trying to use different name so that it won't be confused with Runnable or Callable
    protected abstract fun getGenerator(seed: Long, params: Any?): List<Joise>
    protected abstract fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double)

    private fun getChunksRange(): List<Int> {
        val (yStart, yEnd) = Worldgen.getChunkGenStrip(world)
        return (0 until world.width / CHUNK_W).flatMap { cx ->
            (LandUtil.chunkXYtoChunkNum(world, cx, yStart)..LandUtil.chunkXYtoChunkNum(world, cx, yEnd)).toList()
        }
    }

    fun submitJob(loadscreen: LoadScreenBase?) {
        getChunksRange().forEach { chunkNum ->
            val (chunkX, chunkY) = LandUtil.chunkNumToChunkXY(world, chunkNum)
            Worldgen.threadExecutor.submit {
                val localJoise = getGenerator(seed, params)
                val sampleOffset = world.width / 8.0
                draw(chunkX * LandUtil.CHUNK_W, chunkY * CHUNK_H, localJoise, sampleOffset)
                loadscreen?.progress?.addAndGet(1L)

                world.chunkFlags[chunkY][chunkX] = if (isFinal) GameWorld.CHUNK_LOADED else GameWorld.CHUNK_GENERATING
            }
        }
    }

    fun getChunkDone(chunkX: Int, chunkY: Int) {
        val localJoise = getGenerator(seed, params)
        val sampleOffset = world.width / 8.0
        draw(chunkX * LandUtil.CHUNK_W, chunkY * CHUNK_H, localJoise, sampleOffset)
    }
}

data class WorldgenParams(
    val seed: Long,
    // optional parameters
    val terragenParams: TerragenParams = TerragenParams(),
    val biomegenParams: BiomegenParams = BiomegenParams(),
    val treegenParams: TreegenParams = TreegenParams(),
)

infix fun Long.shake(other: Long): Long {
    var s0 = this
    var s1 = other

    s1 = s1 xor s0
    s0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14)
    s1 = s1 shl 36 or s1.ushr(28)

    return s0 + s1
}

infix fun Long.shake(other: String): Long {
    return this shake XXHash64.hash(other.toByteArray(), this + 31)
}

val TWO_PI = Math.PI * 2.0
val HALF_PI = Math.PI / 2.0
