package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.OreCodex
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gameworld.BlockLayerInMemoryI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.unsafe.UnsafeHelper
import java.util.concurrent.*
import kotlin.math.*

/**
 * Created by minjaesong on 2024-02-13.
 */
object ExplosionManager {

    private val executor = Executors.newSingleThreadExecutor()
    private val futures = ArrayList<Future<*>>()
    private val runners = ArrayList<CallableWithState>()

    data class CallableWithState(
        val runner: Callable<Unit>,
        var executed: Boolean = false
    )

    fun goBoom(world: GameWorld, tx: Int, ty: Int, power: Float, dropProbNonOre: Float, dropProbOre: Float, callback: () -> Unit) {
        val CALC_RADIUS = power.ceilToInt() + 2
        val CALC_WIDTH = CALC_RADIUS * 2 + 1

        val lineMin = CALC_RADIUS - ty
        val lineMax = world.height - ty + CALC_RADIUS

        // create a copy of the tilemap
        val tilemap = BlockLayerInMemoryI16(CALC_WIDTH, CALC_WIDTH)
        val breakmap = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH)

        // fill in the tilemap copy
        for (line0 in 0 until CALC_WIDTH) {
            val line = line0.coerceIn(lineMin until lineMax)
            memcpyFromWorldTiles(CALC_RADIUS, CALC_WIDTH, world, tx - CALC_RADIUS, ty - CALC_RADIUS, line, tilemap)
            memcpyFromWorldBreakage(CALC_WIDTH, world, tx - CALC_RADIUS, ty - CALC_RADIUS, line, breakmap)
        }

        val runner = createExplosionWorker(CALC_RADIUS, CALC_WIDTH, world, breakmap, tilemap, tx, ty, power, dropProbNonOre, dropProbOre, callback)
//        futures.add(executor.submit(runner))

