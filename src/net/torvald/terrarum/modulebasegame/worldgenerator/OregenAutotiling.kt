package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.Point2i
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.gameitems.isOre
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.OrePlacement
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import kotlin.math.max

/**
 * Created by minjaesong on 2023-10-26.
 */
class OregenAutotiling(world: GameWorld, seed: Long) : Gen(world, seed) {

    private val threadExecutor = TerrarumIngame.worldgenThreadExecutor
    private val genSlices = max(threadExecutor.threadCount, world.width / 8)

    override fun getDone() {
        threadExecutor.renew()
        (0 until world.width).sliceEvenly(genSlices).mapIndexed { i, xs ->
            threadExecutor.submit {
                for (x in xs) {
                    draw(x)
                }
            }
        }

        threadExecutor.join()
    }


    private fun draw(x: Int) {
        for (y in 0 until world.height) {
            val (ore, _) = world.getTileFromOre(x, y)

            if (ore.isOre()) {
                // get placement (tile connection) info
                val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                    acc or (placement.item == ore).toInt(index)
                }
                val placement = BlocksDrawer.connectLut16[autotiled]

                // actually put the ore block
                world.setTileOre(x, y, ore, placement) // autotiling will be handled by the other worldgen process
            }
        }
    }


    private fun getNearbyTilesPos8(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x + 1, y + 1),
            Point2i(x, y + 1),
            Point2i(x - 1, y + 1),
            Point2i(x - 1, y),
            Point2i(x - 1, y - 1),
            Point2i(x, y - 1),
            Point2i(x + 1, y - 1)
        )
    }
    private fun getNearbyOres8(x: Int, y: Int): List<OrePlacement> {
        return getNearbyTilesPos8(x, y).map { world.getTileFromOre(it.x, it.y) }
    }

}