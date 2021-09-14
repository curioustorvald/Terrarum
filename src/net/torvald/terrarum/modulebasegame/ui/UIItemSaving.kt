package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.WriteSavegame
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2021-09-14.
 */
class UIItemSaving(parentUI: UICanvas, initialX: Int, initialY: Int) : UIItem(parentUI, initialX, initialY) {

    companion object {
        const val WIDTH = 320
        const val HEIGHT = 100
    }

    override val width = WIDTH
    override val height = HEIGHT

    private val circleSheet = CommonResourcePool.getAsTextureRegionPack("loading_circle_64")
    private val circles = circleSheet.horizontalCount * circleSheet.verticalCount

    init {
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        val t = Lang["MENU_IO_SAVING"]
        val tlen = App.fontGame.getWidth(t)
        App.fontGame.draw(batch, t, (posX + (width - tlen) / 2).toFloat(), posY.toFloat())

        // -1..63
        val index = ((WriteSavegame.saveProgress / WriteSavegame.saveProgressMax) * circles).roundToInt() - 1
        if (index >= 0) {
            val sx = index % circleSheet.horizontalCount
            val sy = index / circleSheet.horizontalCount
            batch.draw(circleSheet.get(sx, sy), (posX + (width - circleSheet.tileW) / 2).toFloat(), posY + height.toFloat(), circleSheet.tileW.toFloat(), circleSheet.tileH * -1f)
        }
    }

    override fun dispose() {}
}