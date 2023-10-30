package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created by minjaesong on 2023-10-30.
 */
class FancyWorldGenLoadScreen(screenToBeLoaded: IngameInstance, private val worldwidth: Int, private val worldheight: Int, override var preLoadJob: (LoadScreenBase) -> Unit) : LoadScreenBase() {

    init {
        CommonResourcePool.addToLoadingList("basegame-gui-loadscrlayer01") {
            Texture(ModMgr.getGdxFile("basegame", "gui/loadscr_layer01.png"))
        }
        CommonResourcePool.addToLoadingList("basegame-gui-loadscrlayer02") {
            Texture(ModMgr.getGdxFile("basegame", "gui/loadscr_layer02.png"))
        }
        CommonResourcePool.loadAll()

        App.disposables.add(this)
    }


    override var screenToLoad: IngameInstance? = screenToBeLoaded
    private val world: GameWorld // must use Getter, as the field WILL BE redefined by the TerrarumIngame.enterCreateNewWorld() !
        get() = screenToLoad!!.world


    val ratio = worldwidth * sqrt(2.0 / (worldwidth.sqr() + worldheight.sqr())) // world size is always wider than tall
    val htilesCount = worldwidth / CHUNK_W
    val vtilesCount = worldheight / CHUNK_W

    val tileSize = ((540 * ratio) / htilesCount).roundToInt() // (visible tilesize + gapSize)
    val gapSize = if (tileSize >= 15) 3 else if (tileSize >= 7) 2 else 1
    val visibleTileSize = tileSize - gapSize
    val previewWidth = tileSize * htilesCount - gapSize
    val previewHeight = tileSize * vtilesCount

    val xoff = 0//(Math.random() * (1024 - 764)).toInt()

    val baseTileTex = arrayOf(
        CommonResourcePool.getAsTexture("basegame-gui-loadscrlayer01"),
        CommonResourcePool.getAsTexture("basegame-gui-loadscrlayer02"),
    )

    val drawWidth = Toolkit.drawWidth

    val imgYoff = (252 - previewHeight * 0.28f).toInt()

    val tiles = baseTileTex.map {
        TextureRegionPack(it, visibleTileSize, imgYoff + previewHeight, gapSize, 0, xoff, 0)
    }

    override fun render(delta: Float) {
        gdxClearAndEnableBlend(.094f, .094f, .094f, 0f)

        App.batch.inUse { val it = it as FlippingSpriteBatch
            it.color = Color.WHITE
            val previewX = (drawWidth - previewWidth).div(2f).roundToFloat()
            val previewY = (App.scr.height - previewHeight.times(1.5f)).div(2f).roundToFloat()
            Toolkit.drawBoxBorder(it, previewX.toInt()-1, previewY.toInt()-1, previewWidth+2, previewHeight+2)

            drawTiles(it, getStage(), getProgress(), previewX, previewY - imgYoff)

            val text = messages.getHeadElem() ?: ""
            App.fontGame.draw(it,
                text,
                (drawWidth - App.fontGame.getWidth(text)).div(2f).roundToFloat(),
                previewY + previewHeight + 98 - App.fontGame.lineHeight
            )
        }


        super.render(delta)
    }

    private fun getProgress(): Int {
        return 42
    }

    private fun getStage(): Int {
        return 2
    }

    private fun drawTiles(batch: FlippingSpriteBatch, layerCount: Int, tileCount: Int, x: Float, y: Float) {
        for (layer in 0 until layerCount) {
            for (i in 0 until if (layer == layerCount - 1) tileCount else htilesCount) {
                batch.draw(tiles[layer].get(i, 0), x + i * tileSize, y)
            }
        }
    }
}