package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2021-09-14.
 */
class ChunkLoadingLoadScreen(screenToBeLoaded: IngameInstance, private val worldwidth: Int, private val worldheight: Int, override var preLoadJob: (LoadScreenBase) -> Unit) : LoadScreenBase() {

    override var screenToLoad: IngameInstance? = screenToBeLoaded
    private val world: GameWorld // must use Getter, as the field WILL BE redefined by the TerrarumIngame.enterCreateNewWorld() !
        get() = screenToLoad!!.world

    private var previewWidth = (App.scr.width * WorldgenLoadScreen.WIDTH_RATIO).roundToInt()
    private var previewHeight = (App.scr.width * WorldgenLoadScreen.WIDTH_RATIO * worldheight / worldwidth).roundToInt()

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
    }

    override fun render(delta: Float) {
        gdxClearAndSetBlend(.094f, .094f, .094f, 0f)


        if (worldwidth != -1 && worldheight != -1) {

            previewRenderCounter += delta
            if (previewRenderCounter >= WorldgenLoadScreen.PREVIEW_UPDATE_RATE) {
                previewRenderCounter -= WorldgenLoadScreen.PREVIEW_UPDATE_RATE
                renderToPreview()
                previewTexture.dispose()
                previewTexture = Texture(previewPixmap)
            }


            App.batch.inUse {
                it.color = Color.WHITE
                val previewX = (App.scr.width - previewWidth).div(2f).round()
                val previewY = (App.scr.height - previewHeight.times(1.5f)).div(2f).round()
                Toolkit.drawBoxBorder(it, previewX.toInt() - 1, previewY.toInt() - 1, previewWidth + 2, previewHeight + 2)
                it.draw(previewTexture,
                        previewX,
                        previewY
                )
                val text = messages.getHeadElem() ?: ""
                App.fontGame.draw(it,
                        text,
                        (App.scr.width - App.fontGame.getWidth(text)).div(2f).round(),
                        previewY + previewHeight + 98 - App.fontGame.lineHeight
                )
            }

        }

        super.render(delta)
    }

    private fun renderToPreview() {
        for (y in 0 until previewHeight) {
            for (x in 0 until previewWidth) {
                val wx = (world.width.toFloat() / previewWidth * x).roundToInt()
                val wy = (world.height.toFloat() / previewHeight * y).roundToInt()

                val outCol = if (BlockCodex[world.getTileFromTerrain(wx, wy)].isSolid) WorldgenLoadScreen.COL_TERR
                else if (BlockCodex[world.getTileFromWall(wx, wy)].isSolid) WorldgenLoadScreen.COL_WALLED
                else WorldgenLoadScreen.COL_AIR

                previewPixmap.setColor(outCol)
                previewPixmap.drawPixel(x, previewHeight - 1 - y) // this flips Y
            }
        }

    }


    override fun dispose() {
        if (!previewPixmap.isDisposed)
            previewPixmap.dispose()

        previewTexture.dispose()
    }
}