package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ceilInt
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

    private val spinnerWidth = 140
    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2


    private val linegap = 16
    private val panelgap = 20

    private val options = arrayOf(
        arrayOf("fx_dither", { Lang["MENU_OPTIONS_DITHER"] }, "toggle"),
        arrayOf("fx_backgroundblur", { Lang["MENU_OPTIONS_BLUR"] }, "toggle"),
        arrayOf("fx_streamerslayout", { Lang["MENU_OPTION_STREAMERS_LAYOUT"] }, "toggle"),
        arrayOf("maxparticles", { Lang["MENU_OPTIONS_PARTICLES"] }, "spinner,256,1024,256"),
        arrayOf("displayfps", { Lang["MENU_LABEL_FRAMESPERSEC"]+"*" }, "spinner,0,300,2"),
        arrayOf("usevsync", { Lang["MENU_OPTIONS_VSYNC"]+"*" }, "toggle"),
        arrayOf("screenwidth,screenheight", { Lang["MENU_OPTIONS_RESOLUTION"]+"*" }, "typeinres"),
        arrayOf("screenmagnifying", { Lang["GAME_ACTION_ZOOM"]+"*" }, "spinnerd,1.0,2.0,0.05"),
    )

    // @return Pair of <UIItem, Init job for the item>
    private fun makeButton(args: String, x: Int, y: Int, optionName: String): Pair<UIItem, (UIItem, String) -> Unit> {
        return if (args.startsWith("toggle")) {
            UIItemToggleButton(this, x - 75, y, App.getConfigBoolean(optionName)) to { it: UIItem, optionStr: String ->
                (it as UIItemToggleButton).clickOnceListener = { _, _, _ ->
                    it.toggle()
                    App.setConfig(optionStr, it.getStatus())
                }
            }
        }
        else if (args.startsWith("spinner,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x - spinnerWidth, y, App.getConfigInt(optionName), arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), spinnerWidth, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerd,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x - spinnerWidth, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), arg[3].toDouble(), spinnerWidth, numberToTextFunction = { "${((it as Double)*100).toInt()}%" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }            }
        }
        else if (args.startsWith("typeinint")) {
//            val arg = args.split(',') // args: none
            UIItemTextLineInput(this, x - spinnerWidth, y, spinnerWidth, { "${App.getConfigInt(optionName)}" }, InputLenCap(4, InputLenCap.CharLenUnit.CODEPOINTS), { it.headkey in Input.Keys.NUM_0..Input.Keys.NUM_9 || it.headkey == Input.Keys.BACKSPACE }) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = {
                    App.setConfig(optionStr, it.toInt()) // HAXXX!!!
                }
            }
        }
        else if (args.startsWith("typeinres")) {
            val keyWidth = optionName.substringBefore(',')
            val keyHeight = optionName.substringAfter(',')
            UIItemTextLineInput(this, x - spinnerWidth, y, spinnerWidth, { "${App.getConfigInt(keyWidth)}x${App.getConfigInt(keyHeight)}" }, InputLenCap(9, InputLenCap.CharLenUnit.CODEPOINTS), { it.headkey == Input.Keys.ENTER || it.headkey == Input.Keys.BACKSPACE || it.character?.matches(Regex("[0-9xX]")) == true }, UIItemTextButton.Companion.Alignment.CENTRE) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = { text ->
                    val text = text.lowercase()
                    if (text.matches(Regex("""[0-9]+x[0-9]+"""))) {
                        it.markAsNormal()
                        val width = text.substringBefore('x').toInt()
                        val height = text.substringAfter('x').toInt()
                        App.setConfig(keyWidth, width)
                        App.setConfig(keyHeight, height)
                    }
                    else it.markAsInvalid()
                }
            }
        }
        else throw IllegalArgumentException(args)
    }

    private val optionControllers: List<Pair<UIItem, (UIItem, String) -> Unit>> = options.mapIndexed { index, strings ->
        makeButton(options[index][2] as String,
                drawX + width - panelgap,
                drawY + panelgap - 2 + index * (20 + linegap),
                options[index][0] as String
        )
    }

    init {
        optionControllers.forEachIndexed { i, it ->
            it.second.invoke(it.first, options[i][0] as String)
            addUIitem(it.first)
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

            batch.color = Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)

            val overlayResTxt = "${(App.scr.chatWidth * App.scr.magn).ceilInt()}$TIMES${App.scr.windowH}"

            App.fontGame.draw(batch, overlayResTxt,
                    (xstart + (App.scr.chatWidth - App.fontGame.getWidth(overlayResTxt)) / 2).toFloat(),
                    ((App.scr.height - App.fontGame.lineHeight) / 2).toFloat()
            )
        }
    }

    override fun dispose() {
    }
}