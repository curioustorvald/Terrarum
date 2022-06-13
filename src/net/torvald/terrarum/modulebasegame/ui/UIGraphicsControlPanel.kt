package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELL_COL
import net.torvald.terrarum.ui.*
import net.torvald.unicode.TIMES

/**
 * Created by minjaesong on 2021-10-06.
 */
class UIGraphicsControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 400
    override var height = 400
    override var openCloseTime = 0f

    private val spinnerWidth = 136
    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2


    private val linegap = 16
    private val panelgap = 20

    private val options = arrayOf(
            arrayOf("fx_dither", { Lang["MENU_OPTIONS_DITHER"] }, "toggle"),
            arrayOf("fx_backgroundblur", { Lang["MENU_OPTIONS_BLUR"] }, "toggle"),
            arrayOf("fx_streamerslayout", { Lang["MENU_OPTION_STREAMERS_LAYOUT"] }, "toggle"),
            arrayOf("usevsync", { Lang["MENU_OPTIONS_VSYNC"]+"*" }, "toggle"),
            arrayOf("screenmagnifying", { Lang["MENU_OPTIONS_RESOLUTION"]+"*" }, "spinnerd,1.0,2.0,0.25"),
            arrayOf("maxparticles", { Lang["MENU_OPTIONS_PARTICLES"] }, "spinner,256,1024,256"),
    )

    private fun makeButton(args: String, x: Int, y: Int, optionName: String): UIItem {
        return if (args.startsWith("toggle")) {
            UIItemToggleButton(this, x - 75, y, App.getConfigBoolean(optionName))
        }
        else if (args.startsWith("spinner,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x - spinnerWidth, y, App.getConfigInt(optionName), arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), spinnerWidth, numberToTextFunction = { "${it.toLong()}" })
        }
        else if (args.startsWith("spinnerd,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x - spinnerWidth, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), arg[3].toDouble(), spinnerWidth, numberToTextFunction = { "${it}x" })
        }
        else throw IllegalArgumentException(args)
    }

    private val optionControllers = options.mapIndexed { index, strings ->
        makeButton(options[index][2] as String,
                drawX + width - panelgap,
                drawY + panelgap - 2 + index * (20 + linegap),
                options[index][0] as String
        )
        /*UIItemToggleButton(this,
                drawX + width - panelgap - 75,
                drawY + panelgap - 2 + index * (20 + linegap),
                App.getConfigBoolean(options[index][0])
        )*/
    }

    init {
        optionControllers.forEachIndexed { i, it ->
            if (it is UIItemToggleButton) {
                it.clickOnceListener = { _, _, _ ->
                    it.toggle()
                    App.setConfig(options[i][0] as String, it.getStatus())
                }
            }
            else if (it is UIItemSpinner) {
                it.selectionChangeListener = {
                    App.setConfig(options[i][0] as String, it)
                }
            }

            addUIitem(it)
        }
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, drawX, drawY, width, height)

        batch.color = CELL_COL
        Toolkit.fillArea(batch, drawX, drawY, width, height)

        batch.color = Color.WHITE
        options.forEachIndexed { index, strings ->
            App.fontGame.draw(batch, (strings[1] as () -> String).invoke(), drawX + panelgap.toFloat(), drawY + panelgap + index * (20f + linegap))
        }
        uiItems.forEach { it.render(batch, camera) }
        App.fontGame.draw(batch, "* ${Lang["MENU_LABEL_RESTART_REQUIRED"]}", drawX + panelgap.toFloat(), drawY + height - panelgap - App.fontGame.lineHeight)

        if (App.getConfigBoolean("fx_streamerslayout")) {
            val xstart = App.scr.width - App.scr.chatWidth

            batch.color = Color(0x00f8ff_40)
            Toolkit.fillArea(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)

            batch.color = Toolkit.Theme.COL_ACTIVE
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