package net.torvald.terrarum.blockstats

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.worlddrawer.toRGBA

object MinimapComposer {

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
    val minimap = Pixmap(Gdx.files.internal("./assets/testimage.png"))
    // total size of the minimap. Remember: textures can be mosaic-ed to display full map.
    var totalWidth = 0
    var totalHeight = 0

    /** World coord for top-left side of the tileslot. ALWAYS multiple of LIVETILE_SIZE */
    var topLeftCoordX = 0
    /** World coord for top-left side of the tileslot. ALWAYS multiple of LIVETILE_SIZE */
    var topLeftCoordY = 0

    const val LIVETILE_SIZE = 64
    const val DISPLAY_CANVAS_WIDTH = 2048
    const val DISPLAY_CANVAS_HEIGHT = 1024
    const val TILES_IN_X = DISPLAY_CANVAS_WIDTH / LIVETILE_SIZE
    const val TILES_IN_Y = DISPLAY_CANVAS_HEIGHT / LIVETILE_SIZE
    private val displayPixmap = Pixmap(DISPLAY_CANVAS_WIDTH, DISPLAY_CANVAS_HEIGHT, Pixmap.Format.RGBA8888)
    // numbers inside of it will change a lot
    private val tileSlot = Array(TILES_IN_Y) { IntArray(TILES_IN_X) }
    // pixmaps inside of this will never be redefined
    private val liveTiles = Array(TILES_IN_X * TILES_IN_Y) { Pixmap(LIVETILE_SIZE, LIVETILE_SIZE, Pixmap.Format.RGBA8888) }


    init {
        totalWidth = minimap.width
        totalHeight = minimap.height
    }

    fun update() {

    }

    private fun createUpdater(tileSlotIndexX: Int, tileSlotIndexY: Int) = Runnable {
        val pixmap = liveTiles[tileSlot[tileSlotIndexY][tileSlotIndexX]]
        val topLeftX = topLeftCoordX + LIVETILE_SIZE * tileSlotIndexX
        val topLeftY = topLeftCoordY + LIVETILE_SIZE * tileSlotIndexY

        for (y in topLeftY until topLeftY + LIVETILE_SIZE) {
            for (x in if (tileSlotIndexY >= TILES_IN_X / 2) (topLeftX + LIVETILE_SIZE - 1) downTo topLeftX else topLeftX until topLeftX + LIVETILE_SIZE) {
                val color = Color.WHITE // TODO
                pixmap.drawPixel(x - topLeftX, y - topLeftY, color.toRGBA())
            }
        }
    }

    fun dispose() {
        liveTiles.forEach { it.dispose() }
    }

}