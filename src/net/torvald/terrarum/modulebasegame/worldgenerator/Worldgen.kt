package net.torvald.terrarum.modulebasegame.worldgenerator

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
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import java.util.concurrent.Callable
import kotlin.experimental.and
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * New world generator.
 *
 * Created by minjaesong on 2019-09-02.
 */
object Worldgen {

    const val YHEIGHT_MAGIC = 2800.0 / 3.0
    const val YHEIGHT_DIVISOR = 2.0 / 7.0

    private const val ALPHA_1_2 = 0x0000_000004_000002

    fun GameWorld.getClampedHeight(): Int {
        val clampNum = when (INGAME.worldGenVer) {
            in 0L..ALPHA_1_2 -> 4500 // 4500 is the height on the HUGE setting until Alpha 1.2
            else -> 3200
        }
        return this.height.coerceAtMost(clampNum)
    }


    /** Will modify the Terragen as if the height of the world is strictly 3200 (see [GameWorld.getClampedHeight]) */
    fun getSY(y: Int): Double = y - (world.getClampedHeight() - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant


    private lateinit var world: GameWorld
    lateinit var params: WorldgenParams
        private set

    val threadExecutor = TerrarumIngame.worldgenThreadExecutor

    private val threadLock = java.lang.Object()

    fun attachMap(world: GameWorld, genParams: WorldgenParams) {
        this.world = world
        params = genParams

        highlandLowlandSelectCache = getHighlandLowlandSelectCache(params.terragenParams, params.seed)
        landBlock = getLandBlock(params.terragenParams, params.seed)
        caveAttenuateBiasScaledForOresCache = getCaveAttenuateBiasScaled(highlandLowlandSelectCache, params.terragenParams)
        groundScalingCached = getGroundScalingCached(highlandLowlandSelectCache, params.terragenParams)
        biomeMap = HashMap()

        prepareWorldgen(params.seed, params.terragenParams)
    }

    internal lateinit var groundScalingCached: ModuleCache
    internal lateinit var landBlock: ModuleCache
    internal lateinit var highlandLowlandSelectCache: ModuleCache
    internal lateinit var caveAttenuateBiasScaledForOresCache: ModuleCache
    internal lateinit var biomeMap: HashMap<BlockAddress, Byte>


    /**
     * Other modules are free to add their own ores to the world generator.
     */
    fun registerOre(oreInfo: OregenParams) {
        oreRegistry.add(oreInfo)
    }

    private val oreRegistry = ArrayList<OregenParams>()

    // called by attachMap, which in turn called by enterCreateNewWorld and enterLoadFromSave
    fun prepareWorldgen(seed: Long, terragenParams: TerragenParams) {
        terragenParams.pregenerateStrataCache(seed)
    }

    fun getJobs(tags: List<String> = emptyList()): List<Work> {
        val oreTilingModes = HashMap<ItemID, String>().also {
            it.putAll(oreRegistry.map { it.tile to it.tiling })
        }

        val tagFilter = if (tags.isEmpty()) { _: Work -> true }
        else { work: Work -> (work.tags union tags).isNotEmpty() }

        return listOf(
            Work(Lang["MENU_IO_WORLDGEN_RETICULATING_SPLINES"], Terragen(world, false, groundScalingCached, landBlock, params.seed, params.terragenParams), listOf("TERRAIN")), // also generates marble veins
            Work(Lang["MENU_IO_WORLDGEN_CARVING_EARTH"], Cavegen(world, false, highlandLowlandSelectCache, params.seed, params.terragenParams), listOf("TERRAIN", "CAVE")),
            Work(Lang["MENU_IO_WORLDGEN_FLOODING_UNDERGROUND"], Aquagen(world, false, groundScalingCached, params.seed, params.terragenParams), listOf("WATER")),
            Work(Lang["MENU_IO_WORLDGEN_GROWING_MINERALS"], Oregen(world, false, caveAttenuateBiasScaledForOresCache, params.seed, oreRegistry, params.terragenParams), listOf("ORES")),
            Work(Lang["MENU_IO_WORLDGEN_POSITIONING_ROCKS"], OregenAutotiling(world, false, params.seed, oreTilingModes), listOf("ORES")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Biomegen(world, false, params.seed, params.biomegenParams, biomeMap), listOf("BIOME")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Treegen(world, true, params.seed, params.terragenParams, params.treegenParams, biomeMap), listOf("TREES")),
        ).filter(tagFilter)
    }

    fun generateMap(loadscreen: LoadScreenBase) {
        val t1 = System.nanoTime()
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

        // generated the missing chunks, 5x5 centred to the player
        val pcx = spawnX / CHUNK_W
        val pcy = spawnY / CHUNK_H
        App.getLoadScreen().addMessage(Lang["MENU_IO_WORLDGEN_CLEANING_UP"])

        val chunkgenJobs = listOf(
            Point2iMod(pcx - 1, pcy - 2), Point2iMod(pcx, pcy - 2), Point2iMod(pcx + 1, pcy - 2),
            Point2iMod(pcx - 2, pcy - 1), Point2iMod(pcx - 1, pcy - 1), Point2iMod(pcx, pcy - 1), Point2iMod(pcx + 1, pcy - 1),  Point2iMod(pcx + 2, pcy - 1),
            Point2iMod(pcx - 2, pcy), Point2iMod(pcx - 1, pcy), Point2iMod(pcx + 1, pcy),  Point2iMod(pcx + 2, pcy),
            Point2iMod(pcx - 2, pcy + 1), Point2iMod(pcx - 1, pcy + 1), Point2iMod(pcx, pcy + 1), Point2iMod(pcx + 1, pcy + 1),  Point2iMod(pcx + 2, pcy + 1),
            Point2iMod(pcx - 1, pcy + 2), Point2iMod(pcx, pcy + 2), Point2iMod(pcx + 1, pcy + 2),
        ).filter { it.y in 0 until world.height }.filter {  (cx, cy) ->
            if (cy !in 0 until world.height / CHUNK_H) false
            else (world.chunkFlags[cy][cx].and(0x7F) == 0.toByte())
        }.map { (cx, cy) ->
            Callable { generateChunkIngame(cx, cy, true) { cx, cy -> } }
        }

        Worldgen.threadExecutor.renew()
        threadExecutor.submitAll(chunkgenJobs)
        Worldgen.threadExecutor.join()


        val tDiff = System.nanoTime() - t1
        printdbg(this, "Generation job finished; time took: ${tDiff / 1000000000.0} seconds, bogoflops: ${App.bogoflops}")
    }

    private fun Point2iMod(x: Int, y: Int) = Point2i(x fmod (world.width / CHUNK_W), y)

    /**
     * Chunk flags will be set automatically
     */
    fun generateChunkIngame(cx: Int, cy: Int, join: Boolean = false, callback: (Int, Int) -> Unit) {
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
            if (join) it.join()
        }
    }

