package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.util.CircularArray
import kotlin.math.roundToInt

/**
 * World loading screen with minecraft 1.14-style preview
 *
 * Created by minjaesong on 2019-11-09.
 */
class WorldgenLoadScreen(screenToBeLoaded: IngameInstance, worldwidth: Int, worldheight: Int) : LoadScreenBase() {

    // a Class impl is chosen to make resize-handling easier, there's not much benefit making this a singleton anyway

    init {
        screenToBeLoaded.world
    }

    private val world = screenToBeLoaded.world
    override var screenToLoad: IngameInstance? = screenToBeLoaded

    companion object {
        private const val WIDTH_RATIO = 0.7
        private const val PREVIEW_UPDATE_RATE = 1/8f

        private val COL_WALL = Color.WHITE
        private val COL_TERR = Color(.5f,.5f,.5f,1f)
        private val COL_AIR = Color.BLACK
    }

    private val previewWidth = (AppLoader.screenW * WIDTH_RATIO).roundToInt()
    private val previewHeight = (AppLoader.screenW * WIDTH_RATIO * worldheight / worldwidth).roundToInt()

    private lateinit var previewPixmap: Pixmap
    private lateinit var previewTexture: Texture

    private var previewRenderCounter = 0f

    override fun show() {
        super.show()

        previewPixmap = Pixmap(previewWidth, previewHeight, Pixmap.Format.RGBA8888)
        previewTexture = Texture(1, 1, Pixmap.Format.RGBA8888)

        previewPixmap.setColor(Color.BLACK)
        previewPixmap.fill()
    }

    override fun render(delta: Float) {
        previewTexture.dispose()
        previewTexture = Texture(previewPixmap)

        //

        previewRenderCounter += delta
        if (previewRenderCounter >= PREVIEW_UPDATE_RATE) {
            previewRenderCounter -= PREVIEW_UPDATE_RATE
            renderToPreview()
        }

        AppLoader.batch.inUse {
            it.draw(previewTexture,
                    (AppLoader.screenW - previewWidth).div(2f).round(),
                    (AppLoader.screenH - previewHeight.times(1.5f)).div(2f).round()
            )
        }

        super.render(delta)
    }

    private fun renderToPreview() {
        for (y in 0 until previewWidth) {
            for (x in 0 until previewHeight) {
                val wx = (world.width / previewWidth * x).toInt()
                val wy = (world.height / previewHeight * y).toInt()
                val colT = if (world.getTileFromTerrain(wx, wy) != 0) COL_WALL else COL_TERR
                val colW = if (world.getTileFromWall(wx, wy) != 0) COL_WALL else COL_AIR
                val outCol = colW mul colT

                previewPixmap.setColor(outCol)
                previewPixmap.drawPixel(x, y)
            }
        }
    }

    override fun addMessage(msg: String) {
        super.addMessage(msg)
        println("[WorldgenLoadScreen] $msg")
    }

    override fun dispose() {
        previewPixmap.dispose()
        previewTexture.dispose()
    }
}