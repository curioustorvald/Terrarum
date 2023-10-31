package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created by minjaesong on 2023-10-30.
 */
open class FancyWorldReadLoadScreen(screenToBeLoaded: IngameInstance, private val worldwidth: Int, private val worldheight: Int, override var preLoadJob: (LoadScreenBase) -> Unit) : LoadScreenBase() {

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

    val ratio = worldwidth * sqrt(2.0 / (worldwidth.sqr() + worldheight.sqr())) // world size is always wider than tall
    val htilesCount = worldwidth / CHUNK_W
    val vtilesCount = worldheight / CHUNK_H

    val unitSize = ((540 * ratio) / htilesCount).roundToInt() // (visible tilesize + gapSize)
    val previewWidth = unitSize * htilesCount
    val previewHeight = unitSize * vtilesCount

    val xoff = (Math.random() * (1024-764)/2).toInt()

    val baseTileTex = arrayOf(
        CommonResourcePool.getAsTexture("basegame-gui-loadscrlayer01"),
        CommonResourcePool.getAsTexture("basegame-gui-loadscrlayer02"),
    )

    val drawWidth = Toolkit.drawWidth

    val imgYoff = (252 - previewHeight * 0.28f).toInt()

    val tiles = baseTileTex.map {
        TextureRegionPack(it, 1, imgYoff + previewHeight, 0, 0, xoff, 0)
    }

    override fun render(delta: Float) {
        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        App.batch.inUse { val it = it as FlippingSpriteBatch
            val previewX = (drawWidth - previewWidth).div(2f).roundToFloat()
            val previewY = (App.scr.height - previewHeight.times(1.5f)).div(2f).roundToFloat()

            // it sets the colour by itself
            drawTiles(it, getStage(), getProgress(), previewX, previewY - imgYoff)


            it.color = Color.WHITE
            Toolkit.drawBoxBorder(it, previewX.toInt()-1, previewY.toInt()-1, previewWidth+2, previewHeight+2)
            val text = messages.getHeadElem() ?: ""
            App.fontGame.draw(it,
                text,
                (drawWidth - App.fontGame.getWidth(text)).div(2f).roundToFloat(),
                previewY + previewHeight + 98 - App.fontGame.lineHeight
            )
        }


        super.render(delta)
    }

    private val totalChunkCount = (worldwidth / CHUNK_W) * (worldheight / CHUNK_H)
    protected open fun getProgress(): Double {
        return progress.get().toDouble() / totalChunkCount * previewWidth
    }

    protected open fun getStage(): Int {
        return 2 // fixed value for Read screen
    }

    protected val batchColour = Color(-1) // create new Color instance just for the progress bar

    protected open fun drawTiles(batch: FlippingSpriteBatch, layerCount: Int, tileCount: Double, x: Float, y: Float) {
        batch.color = batchColour
        for (layer in 0 until layerCount) {
            for (i in 0 until tileCount.ceilToInt()) {
                batch.color.a = (tileCount - i).toFloat()
                batch.draw(tiles[layer].get(i, 0), x + i, y)
            }
        }
    }
}

class FancyWorldgenLoadScreen(screenToBeLoaded: IngameInstance, private val worldwidth: Int, private val worldheight: Int) : FancyWorldReadLoadScreen(screenToBeLoaded, worldwidth, worldheight, {}) {

    override fun getProgress(): Double {
        return progress.get().toDouble() / worldwidth * previewWidth
    }

    override fun getStage(): Int {
        return stageValue
    }

    override fun drawTiles(batch: FlippingSpriteBatch, layerCount: Int, tileCount: Double, x: Float, y: Float) {
        batch.color = batchColour
        for (layer in 0 until layerCount) {
            val isOldLayer = (layer != layerCount - 1)
            for (i in 0 until if (!isOldLayer) tileCount.ceilToInt() else previewWidth) {
                batch.color.a = if (!isOldLayer) (tileCount - i).toFloat() else 1f
                batch.draw(tiles[layer].get(i, 0), x + i, y)
            }
        }
    }
}
