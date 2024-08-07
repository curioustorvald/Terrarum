package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.roundToInt

/**
 * World loading screen with minecraft 1.14-style preview
 *
 * Created by minjaesong on 2019-11-09.
 */
class WorldgenLoadScreen(screenToBeLoaded: IngameInstance, private val worldwidth: Int, private val worldheight: Int) : LoadScreenBase() {

    // a Class impl is chosen to make resize-handling easier, there's not much benefit making this a singleton anyway

    init {
        App.disposables.add(this)
    }

    override var screenToLoad: IngameInstance? = screenToBeLoaded
    private val world: GameWorld // must use Getter, as the field WILL BE redefined by the TerrarumIngame.enterCreateNewWorld() !
        get() = screenToLoad!!.world

    companion object {
        const val WIDTH_RATIO = 0.7
        const val PREVIEW_UPDATE_RATE = App.UPDATE_RATE

        val COL_TERR = Color.WHITE
        val COL_WALLED = Color(.5f, .5f, .5f, 1f)
        val COL_AIR = Color.BLACK
    }

    private val previewWidth = (Toolkit.drawWidth * WIDTH_RATIO).roundToInt()
    private val previewHeight = (Toolkit.drawWidth * WIDTH_RATIO * worldheight / worldwidth).roundToInt()

    private lateinit var previewPixmap: Pixmap
    private lateinit var previewTexture: Texture

    private var previewRenderCounter = 0f

    // NOTE: actual world init and terragen is called by TerrarumIngame.enterLoadFromSave()

    override fun show() {
        super.show()

        previewPixmap = Pixmap(previewWidth, previewHeight, Pixmap.Format.RGBA8888)
        previewTexture = Texture(1, 1, Pixmap.Format.RGBA8888)

        previewPixmap.setColor(Color.BLACK)
        previewPixmap.fill()

        IngameRenderer.setRenderedWorld(world)
    }

    override fun render(delta: Float) {
        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        val drawWidth = Toolkit.drawWidth

        previewRenderCounter += delta
        if (previewRenderCounter >= PREVIEW_UPDATE_RATE) {
            previewRenderCounter -= PREVIEW_UPDATE_RATE
            renderToPreview()
            previewTexture.dispose()
            previewTexture = Texture(previewPixmap)
        }


        App.batch.inUse { val it = it as FlippingSpriteBatch
            it.color = Color.WHITE
            val previewX = (drawWidth - previewWidth).div(2f).roundToFloat()
            val previewY = (App.scr.height - previewHeight.times(1.5f)).div(2f).roundToFloat()
            Toolkit.drawBoxBorder(it, previewX.toInt()-1, previewY.toInt()-1, previewWidth+2, previewHeight+2)
            it.drawFlipped(previewTexture, previewX, previewY)
            val text = messages.getHeadElem() ?: ""
            App.fontGame.draw(it,
                    text,
                    (drawWidth - App.fontGame.getWidth(text)).div(2f).roundToFloat(),
                    previewY + previewHeight + 98 - App.fontGame.lineHeight
            )
        }


        super.render(delta)
    }

    private fun renderToPreview() {
        for (y in 0 until previewHeight) {
            for (x in 0 until previewWidth) {
                val wx = (world.width.toFloat() / previewWidth * x).roundToInt()
                val wy = (world.height.toFloat() / previewHeight * y).roundToInt()

                try {
                    // q&d solution for the dangling pointer; i'm doing this only because it's this fucking load screen that's fucking the dead pointer
                    if (!world.layerTerrain.ptrDestroyed) {
                        val outCol = if (BlockCodex[world.getTileFromTerrain(wx, wy)].isSolid) COL_TERR
                        else if (BlockCodex[world.getTileFromWall(wx, wy)].isSolid) COL_WALLED
                        else COL_AIR

                        previewPixmap.setColor(outCol)
                        previewPixmap.drawPixel(x, previewHeight - 1 - y) // this flips Y
                    }
                }
                catch (e: NoSuchElementException) {}
            }
        }

    }


    override fun dispose() {
        if (!previewPixmap.isDisposed)
            previewPixmap.dispose()

        previewTexture.dispose()
    }
}