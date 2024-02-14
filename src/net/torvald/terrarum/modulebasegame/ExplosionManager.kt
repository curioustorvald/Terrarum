package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.getOffset
import net.torvald.terrarum.tryDispose
import net.torvald.unsafe.UnsafeHelper

/**
 * Created by minjaesong on 2024-02-13.
 */
object ExplosionManager {

    private const val CALC_RADIUS = 127
    private const val CALC_WIDTH = CALC_RADIUS * 2 + 1

    fun goBoom(world: GameWorld, tx: Int, ty: Int, power: Float, callback: () -> Unit) {
        // create a copy of the tilemap
        val tilemap = BlockLayerI16(CALC_WIDTH, CALC_WIDTH)

        // fill in the tilemap copy
        for (line in 0 until CALC_WIDTH) {
            memcpyFromWorld(world, tx - CALC_RADIUS, ty - CALC_RADIUS, line, tilemap)
        }

        createExplosionWorker(tilemap, tx, ty, power, world, callback).start()
    }

    private fun memcpyFromWorld(world: GameWorld, xStart: Int, yStart: Int, yOff: Int, out: BlockLayerI16) {
        // if the bounding box must wrap around
        if (xStart > world.width - CALC_RADIUS) {
            val lenLeft = world.width - xStart
            val lenRight = CALC_WIDTH - lenLeft

            UnsafeHelper.memcpy(
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(xStart, yStart + yOff),
                out.ptr,
                out.getOffset(0, yOff),
                world.layerTerrain.bytesPerBlock * lenLeft
            )
            UnsafeHelper.memcpy(
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(0, yStart + yOff),
                out.ptr,
                out.getOffset(lenLeft, yOff),
                world.layerTerrain.bytesPerBlock * lenRight
            )
        }
        else {
            UnsafeHelper.memcpy(
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(xStart, yStart + yOff),
                out.ptr,
                out.getOffset(0, yOff),
                world.layerTerrain.bytesPerBlock * CALC_WIDTH
            )
        }
    }

    /**
     * @param tilemap a portion copy of the tilemap from the world, centred to the explosive
     * @param tx tilewise centre-x of the explosive
     * @param ty tilewise centre-y of the explosive
     * @param outWorld world object to write the result to
     */
    private fun createExplosionWorker(tilemap: BlockLayerI16, tx: Int, ty: Int, power: Float, outWorld: GameWorld, callback: () -> Unit): Thread {
        return Thread {
            // simulate explosion like lightmaprenderer


            // write to the world


            // dispose of the tilemap copy
            tilemap.tryDispose()

            callback()
        }
    }

}