package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.TIMES
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_BLACK
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemToggleButton

/**
 * Created by minjaesong on 2021-10-06.
 */
class GraphicsControlPanel : UICanvas() {

    override var width = 400
    override var height = 400
    override var openCloseTime = 0f

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2


    private val linegap = 16
    private val panelgap = 20

    private val options = arrayOf(
            arrayOf("fx_dither", "Dither"),
            arrayOf("fx_backgroundblur", "Blur"),
            arrayOf("fx_streamerslayout", Lang["MENU_OPTION_STREAMERS_LAYOUT"]),
            arrayOf("usevsync", Lang["MENU_OPTIONS_VSYNC"]+"*")
    )

    private val togglers = options.mapIndexed { index, strings ->
        UIItemToggleButton(this,
                drawX + width - panelgap - 75,
                drawY + panelgap - 2 + index * (20 + linegap),
                App.getConfigBoolean(options[index][0])
        )
    }

    init {
        togglers.forEachIndexed { i, it ->
            it.clickOnceListener = { _,_,_ ->
                it.toggle()
                App.setConfig(options[i][0], it.getStatus())
            }

            addUIitem(it)
        }
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        Toolkit.drawBoxBorder(batch, drawX, drawY, width, height)

        batch.color = CELLCOLOUR_BLACK
        Toolkit.fillArea(batch, drawX, drawY, width, height)

        batch.color = Color.WHITE
        options.forEachIndexed { index, strings ->
            App.fontGame.draw(batch, strings[1], drawX + panelgap.toFloat(), drawY + panelgap + index * (20f + linegap))
        }
        uiItems.forEach { it.render(batch, camera) }
        App.fontGame.draw(batch, "* ${Lang["MENU_LABEL_RESTART_REQUIRED"]}", drawX + panelgap.toFloat(), drawY + height - panelgap - App.fontGame.lineHeight)

        if (App.getConfigBoolean("fx_streamerslayout")) {
            val xstart = App.scr.width - App.scr.chatWidth

            batch.color = Color(0x00f8ff_40)
            Toolkit.fillArea(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)

            batch.color = UIItemTextButton.defaultActiveCol
            Toolkit.drawBoxBorder(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)
            val overlayResTxt = "${App.scr.chatWidth}$TIMES${App.scr.height}"
            App.fontGame.draw(batch, overlayResTxt,
                    (xstart + (App.scr.chatWidth - App.fontGame.getWidth(overlayResTxt)) / 2).toFloat(),
                    ((App.scr.height - App.fontGame.lineHeight) / 2).toFloat()
            )
        }
    }

    override fun show() {
        super.show()
    }

    override fun hide() {
        super.hide()
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}