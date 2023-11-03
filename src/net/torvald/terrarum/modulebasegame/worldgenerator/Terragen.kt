package net.torvald.terrarum.modulebasegame.worldgenerator

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.XXHash32
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.LoadScreenBase
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.FancyWorldgenLoadScreen
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Created by minjaesong on 2019-07-23.
 */
class Terragen(world: GameWorld, val highlandLowlandSelectCache: ModuleCache, seed: Long, params: Any) : Gen(world, seed, params) {

    companion object {
        const val YHEIGHT_MAGIC = 2800.0 / 3.0
        const val YHEIGHT_DIVISOR = 2.0 / 7.0
    }

    private val dirtStoneDitherSize = 3 // actual dither size will be double of this value
    private val stoneSlateDitherSize = 4

    override fun getDone(loadscreen: LoadScreenBase) {
        loadscreen.stageValue += 1
        loadscreen.progress.set(0L)

        Worldgen.threadExecutor.renew()
        (0 until world.width).sliceEvenly(Worldgen.genSlices).mapIndexed { i, xs ->
            Worldgen.threadExecutor.submit {
                val localJoise = getGenerator(seed, params as TerragenParams)
                for (x in xs) {
                    val sampleTheta = (x.toDouble() / world.width) * TWO_PI
                    val sampleOffset = world.width / 8.0
                    draw(x, localJoise, sampleTheta, sampleOffset)
                }
                loadscreen.progress.addAndGet((xs.last - xs.first + 1).toLong())
            }
        }

        Worldgen.threadExecutor.join()

        printdbg(this, "Waking up Worldgen")
    }


    private val groundDepthBlock = listOf(
            Block.AIR, Block.DIRT, Block.STONE, Block.STONE_SLATE
    )

    private fun Double.tiered(vararg tiers: Double): Int {
        tiers.reversed().forEachIndexed { index, it ->
            if (this >= it) return (tiers.lastIndex - index) // why??
        }
        return tiers.lastIndex
    }

    //private fun draw(x: Int, y: Int, width: Int, height: Int, noiseValue: List<Double>, world: GameWorld) {
    private fun draw(x: Int, noises: List<Joise>, st: Double, soff: Double) {
        var dirtStoneTransition = 0
        var stoneSlateTransition = 0

        for (y in 0 until world.height) {
            val sx = sin(st) * soff + soff // plus sampleOffset to make only
            val sz = cos(st) * soff + soff // positive points are to be sampled
            val sy = y - (world.height - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR // Q&D offsetting to make ratio of sky:ground to be constant
            // DEBUG NOTE: it is the OFFSET FROM THE IDEAL VALUE (observed land height - (HEIGHT * DIVISOR)) that must be constant
            val noiseValue = noises.map { it.get(sx, sy, sz) }

            val terr = noiseValue[0].tiered(.0, .5, .88, 1.88)

            // mark off the position where the transition occurred
            if (dirtStoneTransition == 0 && terr == 2)
                dirtStoneTransition = y
            if (stoneSlateTransition == 0 && terr == 3)
                stoneSlateTransition = y

            val wallBlock = groundDepthBlock[terr]
            val terrBlock = wallBlock

            world.setTileTerrain(x, y, terrBlock, true)
            world.setTileWall(x, y, wallBlock, true)
        }

        // dither shits
        /*
        #
        # - dirt-to-cobble transition, height = dirtStoneDitherSize
        #
        %
        % - cobble-to-rock transition, height = dirtStoneDitherSize
        %
        * - where the stone layer actually begins
         */
        for (pos in 0 until dirtStoneDitherSize * 2) {
            val y = pos + dirtStoneTransition - (dirtStoneDitherSize * 2) + 1
            if (y >= world.height) break
            val hash = XXHash32.hashGeoCoord(x, y).and(0x7FFFFFFF) / 2147483647.0
//            val fore = world.getTileFromTerrain(x, y)
//            val back = world.getTileFromWall(x, y)
            val newTile = if (pos < dirtStoneDitherSize)
                if (hash < pos.toDouble() / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.DIRT
            else // don't +1 to pos.toDouble(); I've suffered
                if (hash >= (pos.toDouble() - dirtStoneDitherSize) / dirtStoneDitherSize) Block.STONE_QUARRIED else Block.STONE

//            if (fore != Block.AIR)
                world.setTileTerrain(x, y, newTile, true)

            world.setTileWall(x, y, newTile, true)
        }

        /*
        #
        # - stone-to-slate transition, height = stoneSlateDitherSize
        #
         */
        for (pos in 0 until stoneSlateDitherSize) {
            val y = pos + stoneSlateTransition - stoneSlateDitherSize + 1
            if (y >= world.height) break
            val hash = XXHash32.hashGeoCoord(x, y).and(0x7FFFFFFF) / 2147483647.0
//            val fore = world.getTileFromTerrain(x, y)
//            val back = world.getTileFromWall(x, y)
            val newTile = if (hash < pos.toDouble() / stoneSlateDitherSize) Block.STONE_SLATE else Block.STONE

//            if (fore != Block.AIR)
                world.setTileTerrain(x, y, newTile, true)

            world.setTileWall(x, y, newTile, true)
        }
    }


    private fun getGenerator(seed: Long, params: TerragenParams): List<Joise> {
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

        return listOf(
            Joise(groundScaling)
        )
    }
}

data class TerragenParams(
    val featureSize: Double = 333.0,
    val lowlandScaleOffset: Double = -0.65, // linearly alters the height
    val highlandScaleOffset: Double = -0.2, // linearly alters the height
    val mountainScaleOffset: Double = -0.1, // linearly alters the height
    val mountainDisturbance: Double = 0.7, // greater = more distortion, overhangs

    val caveShapeFreq: Double = 4.0, //adjust the "density" of the caves
    val caveAttenuateBias: Double = 0.90, // adjust the "concentration" of the cave gen. Lower = larger voids
    val caveSelectThre: Double = 0.918, // also adjust this if you've touched the bias value. Number can be greater than 1.0
    val caveBlockageFractalFreq: Double = 8.88,
    val caveBlockageSelectThre: Double = 1.40, // adjust cave cloing-up strength. Larger = more closing

//    val oreCopperFreq: Double = 0.024, // adjust the "density" of the ore veins
//    val oreCopperPower: Double = 0.01, // super-low value almost negates the depth element
//    val oreCopperScale: Double = 0.505,

//    val oreIronFreq: Double = 0.04, // adjust the "density" of the ore veins
//    val oreIronPower: Double = 0.01, // super-low value almost negates the depth element
//    val oreIronScale: Double = 0.505,


    // 0.01 - 0.505
    // 0.1 - 0.5
    // ...
    // 0.8 - 0.42
)