    data class Work(val loadingScreenName: String, val theWork: Gen, val tags: List<String>)

    fun getEstimationSec(width: Int, height: Int): Long {
        // test method: fresh new instance every worldgen
        // test machine is loaded with the IDE and Web Browser (Firefox) running 1 Twitch stream and 1 YouTube video, both at 1080p

        val testMachineBogoFlops = 47518464.58
        val testMachineThreads = 32
        // eq: ax^b
        val a = 0.05 // use google sheet to get trend line equation
        val b = 0.343 // use google sheet to get trend line equation

        val f = a * (width.toDouble() * height).pow(b)
        return (1.25 * (testMachineBogoFlops / bogoflops) * f * (testMachineThreads.toDouble() / THREAD_COUNT)).roundToLong()
    }

    /**
     * @return starting chunk Y index, ending chunk Y index (inclusive)
     */
    fun getChunkGenStrip(world: GameWorld): Pair<Int, Int> {
        val start = (0.00342f * world.getClampedHeight() - 3.22f).floorToInt().coerceAtLeast(1)
        // this value has to extend up, otherwise the player may spawn into the chopped-off mountaintop
        // this value has to extend down into the rock layer, otherwise, if the bottom of the bottom chunk is dirt, they will turn into grasses
        //     - the second condition is nullified with the new NOT-GENERATED markers on the terrain
        return start - 1 to start + 5
    }

    private val rockScoreMin = 40
    private val treeScoreMin = 25

    private fun Int.inChunk() = (this / CHUNK_W) % 2 == 0

    private fun getRandomX(): Int {
        var posX = (Math.random() * world.width).toInt()
        while (!posX.inChunk())
            posX = (Math.random() * world.width).toInt()

        return posX
    }

