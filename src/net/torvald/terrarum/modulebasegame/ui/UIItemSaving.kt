package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.serialise.WriteSavegame
import net.torvald.terrarum.ui.Toolkit
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
        // these things will not scroll along with the parent GUI!
        val t = Lang["MENU_IO_SAVING"]
        val tlen = App.fontGame.getWidth(t)
        App.fontGame.draw(batch, t, (posX + (width - tlen) / 2).toFloat(), ((App.scr.height - circleSheet.tileH) / 2) - 40f)

        // -1..63
        val index = ((WriteSavegame.saveProgress / WriteSavegame.saveProgressMax) * circles).roundToInt() - 1
        if (index >= 0) {
            val sx = index % circleSheet.horizontalCount
            val sy = index / circleSheet.horizontalCount
            // q&d fix for ArrayIndexOutOfBoundsException caused when saving huge world... wut?
            if (sx in 0 until circleSheet.horizontalCount && sy in 0 until circleSheet.horizontalCount) {
                batch.draw(
                    circleSheet.get(sx, sy),
                    ((Toolkit.drawWidth - circleSheet.tileW) / 2).toFloat(),
                    ((App.scr.height - circleSheet.tileH) / 2).toFloat()
                )
            }
        }
    }

    override fun dispose() {}
}