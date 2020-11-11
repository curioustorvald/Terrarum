package net.torvald.terrarum.blockstats

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.CreateTileAtlas

object MinimapComposer : Disposable {

    // strategy: mosaic the textures, maximum texture size is 4 096.


    private var world: GameWorld = GameWorld.makeNullWorld()

    fun setWorld(world: GameWorld) {
        try {
            if (this.world != world) {
                AppLoader.printdbg(this, "World change detected -- old world: ${this.world.hashCode()}, new world: ${world.hashCode()}")

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

    var tempTex = Texture(1,1,Pixmap.Format.RGBA8888)
    // total size of the minimap. Remember: textures can be mosaic-ed to display full map.
    var totalWidth = 0
    var totalHeight = 0

    /** World coord for top-left side of the tileslot. ALWAYS multiple of LIVETILE_SIZE */
    var topLeftCoordX = 0
    /** World coord for top-left side of the tileslot. ALWAYS multiple of LIVETILE_SIZE */
    var topLeftCoordY = 0

    const val LIVETILE_SIZE = 64
    const val DISPLAY_CANVAS_WIDTH = 2048 // must be divisible by LIVETILE_SIZE
    const val DISPLAY_CANVAS_HEIGHT = 1024 // must be divisible by LIVETILE_SIZE
    val minimap = Pixmap(DISPLAY_CANVAS_WIDTH, DISPLAY_CANVAS_HEIGHT, Pixmap.Format.RGBA8888)
    const val TILES_IN_X = DISPLAY_CANVAS_WIDTH / LIVETILE_SIZE
    const val TILES_IN_Y = DISPLAY_CANVAS_HEIGHT / LIVETILE_SIZE
    // numbers inside of it will change a lot
    private val tilemap = Array(TILES_IN_Y) { y -> IntArray(TILES_IN_X) { x -> y * TILES_IN_X + x } }
    // pixmaps inside of this will never be redefined
    private val liveTiles = Array(TILES_IN_X * TILES_IN_Y) { Pixmap(LIVETILE_SIZE, LIVETILE_SIZE, Pixmap.Format.RGBA8888) }
    // indices are exacly the same as liveTiles
    private val liveTilesMeta = Array(TILES_IN_X * TILES_IN_Y) { LiveTileMeta(revalidate = true) }

    private val updaterQueue = Queue<Runnable>(TILES_IN_X * TILES_IN_Y * 2)
    private var currentThreads = Array(maxOf(1, AppLoader.THREAD_COUNT.times(2).div(3))) {
        Thread()
    }

    init {
        totalWidth = minimap.width
        totalHeight = minimap.height

        AppLoader.disposableSingletonsPool.add(this)
    }

    fun update() {
        // make the queueing work
        // enqueue first
        for (y in tilemap.indices) {
            for (x in tilemap[0].indices) {
                if (liveTilesMeta[tilemap[y][x]].revalidate) {
                    liveTilesMeta[tilemap[y][x]].revalidate = false
                    updaterQueue.addLast(createUpdater(x, y))
                    printdbg(this, "Queueing tilemap update ($x,$y); queue size now: ${updaterQueue.size}")
                }
            }
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


        // assign tiles to the tilemap
        // TODO

    }
    fun revalidateAll() {
        liveTilesMeta.forEach { it.revalidate = true }
    }

    private var rerender = true

    /**
     * When to call:
     * - every 5 seconds or so
     * - every .5 seconds for 10 seconds after the tilemap changed
     */
    fun requestRender() {
        printdbg(this, "Rerender requested")
        rerender = true
    }

    fun renderToBackground() {
        if (rerender) {
            for (y in 0 until TILES_IN_Y) {
                for (x in 0 until TILES_IN_X) {
                    minimap.drawPixmap(liveTiles[tilemap[y][x]], x * LIVETILE_SIZE, y * LIVETILE_SIZE)
                }
            }
            rerender = false
        }
    }

    private val HQRNG = net.torvald.random.HQRNG()

    private fun createUpdater(tileSlotIndexX: Int, tileSlotIndexY: Int) = Runnable {
        val pixmap = liveTiles[tilemap[tileSlotIndexY][tileSlotIndexX]]
        val topLeftX = topLeftCoordX + LIVETILE_SIZE * tileSlotIndexX
        val topLeftY = topLeftCoordY + LIVETILE_SIZE * tileSlotIndexY

        for (y in topLeftY until topLeftY + LIVETILE_SIZE) {
            for (x in if (tileSlotIndexY >= TILES_IN_X / 2) (topLeftX + LIVETILE_SIZE - 1) downTo topLeftX else topLeftX until topLeftX + LIVETILE_SIZE) {
                val tileTerr = world.getTileFromTerrain(x, y) ?: throw Error("OoB: $x, $y")
                val wallTerr = world.getTileFromWall(x, y) ?: Block.AIR
                val colTerr = CreateTileAtlas.terrainTileColourMap.get(tileTerr % 16, tileTerr / 16)
                val colWall = CreateTileAtlas.terrainTileColourMap.get(wallTerr % 16, wallTerr / 16).mul(BlocksDrawer.wallOverlayColour)

                val outCol = if (colTerr.a > 0.1f) colTerr else colWall

                pixmap.blending = Pixmap.Blending.None
                pixmap.setColor(outCol)
                //pixmap.setColor(Color.CORAL)
                pixmap.drawPixel(x - topLeftX, y - topLeftY)
            }
        }
    }

    override fun dispose() {
        liveTiles.forEach { it.dispose() }
        minimap.dispose()
        try {
            tempTex.dispose()
        }
        catch (e: GdxRuntimeException) {}
    }

    private data class LiveTileMeta(var revalidate: Boolean)
}