package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ceilInt
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.TIMES

/**
 * Created by minjaesong on 2023-06-22.
 */
class UIPerformanceControlPanel(remoCon: UIRemoCon?) : UICanvas() {


    private val linegap = 14
    private val panelgap = 20

    private val rowheight = 20 + linegap

    private val h1MarginTop = 16
    private val h1MarginBottom = 4

    private val options = arrayOf(
        arrayOf("", { Lang["MENU_OPTIONS_GAMEPLAY"] }, "h1"),
            arrayOf("autosaveinterval", { Lang["MENU_OPTIONS_AUTOSAVE"] + " (${Lang["CONTEXT_TIME_MINUTE_PLURAL"]})" }, "spinnerimul,1,120,1,60000"),
            arrayOf("notificationshowuptime", { Lang["MENU_OPTIONS_NOTIFICATION_DISPLAY_DURATION"] + " (${Lang["CONTEXT_TIME_SECOND_PLURAL"]})" }, "spinnerimul,2,10,1,1000"),
        arrayOf("", { Lang["MENU_LABEL_JVM_DNT"] }, "h1"),
            arrayOf("jvm_xmx", { Lang["MENU_OPTIONS_JVM_HEAP_MAX"] + " (GB)" }, "spinner,2,32,1"),
            arrayOf("jvm_extra_cmd", { Lang["MENU_LABEL_EXTRA_JVM_ARGUMENTS"] }, "typein"),
            arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
    )

    private val optionsYpos = IntArray(options.size + 1)

    init {
        CommonResourcePool.addToLoadingList("gui_hrule") {
            TextureRegionPack(Gdx.files.internal("assets/graphics/gui/hrule.tga"), 216, 20)
        }
        CommonResourcePool.loadAll()



        var akku = 0
        options.forEachIndexed { index, row ->
            val option = row[2]

            if (index > 0 && option == "h1") {
                akku += h1MarginTop
            }

            optionsYpos[index] = akku

            akku += when (option) {
                "h1" -> rowheight + h1MarginBottom
                else -> rowheight
            }
        }
        optionsYpos[optionsYpos.lastIndex] = akku
    }
    override var width = 560
    override var height = optionsYpos.last()

    private val hrule = CommonResourcePool.getAsTextureRegionPack("gui_hrule")

    private val spinnerWidth = 140
    private val typeinWidth = 240
    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    // @return Pair of <UIItem, Init job for the item>
    private fun makeButton(args: String, x: Int, y: Int, optionName: String): Pair<UIItem, (UIItem, String) -> Unit> {
        return if (args.startsWith("h1") || args.startsWith("p")) {
            (object : UIItem(this, x, y) {
                override val width = 1
                override val height = 1
                override fun dispose() {}
            }) to { _, _ -> }
        }
        else if (args.startsWith("toggle")) {
            UIItemToggleButton(this, x, y, spinnerWidth, App.getConfigBoolean(optionName)) to { it: UIItem, optionStr: String ->
                (it as UIItemToggleButton).clickOnceListener = { _, _ ->
                    it.toggle()
                    App.setConfig(optionStr, it.getStatus())
                }
            }
        }
        else if (args.startsWith("spinner,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x, y, App.getConfigInt(optionName), arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), spinnerWidth, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerd,")) {
            val arg = args.split(',')
            UIItemSpinner(this, x, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), arg[3].toDouble(), spinnerWidth, numberToTextFunction = { "${((it as Double)*100).toInt()}%" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerimul,")) {
            val arg = args.split(',')
            val mult = arg[4].toInt()
            UIItemSpinner(this, x, y, App.getConfigInt(optionName) / mult, arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), spinnerWidth, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it.toInt() * mult)
                }
            }
        }
        else if (args.startsWith("typeinint")) {
//            val arg = args.split(',') // args: none
            UIItemTextLineInput(this, x, y, spinnerWidth,
                    defaultValue = { "${App.getConfigInt(optionName)}" },
                    maxLen = InputLenCap(4, InputLenCap.CharLenUnit.CODEPOINTS),
                    keyFilter = { it.headkey in Input.Keys.NUM_0..Input.Keys.NUM_9 || it.headkey == Input.Keys.BACKSPACE }
            ) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = {
                    App.setConfig(optionStr, it.toInt()) // HAXXX!!!
                }
            }
        }
        else if (args.startsWith("typeinres")) {
            val keyWidth = optionName.substringBefore(',')
            val keyHeight = optionName.substringAfter(',')
            UIItemTextLineInput(this, x, y, spinnerWidth,
                    defaultValue = { "${App.getConfigInt(keyWidth)}x${App.getConfigInt(keyHeight)}" },
                    maxLen = InputLenCap(9, InputLenCap.CharLenUnit.CODEPOINTS),
                    keyFilter = { it.headkey == Input.Keys.ENTER || it.headkey == Input.Keys.BACKSPACE || it.character?.matches(Regex("[0-9xX]")) == true },
                    alignment = UIItemTextButton.Companion.Alignment.CENTRE
            ) to { it: UIItem, optionStr: String ->
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
        else if (args.startsWith("typein")) {
            //args: none
            UIItemTextLineInput(this, x, y, typeinWidth, defaultValue = { App.getConfigString(optionName) }) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else throw IllegalArgumentException(args)
    }

    private val optionControllers: List<Pair<UIItem, (UIItem, String) -> Unit>> = options.mapIndexed { index, strings ->
        makeButton(options[index][2] as String,
            drawX + width / 2 + panelgap,
            drawY - 2 + optionsYpos[index],
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
        /*batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, drawX, drawY, width, height)

        batch.color = CELL_COL
        Toolkit.fillArea(batch, drawX, drawY, width, height)*/

        options.forEachIndexed { index, strings ->
            val mode = strings[2]

            val font = if (mode == "h1") App.fontUITitle else App.fontGame

            val label = (strings[1] as () -> String).invoke()
            val labelWidth = font.getWidth(label)
            batch.color = when (mode) {
                "h1" -> Toolkit.Theme.COL_MOUSE_UP
                "p" -> Color.LIGHT_GRAY
                else -> Color.WHITE
            }

            val xpos = if (mode == "p" || mode == "h1")
                drawX + (width - labelWidth)/2 // centre-aligned
            else
                drawX + width/2 - panelgap - labelWidth // right aligned at the middle of the panel, offsetted by panelgap

            font.draw(batch, label, xpos.toFloat(), drawY + optionsYpos[index] - 2f)

            // draw hrule
            if (mode == "h1") {
                val ruleWidth = ((width - 24 - labelWidth) / 2).toFloat()
                batch.draw(hrule.get(0,0), xpos - 24f - ruleWidth, drawY + optionsYpos[index].toFloat(), ruleWidth, hrule.tileH.toFloat())
                batch.draw(hrule.get(0,1), xpos + 24f + labelWidth, drawY + optionsYpos[index].toFloat(), ruleWidth, hrule.tileH.toFloat())
            }
        }
        uiItems.forEach { it.render(batch, camera) }

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