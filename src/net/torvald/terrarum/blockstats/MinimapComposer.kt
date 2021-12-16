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
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_HEIGHT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_WIDTH
import net.torvald.terrarum.toInt
import java.util.concurrent.atomic.AtomicInteger

object MinimapComposer : Disposable {

    private val threadExecutor = ThreadExecutor(maxOf(1, App.THREAD_COUNT.times(2).div(3)))

    val MINIMAP_TILE_WIDTH = MINIMAP_WIDTH.toInt() + 16
    val MINIMAP_TILE_HEIGHT = MINIMAP_HEIGHT.toInt() + 16


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

    val pixmaps = Array(9) { Pixmap(MINIMAP_TILE_WIDTH, MINIMAP_TILE_HEIGHT, Pixmap.Format.RGBA8888) }

    private val updaterQueue = Array<Runnable?>(pixmaps.size) { null }
    private var currentThreads = Array(9) { Thread() }


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
            pixmaps.forEachIndexed { i, pixmap ->
                val tx = tlx + (MINIMAP_TILE_WIDTH * ((i % 3) - 1))
                val ty = tly + (MINIMAP_TILE_HEIGHT * ((i / 3) - 1))

                updaterQueue[i] = createUpdater(tx, ty, pixmap, i)
                printdbg(this, "Queueing tilemap update ($tx,$ty) from queue[$i]")
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

            updaterQueue.forEachIndexed { k, runnable ->
                if (runnable != null) {
                    currentThreads[k] = Thread(runnable, "MinimapLivetilePainter")
                    printdbg(this, "Consuming from queue[$k]")
                    currentThreads[k].start()
                }
            }
        }
        else {
            printdbg(this, "$udc Threads still running, request disregarded")
        }

        printdbg(this, "** $udc Threads still running **")
    }

    private val HQRNG = net.torvald.random.HQRNG()

    private val testcols = arrayOf(
            Color.CORAL, Color.LIME, Color.CYAN,
            Color.YELLOW, Color.SKY, Color.GOLD,
            Color.BROWN, Color.DARK_GRAY, Color.RED
    ).map { Color(it.lerp(Color.WHITE, 0.5f)) }

    /**
     * @param tx top-left of the world
     * @param ty top-left of the world
     * @param pixmap pixmap to draw pixels on
     */
    private fun createUpdater(tx: Int, ty: Int, pixmap: Pixmap, index: Int) = Runnable {
        for (y in ty until ty + MINIMAP_TILE_HEIGHT) {
            for (x in tx until tx + MINIMAP_TILE_WIDTH) {
                val tileTerr = world.getTileFromTerrain(x, y)
                val wallTerr = world.getTileFromWall(x, y)
                val colTerr = App.tileMaker.terrainTileColourMap.get(tileTerr)!!.toGdxColor()
                val colWall = App.tileMaker.terrainTileColourMap.get(wallTerr)!!.toGdxColor().mul(App.tileMaker.wallOverlayColour)

                val outCol = if (colTerr.a > 0.1f) colTerr else colWall

                pixmap.blending = Pixmap.Blending.None
//                pixmap.setColor(outCol)
                pixmap.setColor(outCol.mul(testcols[index]))
                pixmap.drawPixel(x - tx, y - ty)
            }
        }

        updaterQueue[index] = null
    }

    override fun dispose() {
        pixmaps.forEach { it.dispose() }
    }

}