package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.module.*
import net.torvald.random.XXHash64
import net.torvald.terrarum.App
import net.torvald.terrarum.App.*
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import kotlin.math.max
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
    var genSlices = -1

    private val threadLock = java.lang.Object()

    fun attachMap(world: GameWorld, genParams: WorldgenParams) {
        this.world = world
        params = genParams
    }

    internal lateinit var highlandLowlandSelectCache: ModuleCache
    internal lateinit var caveAttenuateBiasScaled: ModuleScaleDomain
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
            Work(Lang["MENU_IO_WORLDGEN_RETICULATING_SPLINES"], Terragen(world, highlandLowlandSelectCache, params.seed, params.terragenParams), listOf("TERRAIN")),
            Work(Lang["MENU_IO_WORLDGEN_GROWING_MINERALS"], Oregen(world, caveAttenuateBiasScaled, params.seed, oreRegistry), listOf("ORES")),
            Work(Lang["MENU_IO_WORLDGEN_POSITIONING_ROCKS"], OregenAutotiling(world, params.seed, oreTilingModes), listOf("ORES")),
            Work(Lang["MENU_IO_WORLDGEN_CARVING_EARTH"], Cavegen(world, highlandLowlandSelectCache, params.seed, params.terragenParams), listOf("TERRAIN", "CAVE")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Biomegen(world, params.seed, params.biomegenParams, biomeMap), listOf("BIOME")),
            Work(Lang["MENU_IO_WORLDGEN_PAINTING_GREEN"], Treegen(world, params.seed, params.terragenParams, params.treegenParams, biomeMap), listOf("TREES")),
        ).filter(tagFilter)
    }

    fun generateMap(loadscreen: LoadScreenBase) {
        highlandLowlandSelectCache = getHighlandLowlandSelectCache(params.terragenParams, params.seed)
        caveAttenuateBiasScaled = getCaveAttenuateBiasScaled(highlandLowlandSelectCache, params.terragenParams)
        biomeMap = HashMap()

        genSlices = world.width / 9


        val jobs = getJobs()


        for (i in jobs.indices) {
            printdbg(this, "Worldgen: job #${i+1}")

            val it = jobs[i]

            App.getLoadScreen().addMessage(it.loadingScreenName)
            it.theWork.getDone(loadscreen)
        }

        // determine spawn point
        world.spawnX = 0
        world.spawnY = 180
        // go up?
        if (BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
            // go up!
            while (BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
                world.spawnY -= 1
            }
        }
        else {
            // go down!
            while (!BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
                world.spawnY += 1
            }
        }

        printdbg(this, "Generation job finished")

    }

    data class Work(val loadingScreenName: String, val theWork: Gen, val tags: List<String>)

    fun getEstimationSec(width: Int, height: Int): Long {
        return (30.0 * 1.25 * (48600000.0 / bogoflops) * ((width * height) / 20095000.0) * (32.0 / THREAD_COUNT)).roundToLong()
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

    private fun getCaveAttenuateBiasScaled(highlandLowlandSelectCache: ModuleCache, params: TerragenParams): ModuleScaleDomain {
        val caveAttenuateBias = ModuleBias().also {
            it.setSource(highlandLowlandSelectCache)
            it.setBias(params.caveAttenuateBias) // (0.5+) adjust the "concentration" of the cave gen. Lower = larger voids
        }

        return ModuleScaleDomain().also {
            it.setScaleX(1.0 / params.featureSize) // adjust this value to change features size
            it.setScaleY(1.0 / params.featureSize)
            it.setScaleZ(1.0 / params.featureSize)
            it.setSource(caveAttenuateBias)
        }
    }

}

abstract class Gen(val world: GameWorld, val seed: Long, val params: Any? = null) {
    open fun getDone(loadscreen: LoadScreenBase) { } // trying to use different name so that it won't be confused with Runnable or Callable
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
