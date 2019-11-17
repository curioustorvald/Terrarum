package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.util.CircularArray
import kotlin.math.roundToInt

/**
 * World loading screen with minecraft 1.14-style preview
 *
 * Created by minjaesong on 2019-11-09.
 */
class WorldgenLoadScreen(private val world: GameWorld, screenToBeLoaded: IngameInstance) : LoadScreenBase() {

    // a Class impl is chosen to make resize-handling easier, there's not much benefit making this a singleton anyway

    override var screenToLoad: IngameInstance? = screenToBeLoaded

    companion object {
        private const val WIDTH_RATIO = 0.6
    }

    private val previewWidth = (AppLoader.screenW * WIDTH_RATIO).roundToInt()
    private val previewHeight = (AppLoader.screenW * WIDTH_RATIO * world.height / world.width).roundToInt()

    private lateinit var previewPixmap: Pixmap
    private lateinit var previewTexture: Texture

    override fun show() {
        previewPixmap = Pixmap(previewWidth, previewHeight, Pixmap.Format.RGBA8888)
        previewTexture = Texture(1, 1, Pixmap.Format.RGBA8888)
    }

    override fun render(delta: Float) {
        previewTexture.dispose()
        previewTexture = Texture(previewPixmap)

        //
        AppLoader.batch.inUse {
            it.draw(previewTexture,
                    (AppLoader.screenW - previewWidth).div(2f).round(),
                    (AppLoader.screenH - previewHeight.times(1.25f)).div(2f).round()
            )
        }
    }

    override fun dispose() {
        previewPixmap.dispose()
        previewTexture.dispose()
    }
}