        runners.removeIf { it.executed }
        runners.add(CallableWithState(runner))
    }

    init {
        Thread {
            while (true) {
                try {
                    runners.toList().firstOrNull { !it.executed }?.let { job ->
                        val executor = Executors.newSingleThreadExecutor()
                        executor.submit(job.runner).get(500L, TimeUnit.MILLISECONDS)
                        executor.shutdownNow()
                        job.executed = true
                    }
                }
                catch (_: TimeoutException) { }

                Thread.sleep(50L)
            }
        }.start()
    }

    private fun memcpyFromWorldTiles(CALC_RADIUS: Int, CALC_WIDTH: Int, world: GameWorld, xStart: Int, yStart: Int, yOff: Int, out: BlockLayerInMemoryI16) {
        // if the bounding box must wrap around
        /*if (xStart > world.width - CALC_RADIUS) {
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
        }*/
        // temporary: copy one by one
        for (ox in xStart until xStart + CALC_WIDTH) {
            val (x, y) = world.coerceXY(ox, yStart + yOff)
            val tileInWorld = world.layerTerrain.unsafeGetTile(x, y)
            out.unsafeSetTile(x, y, tileInWorld)
        }
    }

    private fun memcpyToWorldTiles(CALC_RADIUS: Int, CALC_WIDTH: Int, world: GameWorld, xStart: Int, yStart: Int, yOff: Int, out: BlockLayerInMemoryI16) {
        // if the bounding box must wrap around
        /*if (xStart > world.width - CALC_RADIUS) {
            val lenLeft = world.width - xStart
            val lenRight = CALC_WIDTH - lenLeft

            UnsafeHelper.memcpy(
                out.ptr,
                out.getOffset(0, yOff),
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(xStart, yStart + yOff),
                world.layerTerrain.bytesPerBlock * lenLeft
            )
            UnsafeHelper.memcpy(
                out.ptr,
                out.getOffset(lenLeft, yOff),
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(0, yStart + yOff),
                world.layerTerrain.bytesPerBlock * lenRight
            )
        }
        else {
            UnsafeHelper.memcpy(
                out.ptr,
                out.getOffset(0, yOff),
                world.layerTerrain.ptr,
                world.layerTerrain.getOffset(xStart, yStart + yOff),
                world.layerTerrain.bytesPerBlock * CALC_WIDTH
            )
        }*/
        // temporary: copy one by one
        for (ox in xStart until xStart + CALC_WIDTH) {
            val (x, y) = world.coerceXY(ox, yStart + yOff)
            val tileInWorld = out.unsafeGetTile(x, y)
            world.layerTerrain.unsafeSetTile(x, y, tileInWorld)
        }
    }

    private fun memcpyFromWorldBreakage(CALC_WIDTH: Int, world: GameWorld, xStart: Int, yStart: Int, yOff: Int, out: UnsafeFloatArray) {
        for (x in xStart until xStart + CALC_WIDTH) {
            out[x - xStart, yOff] = world.getTerrainDamage(x, yStart + yOff)
        }
    }

    private fun memcpyToWorldBreakage(CALC_WIDTH: Int, world: GameWorld, xStart: Int, yStart: Int, yOff: Int, out: UnsafeFloatArray) {
        for (x in xStart until xStart + CALC_WIDTH) {
            world.inflictTerrainDamage(x, yStart + yOff, out[x - xStart, yOff].toDouble(), false)
        }
    }

    /**
     * @param tilemap a portion copy of the tilemap from the world, centred to the explosive
     * @param tx tilewise centre-x of the explosive from the world
     * @param ty tilewise centre-y of the explosive from the world
     * @param outWorld world object to write the result to
     */
    private fun createExplosionWorker(
        CALC_RADIUS: Int, CALC_WIDTH: Int,
        world: GameWorld,
        breakmap: UnsafeFloatArray,
        tilemap: BlockLayerInMemoryI16,
        tx: Int, ty: Int,
        power: Float,
        dropProbNonOre: Float,
        dropProbOre: Float,
        callback: () -> Unit): Callable<Unit>
    { return Callable {
        val mapBoomPow = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // explosion power map
        val mapTileStr = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // the tile strengths
        val mapTileStr2 = UnsafeFloatArray(CALC_WIDTH, CALC_WIDTH) // the tile strengths
        var boomPow = 0f // ???
        var ambientAccumulator = 0f // ambient explosion power
        var tileStr = 0f
        var tileStr2 = 0f


        fun _swipeTask(x: Int, y: Int, xOld: Int, yOld: Int, swipeDiag: Boolean) {//, distFromLightSrc: Ivec4) {
            if (xOld < 0 || yOld < 0 || xOld >= CALC_WIDTH || yOld >= CALC_WIDTH) return

            ambientAccumulator = mapBoomPow[x, y]

            if (!swipeDiag) {
                tileStr = mapTileStr[x, y]
                ambientAccumulator = maxOf(ambientAccumulator, mapBoomPow[xOld, yOld] - tileStr)
            }
            else {
                tileStr2 = mapTileStr2[x, y]
                ambientAccumulator = maxOf(ambientAccumulator, mapBoomPow[xOld, yOld] - tileStr2)
            }

            mapBoomPow[x, y] = ambientAccumulator
        }
        fun swipeBoomPower(sx: Int, sy: Int, ex: Int, ey: Int, dx: Int, dy: Int, swipeDiag: Boolean) {
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

        fun r1() {
            for (line in 1 until CALC_WIDTH - 1) {
                swipeBoomPower(1, line, CALC_WIDTH - 2, line, 1, 0, false)
            }
        }
        fun r2() {
            for (line in 1 until CALC_WIDTH - 1) {
                swipeBoomPower(line, 1, line, CALC_WIDTH - 2, 0, 1, false)
            }
        }
        fun r3() {
            for (i in 0 until CALC_WIDTH + CALC_WIDTH - 5) {
                swipeBoomPower(
                    max(1, i - CALC_WIDTH + 4), max(1, CALC_WIDTH - 2 - i),
                    min(CALC_WIDTH - 2, i + 1), min(CALC_WIDTH - 2, (CALC_WIDTH + CALC_WIDTH - 5) - i),
                    1, 1, true
                )
            }
        }
        fun r4() {
            for (i in 0 until CALC_WIDTH + CALC_WIDTH - 5) {
                swipeBoomPower(
                    max(1, i - CALC_WIDTH + 4), min(CALC_WIDTH - 2, i + 1),
                    min(CALC_WIDTH - 2, i + 1), max(1, (CALC_WIDTH - 2) - (CALC_WIDTH + CALC_WIDTH - 6) + i),
                    1, -1, true
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

                val thisTerrain = world.getTileFromTerrainRaw(worldX, worldY)
                val thisTerrainProp = BlockCodex[world.tileNumberToNameMap[thisTerrain.toLong()]]
                val thisTerrainDmg = breakmap[lx, ly]//world.getTerrainDamage(worldX, worldY)

                // create tile strength map
                mapTileStr[lx, ly] = thisTerrainProp.strength.toFloat().minus(thisTerrainDmg).toBlastResistance()
                mapTileStr2[lx, ly] = thisTerrainProp.strength.toFloat().minus(thisTerrainDmg).times(1.4142135f.blastToDmg()).toBlastResistance()

                // initialise explosion power map with the explosive
                if (tx == worldX && ty == worldY) {
                    boomPow = maxOf(boomPow, power)
                    mapBoomPow.max(lx, ly, boomPow)
                }
            }
        }

        // simulate explosion like strengthmaprenderer
        r1();r2();r3();r4()
        r1();r2();r3();r4()

        //// just write to the damagemap lol
        for (wy in worldYstart until worldYstart + CALC_WIDTH) {
            for (wx in worldXstart until worldXstart + CALC_WIDTH) {
                val lx = wx - (tx - CALC_RADIUS - 1)
                val ly = wy - (ty - CALC_RADIUS - 1)
                world.inflictTerrainDamage(wx, wy, mapBoomPow[lx, ly].blastToDmg().toDouble(), false).let { (tile, ore) ->
                    if (ore != null || tile != null) {
                        // drop item
                        val prob = if (ore != null) dropProbOre else dropProbNonOre
                        if (Math.random() < prob) {
                            val drop = if (ore != null) OreCodex[ore].item else BlockCodex[tile].drop
                            PickaxeCore.dropItem(drop, wx, wy)
                        }
                    }

                    if (tile != null) {
                        PickaxeCore.makeDust(tile, wx, wy, 8 + (5 * Math.random()).toInt())

                        // drop random disc
                        val itemprop = ItemCodex[tile]
                        if (Math.random() < (1.0 / 4096.0) * (dropProbNonOre) && // prob: 1/16384
                            (itemprop?.hasTag("CULTIVABLE") == true ||
                                    itemprop?.hasTag("SAND") == true ||
                                    itemprop?.hasTag("GRAVEL") == true)
                        ) {
                            PickaxeCore.dropItem(PickaxeCore.getRandomDisc(), wx, wy)
                        }
                    }
                }
            }
        }

        // dispose of the tilemap copy
        mapBoomPow.destroy()
        breakmap.destroy()
        mapTileStr.destroy()
        mapTileStr2.destroy()
        tilemap.ptr.destroy()
        breakmap.destroy()

        callback()
    } }


    private val q = 2.828427f
    private fun Float.toBlastResistance() = if (this <= 0f) 0f else this.pow(1f / q)
    private fun Float.blastToDmg() = if (this <= 0f) 0f else this.pow(q)


    private class UnsafeFloatArray(val width: Int, val height: Int) {
        val bytesPerBlock: Long = 4L
        private val SIZE_IN_BYTES = bytesPerBlock * width * height
        val array = UnsafeHelper.allocate(SIZE_IN_BYTES)
        val ptr get() = array

        init {
            array.fillWith(0)
        }

        private inline fun toAddr(x: Int, y: Int) = (width * y + x).toLong()
        inline fun getOffset(x: Int, y: Int) = bytesPerBlock * toAddr(x, y)

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