package net.torvald.terrarum.blockstats

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.abs
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_HEIGHT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_WIDTH
import net.torvald.terrarum.sqr
import net.torvald.terrarum.toInt
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object MinimapComposer : Disposable {

    private val threadExecutor = ThreadExecutor(maxOf(1, App.THREAD_COUNT.times(2).div(3)))

    const val SQUARE_SIZE = 13 // preferably in odd number

    val MINIMAP_TILE_WIDTH = (MINIMAP_WIDTH.toInt() * 3) / SQUARE_SIZE + 4
    val MINIMAP_TILE_HEIGHT = (MINIMAP_HEIGHT.toInt() * 3) / SQUARE_SIZE + 4

    private var world: GameWorld = GameWorld.makeNullWorld()

    fun setWorld(world: GameWorld) {
        try {
            if (this.world != world) {
                App.printdbg(this, "World change detected -- old world: ${this.world.hashCode()}, new world: ${world.hashCode()}")

                // TODO, also set totalWidth/Height
            }
        }
        catch (e: UninitializedPropertyAccessException) {
            // new init, do nothing
        }
        finally {
            this.world = world
        }
    }

    val pixmaps = Array(SQUARE_SIZE*SQUARE_SIZE) { Pixmap(MINIMAP_TILE_WIDTH, MINIMAP_TILE_HEIGHT, Pixmap.Format.RGBA8888) }

    private val updaterQueue = Array<Callable<Unit>?>(pixmaps.size) { null }

    private val spiralIndices = ArrayList<Int>()

    init {
        val X = SQUARE_SIZE
        val Y = SQUARE_SIZE
        var x = 0
        var y = 0
        var dx = 0
        var dy = -1

        (0 until SQUARE_SIZE*SQUARE_SIZE).forEach {
//            if ((-(X/2) <= x && x <= X/2) && (-(Y/2) <= y && y <= Y/2)) // just in case it's not a square
                spiralIndices.add((y + Y/2) * X + (x + X/2))
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1-y)) {
                val d1 = -dy
                val d2 = dx
                dx = d1
                dy = d2
            }
            x += dx
            y += dy
        }
    }

    private infix fun Int.pow(exp: Int): Int {
        var exp = exp
        var base = this
        var result = 1
        while (true) {
            if (exp and 1 != 0)
                result *= base
            exp = exp shr 1
            if (exp.inv() != 0)
                break
            base *= base
        }

        return result
    }

    init {


        App.disposables.add(this)
    }

    /**
     * @param x player-centric
     * @param y player-centric
     */
    fun queueRender(x: Int, y: Int) {

        val udc = updaterQueue.sumOf { (it != null).toInt() }

        if (udc == 0) {
            val tlx = x - (MINIMAP_TILE_WIDTH / 2)
            val tly = y - (MINIMAP_TILE_HEIGHT / 2)

//        printdbg(this, "queue render - c($x,$y), tl($tlx,$tlx)")

            // make the queueing work
            // enqueue first
            spiralIndices.forEachIndexed { index, i ->
                val pixmap = pixmaps[i]

                val tx = tlx + (MINIMAP_TILE_WIDTH * ((i % SQUARE_SIZE) - (SQUARE_SIZE / 2)))
                val ty = tly + (MINIMAP_TILE_HEIGHT * ((i / SQUARE_SIZE) - (SQUARE_SIZE / 2)))

                updaterQueue[index] = Callable { createUpdater(tx, ty, pixmap, i).run() }
//                printdbg(this, "Queueing tilemap update ($tx,$ty) from queue[$i]")
            }

            // consume the queue
            /*for (k in currentThreads.indices) {
                if (currentThreads[k].state == Thread.State.TERMINATED && !updaterQueue.isEmpty) {
                    currentThreads[k] = Thread(updaterQueue.removeFirst(), "MinimapLivetilePainter")
                    printdbg(this, "Consuming from queue; queue size now: ${updaterQueue.size}")
                }
                if (currentThreads[k].state == Thread.State.NEW) {
                    currentThreads[k].start()
                }
            }*/


            /*updaterQueue.forEachIndexed { k, runnable ->
                if (runnable != null) {
                    currentThreads[k] = Thread(runnable, "MinimapLivetilePainter")
                    printdbg(this, "Consuming from queue[$k]")
                    currentThreads[k].start()
                }
            }*/


            threadExecutor.renew()
            // TODO submit in spiral index
            threadExecutor.submitAll(updaterQueue.filterNotNull())
            Thread { threadExecutor.join() }.start()

        }
        else {
//            printdbg(this, "$udc Threads still running, request disregarded")
        }

//        printdbg(this, "** $udc Threads still running **")
    }

    private val oobColTop = Color(0)
    private val oobColBtm = Color(0x202020FF)

    /**
     * @param tx top-left of the world
     * @param ty top-left of the world
     * @param pixmap pixmap to draw pixels on
     */
    private fun createUpdater(tx: Int, ty: Int, pixmap: Pixmap, index: Int) = Runnable {
        try {
            for (y in ty until ty + MINIMAP_TILE_HEIGHT) {
                for (x in tx until tx + MINIMAP_TILE_WIDTH) {
                    val tileTerr = world.getTileFromTerrain(x, y)
                    val wallTerr = world.getTileFromWall(x, y)
                    val colTerr = App.tileMaker.terrainTileColourMap.get(tileTerr)!!.toGdxColor()
                    val colWall = App.tileMaker.terrainTileColourMap.get(wallTerr)!!.toGdxColor().mul(App.tileMaker.wallOverlayColour)

                    val outCol = if (y < 0)
                        oobColTop
                    else if (y >= world.height)
                        oobColBtm
                    else if (colTerr.a > 0.1f) colTerr else colWall

                    pixmap.blending = Pixmap.Blending.None
                    pixmap.setColor(outCol)
                    pixmap.drawPixel(x - tx, y - ty)
                }
            }

            updaterQueue[index] = null
        }
        catch(e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun dispose() {
        pixmaps.forEach { it.dispose() }
    }

}