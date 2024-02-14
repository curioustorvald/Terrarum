package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.getOffset
import net.torvald.terrarum.tryDispose
import net.torvald.unsafe.UnsafeHelper
import kotlin.math.max
import kotlin.math.min

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

        createExplosionWorker(world, tilemap, tx, ty, power, world, callback).start()
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
     * @param tx tilewise centre-x of the explosive from the world
     * @param ty tilewise centre-y of the explosive from the world
     * @param outWorld world object to write the result to
     */
    private fun createExplosionWorker(world: GameWorld, tilemap: BlockLayerI16, tx: Int, ty: Int, power: Float, outWorld: GameWorld, callback: () -> Unit): Thread { return Thread {
        val lightmap = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // explosion power map
        val _mapThisTileOpacity = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // the tile strengths
        val _mapThisTileOpacity2 = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // the tile strengths
        val _mapLightLevelThis = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // the explosion powers
        var _thisTileLuminosity = 0f // ???
        var _ambientAccumulator = 0f // ambient explosion power
        var _thisTileOpacity = 0f
        var _thisTileOpacity2 = 0f


        fun _swipeTask(x: Int, y: Int, x2: Int, y2: Int, swipeDiag: Boolean) {//, distFromLightSrc: Ivec4) {
            if (x2 < 0 || y2 < 0 || x2 >= CALC_WIDTH || y2 >= CALC_WIDTH) return

            _ambientAccumulator = _mapLightLevelThis[x, y]

            if (!swipeDiag) {
                _thisTileOpacity = _mapThisTileOpacity[x, y]
                _ambientAccumulator = maxOf(_ambientAccumulator, lightmap[x2, y2] - _thisTileOpacity)
            }
            else {
                _thisTileOpacity2 = _mapThisTileOpacity2[x, y]
                _ambientAccumulator = maxOf(_ambientAccumulator, lightmap[x2, y2] - _thisTileOpacity2)
            }

            _mapLightLevelThis[x, y] = _ambientAccumulator
            lightmap[x, y] = _ambientAccumulator
        }
        fun swipeBoomPower(sx: Int, sy: Int, ex: Int, ey: Int, dx: Int, dy: Int, strengthmap: UnsafeFloatArray, swipeDiag: Boolean) {
            var swipeX = sx
            var swipeY = sy
            while (swipeX*dx <= ex*dx && swipeY*dy <= ey*dy) {
                // conduct the task #1
                // spread towards the end
                _swipeTask(swipeX, swipeY, swipeX-dx, swipeY-dy, swipeDiag)

                swipeX += dx
                swipeY += dy
            }

            swipeX = ex; swipeY = ey
            while (swipeX*dx >= sx*dx && swipeY*dy >= sy*dy) {
                // conduct the task #2
                // spread towards the start
                _swipeTask(swipeX, swipeY, swipeX+dx, swipeY+dy, swipeDiag)

                swipeX -= dx
                swipeY -= dy
            }
        }

        fun r1(strengthmap: UnsafeFloatArray) {
            for (line in 1 until CALC_WIDTH - 1) {
                swipeBoomPower(1, line, CALC_WIDTH - 2, line, 1, 0, strengthmap, false)
            }
        }
        fun r2(strengthmap: UnsafeFloatArray) {
            for (line in 1 until CALC_WIDTH - 1) {
                swipeBoomPower(line, 1, line, CALC_WIDTH - 2, 0, 1, strengthmap, false)
            }
        }
        fun r3(strengthmap: UnsafeFloatArray) {
            for (i in 0 until CALC_WIDTH + CALC_WIDTH - 5) {
                swipeBoomPower(
                    max(1, i - CALC_WIDTH + 4), max(1, CALC_WIDTH - 2 - i),
                    min(CALC_WIDTH - 2, i + 1), min(CALC_WIDTH - 2, (CALC_WIDTH + CALC_WIDTH - 5) - i),
                    1, 1, strengthmap, true
                )
            }
        }
        fun r4(strengthmap: UnsafeFloatArray) {
            for (i in 0 until CALC_WIDTH + CALC_WIDTH - 5) {
                swipeBoomPower(
                    max(1, i - CALC_WIDTH + 4), min(CALC_WIDTH - 2, i + 1),
                    min(CALC_WIDTH - 2, i + 1), max(1, (CALC_WIDTH - 2) - (CALC_WIDTH + CALC_WIDTH - 6) + i),
                    1, -1, strengthmap, true
                )
            }
        }

        val worldXstart = tx - CALC_RADIUS - 1
        val worldYstart = ty - CALC_RADIUS - 1

        // precalculate
        for (rawy in worldYstart until worldYstart + CALC_WIDTH) {
            for (rawx in worldXstart until worldXstart + CALC_WIDTH) {
                val lx = rawx - (tx - CALC_RADIUS - 1)
                val ly = rawy - (ty - CALC_RADIUS - 1)
                val (worldX, worldY) = world.coerceXY(rawx, rawy)

                val _thisTerrain = world.getTileFromTerrainRaw(worldX, worldY)
                val _thisTerrainProp = BlockCodex[world.tileNumberToNameMap[_thisTerrain.toLong()]]

                // create tile strength map
                _mapThisTileOpacity.set(lx, ly, _thisTerrainProp.strength.toFloat())
                _mapThisTileOpacity2.set(lx, ly, _thisTerrainProp.strength.toFloat() * 1.41421356f)

                // initialise explosion power map with the explosive
                if (tx == worldX && ty == worldY) {
                    _thisTileLuminosity = maxOf(_thisTileLuminosity, power)
                    _mapLightLevelThis.max(lx, ly, _thisTileLuminosity)
                }
            }
        }

        // simulate explosion like strengthmaprenderer
        r1(lightmap);r2(lightmap);r3(lightmap);r4(lightmap)
        r1(lightmap);r2(lightmap);r3(lightmap);r4(lightmap)

        // write lightmap to the tilemap
        for (rawy in worldYstart until worldYstart + CALC_WIDTH) {
            for (rawx in worldXstart until worldXstart + CALC_WIDTH) {
                val lx = rawx - (tx - CALC_RADIUS - 1)
                val ly = rawy - (ty - CALC_RADIUS - 1)
                world.inflictTerrainDamage(rawx, rawy, lightmap[lx, ly].toDouble())
            }
        }

        // memcpy the tilemap to the world

        // dispose of the tilemap copy
        lightmap.destroy()
        _mapThisTileOpacity.destroy()
        _mapThisTileOpacity2.destroy()
        _mapLightLevelThis.destroy()
        tilemap.tryDispose()

        callback()
    } }




    private class UnsafeFloatArray(val width: Int, val height: Int) {
        private val SIZE_IN_BYTES = 4L * width * height
        val array = UnsafeHelper.allocate(SIZE_IN_BYTES)

        init {
            array.fillWith(0)
        }

        private inline fun toAddr(x: Int, y: Int) = (width * y + x).toLong()

        operator fun set(x: Int, y: Int, value: Float) {
            array.setFloat(toAddr(x, y), value)
        }

        operator fun get(x: Int, y: Int) = array.getFloat(toAddr(x, y))
        fun max(x: Int, y: Int, value: Float) {
            set(x, y, maxOf(get(x, y), value))
        }

        fun destroy() = this.array.destroy()
    }

}