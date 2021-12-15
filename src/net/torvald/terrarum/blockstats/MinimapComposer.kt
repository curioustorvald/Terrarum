package net.torvald.terrarum.blockstats

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_HEIGHT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryMinimap.Companion.MINIMAP_WIDTH

object MinimapComposer : Disposable {

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

    private val updaterQueue = Queue<Runnable>(pixmaps.size)
    private var currentThreads = Array(maxOf(1, App.THREAD_COUNT.times(2).div(3))) {
        Thread()
    }

    init {


        App.disposables.add(this)
    }

    /**
     * @param x player-centric
     * @param y player-centric
     */
    fun queueRender(x: Int, y: Int) {

        val tlx = x - (MINIMAP_TILE_WIDTH / 2)
        val tly = y - (MINIMAP_TILE_HEIGHT / 2)

//        printdbg(this, "queue render - c($x,$y), tl($tlx,$tlx)")

        // make the queueing work
        // enqueue first
        pixmaps.forEachIndexed { i, pixmap ->
            val tx = tlx + (MINIMAP_TILE_WIDTH * ((i % 3) - 1))
            val ty = tly + (MINIMAP_TILE_HEIGHT * ((i / 3) - 1))

            updaterQueue.addLast(createUpdater(tx, ty, pixmap))
            printdbg(this, "Queueing tilemap update ($tx,$ty); queue size now: ${updaterQueue.size}")
        }

        // consume the queue
        for (k in currentThreads.indices) {
            if (currentThreads[k].state == Thread.State.TERMINATED && !updaterQueue.isEmpty) {
                currentThreads[k] = Thread(updaterQueue.removeFirst(), "MinimapLivetilePainter")
                printdbg(this, "Consuming from queue; queue size now: ${updaterQueue.size}")
            }
            if (currentThreads[k].state == Thread.State.NEW) {
                currentThreads[k].start()
            }
        }
    }

    private val HQRNG = net.torvald.random.HQRNG()

    /**
     * @param tx top-left
     * @param ty top-left
     * @param pixmap pixmap to draw pixels on
     */
    private fun createUpdater(tx: Int, ty: Int, pixmap: Pixmap) = Runnable {
        for (y in ty until ty + MINIMAP_TILE_HEIGHT) {
            for (x in tx until tx + MINIMAP_TILE_WIDTH) {
                val tileTerr = world.getTileFromTerrain(x, y)
                val wallTerr = world.getTileFromWall(x, y)
                val colTerr = App.tileMaker.terrainTileColourMap.get(tileTerr)!!.toGdxColor()
                val colWall = App.tileMaker.terrainTileColourMap.get(wallTerr)!!.toGdxColor().mul(App.tileMaker.wallOverlayColour)

                val outCol = if (colTerr.a > 0.1f) colTerr else colWall

                pixmap.blending = Pixmap.Blending.None
                pixmap.setColor(outCol)
                //pixmap.setColor(Color.CORAL)
                pixmap.drawPixel(x, y)
            }
        }
    }

    override fun dispose() {
        pixmaps.forEach { it.dispose() }
    }

}