    private fun tryForSpawnPoint(world: GameWorld): Point2i {
        val yInit = getChunkGenStrip(world).first
        val tallies = ArrayList<Pair<Point2i, Vector3f>>() // xypos, score (0..1+)
        var tries = 0
        var found = false
        val ySearchRadius = 30
        while (tries < 99) {
            val posX = getRandomX()
            var posY = yInit * CHUNK_H
            // go up?
            if (BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                // go up!
                while (posY > ySearchRadius && BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                    posY -= 1
                }
            }
            else {
                // go down!
                while (posY < world.height - ySearchRadius && !BlockCodex[world.getTileFromTerrain(posX, posY)].isSolid) {
                    posY += 1
                }
            }

            printdbg(this, "Trying spawn point #${tries + 1} at ($posX, $posY)")

            if (posY !in ySearchRadius+1 until world.height - ySearchRadius) {
                printdbg(this, "...Survey says: X=$posX has no floor")
                tries += 1
                continue
            }

            var rocks = 0
            var trees = 0
            var flatness = 0
            // make survey
            for (sx in -80..80) {
                for (sy in -ySearchRadius..ySearchRadius) {
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

    private fun getLandBlock(params: TerragenParams, seed: Long): ModuleCache {
        val landBlock = ModuleScaleDomain().also {
            it.setSource(ModuleFractal().also {
                it.setType(ModuleFractal.FractalType.DECARPENTIERSWISS)
                it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
                it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
                it.setNumOctaves(2)
                it.setFrequency(params.landBlockScale / params.featureSize)
                it.seed = seed shake "LandBlock"
            })
            it.setScaleX(1.0 / 4.0)
            it.setScaleZ(1.0 / 4.0)
            it.setScaleY(1.0 / 18.0)
        }

        val landBlockPerturbFractal = ModuleScaleOffset().also {
            it.setSource(ModuleFractal().also {
                it.setType(ModuleFractal.FractalType.FBM)
                it.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
                it.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
                it.setNumOctaves(6)
                it.setFrequency((params.landBlockScale * 3.0) / params.featureSize)
                it.seed = seed shake "MassiveMassif"
            })
            it.setScale(24.0)
        }

        val landBlockPerturb = ModuleTranslateDomain().also {
            it.setSource(landBlock)
            it.setAxisXSource(landBlockPerturbFractal)
            it.setAxisZSource(landBlockPerturbFractal)
        }

        val landBlockClamp = ModuleClamp().also {
            it.setSource(ModuleScaleOffset().also {
                it.setSource(landBlockPerturb)
                it.setScale(1.0)
                it.setOffset(-1.0)
            })
            it.setRange(0.0, 1.0)
        }

        val landBlockCache = ModuleCache().also {
            it.setSource(landBlockClamp)
        }

        return landBlockCache
    }

    private fun getCaveAttenuateBiasScaled(highlandLowlandSelectCache: ModuleCache, params: TerragenParams): ModuleCache {
        val caveAttenuateBias1 = ModuleCache().also { it.setSource(ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias1) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        })}

        val caveAttenuateBiasForOres = ModuleScaleOffset().also {
            it.setSource(caveAttenuateBias1)
            it.setScale(params.caveAttenuateScale1)
        }

        val scale = ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveAttenuateBiasForOres)
        }

        return ModuleCache().also {
            it.setSource(scale)
        }
    }

    private fun getGroundScalingCached(highlandLowlandSelectCache: ModuleCache, params: TerragenParams): ModuleCache {
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

        return ModuleCache().also {
            it.setSource(groundScaling)
        }
    }

}

abstract class Gen(val world: GameWorld, val isFinal: Boolean, val seed: Long, val params: Any? = null) {

    open fun getDone(loadscreen: LoadScreenBase?) { } // trying to use different name so that it won't be confused with Runnable or Callable
    protected abstract fun getGenerator(seed: Long, params: Any?): List<Joise>
    protected abstract fun draw(xStart: Int, yStart: Int, noises: List<Joise>, soff: Double)

    private fun getChunksRange(): List<Long> {
        val (yStart, yEnd) = Worldgen.getChunkGenStrip(world)
        return (0 until world.width / CHUNK_W step 2).flatMap { cx -> // skip every other column because we can :smiling_face_with_horns:
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

sealed class WorldgenParams(
    val seed: Long,
    // optional parameters
    val terragenParams: TerragenParams,
    val biomegenParams: BiomegenParams,
    val treegenParams: TreegenParams,
) {
    companion object {
        fun getParamsByVersion(versionRaw: Long?, seed: Long): WorldgenParams {
            val versionRaw = versionRaw ?: 0x7FFF_FFFFFF_FFFFFF // use current version for null
            when (versionRaw) {
                in 0..0x0000_000004_000003 -> return WorldgenParamsAlpha1(seed) // 0.4.3 is a dev-only version
                in 0x0000_000004_000004..0x7FFF_FFFFFF_FFFFFF -> return WorldgenParamsAlpha2(seed) // 0.4.4 is also a dev-only version
                else -> throw IllegalArgumentException("Unknown version: $versionRaw")
            }
        }
    }
}
class WorldgenParamsAlpha1(seed: Long) : WorldgenParams(
    seed, TerragenParamsAlpha1(), BiomegenParams(), TreegenParams(),
)
class WorldgenParamsAlpha2(seed: Long) : WorldgenParams(
    seed, TerragenParamsAlpha2(), BiomegenParams(), TreegenParams(),
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

val ONE_HALF_PI = 4.71238898038469
val TWO_PI = 6.283185307179586
val HALF_PI = 1.5707963267948966
val QUARTER_PI = 0.7853981633974483
val FOUR_PI = 12.566370614359172
