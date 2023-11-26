package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.TerrarumScreenSize
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

typealias ControlPanelOptions = Array<Array<Any>>

/**
 * Created by minjaesong on 2023-07-14.
 */
object ControlPanelCommon {

    init {
        CommonResourcePool.addToLoadingList("gui_hrule") {
            TextureRegionPack(Gdx.files.internal("assets/graphics/gui/hrule.tga"), 216, 20)
        }
        CommonResourcePool.loadAll()
    }

    var CONFIG_SPINNER_WIDTH = 140
    var CONFIG_TYPEIN_WIDTH = 240
    var CONFIG_SLIDER_WIDTH = 240
    var CONFIG_TEXTSEL_WIDTH = 240

    // @return Pair of <UIItem, Init job for the item>
    fun makeButton(parent: UICanvas, args: String, x: Int, y: Int, optionNames0: String): Pair<UIItem, (UIItem, String) -> Unit> {
        val optionNames = optionNames0.split(",")
        val optionName = optionNames.first()
        val arg = args.split(',')

        return if (args == "h1" || args == "p" || args == "emph" || args == "pp") {
            (object : UIItem(parent, x, y) {
                override val width = 1
                override val height = 1
                override fun dispose() {}
            }) to { _, _ -> }
        }
        else if (args.startsWith("toggle")) {
            UIItemToggleButton(parent, x, y, CONFIG_SPINNER_WIDTH, App.getConfigBoolean(optionName)) to { it: UIItem, optionStr: String ->
                (it as UIItemToggleButton).clickOnceListener = { _, _ ->
                    it.toggle()
                    App.setConfig(optionStr, it.getStatus())
                }
            }
        }
        else if (args.startsWith("textsel,")) {
            val labelFuns = arg.subList(1, arg.size).map { { Lang[it.substringAfter("=")] } }
            val optionsList = arg.subList(1, arg.size).map { it.substringBefore("=") }

            val initialSel = optionsList.indexOf(App.getConfigString(optionName))

//            println("labelFuns = ${labelFuns.map { it.invoke() }}")
//            println("optionsList = $optionsList")
//            println("optionName = $optionName; value = ${App.getConfigString(optionName)}")
//            println("initialSel = $initialSel")

            if (initialSel < 0) throw IllegalArgumentException("config value '${App.getConfigString(optionName)}' for option '$optionName' is not found on the options list")

            UIItemTextSelector(parent, x, y, labelFuns, initialSel, CONFIG_TEXTSEL_WIDTH, clickToShowPalette = false) to { it: UIItem, optionStr: String ->
                (it as UIItemTextSelector).selectionChangeListener = {
                    App.setConfig(optionStr, optionsList[it])
                }
            }
        }
        else if (args.startsWith("spinnersel,")) {
            val labelFuns = arg.subList(1, arg.size).map { { it } }
            val optionsList = arg.subList(1, arg.size).map { it.toInt() }

            val initialSel = optionsList.indexOf(App.getConfigInt(optionName))
            if (initialSel < 0) throw IllegalArgumentException("config value '${App.getConfigInt(optionName)}' for option '$optionName' is not found on the options list")

            UIItemTextSelector(parent, x, y, labelFuns, initialSel, CONFIG_SPINNER_WIDTH, clickToShowPalette = false, useSpinnerButtons = true) to { it: UIItem, optionStr: String ->
                (it as UIItemTextSelector).selectionChangeListener = {
                    App.setConfig(optionStr, optionsList[it])
                }
            }
        }
        else if (args.startsWith("spinner,")) {
            UIItemSpinner(parent, x, y, App.getConfigInt(optionName), arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), CONFIG_SPINNER_WIDTH, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerd,")) {
            UIItemSpinner(parent, x, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), arg[3].toDouble(), CONFIG_SPINNER_WIDTH, numberToTextFunction = { "${((it as Double)*100).toInt()}%" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("sliderd,")) {
            UIItemHorzSlider(parent, x, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), CONFIG_SLIDER_WIDTH) to { it: UIItem, optionStr: String ->
                (it as UIItemHorzSlider).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerimul,")) {
            val mult = arg[4].toInt()
            UIItemSpinner(parent, x, y, App.getConfigInt(optionName) / mult, arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), CONFIG_SPINNER_WIDTH, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it.toInt() * mult)
                }
            }
        }
        else if (args.startsWith("typeinint")) {
//            val arg = args.split(',') // args: none
            UIItemTextLineInput(parent, x, y, CONFIG_SPINNER_WIDTH,
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
            val keyWidth = optionNames[0]
            val keyHeight = optionNames[1]
            UIItemTextLineInput(parent, x, y, CONFIG_SPINNER_WIDTH,
                defaultValue = { "${App.getConfigInt(keyWidth)}x${App.getConfigInt(keyHeight)}" },
                maxLen = InputLenCap(9, InputLenCap.CharLenUnit.CODEPOINTS),
                keyFilter = { it.headkey == Input.Keys.ENTER || it.headkey == Input.Keys.BACKSPACE || it.character?.matches(Regex("[0-9xXpP]")) == true },
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
                    else if (text == "720p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 1280)
                        App.setConfig(keyHeight, 720)
                    }
                    else if (text == "800p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 1280)
                        App.setConfig(keyHeight, 800)
                    }
                    else if (text == "900p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 1600)
                        App.setConfig(keyHeight, 900)
                    }
                    else if (text == "1080p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 1920)
                        App.setConfig(keyHeight, 1080)
                    }
                    else if (text == "1200p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 1920)
                        App.setConfig(keyHeight, 1200)
                    }
                    else if (text == "1440p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 2560)
                        App.setConfig(keyHeight, 1440)
                    }
                    else if (text == "1600p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 2560)
                        App.setConfig(keyHeight, 1600)
                    }
                    else if (text == "2160p") {
                        it.markAsNormal()
                        App.setConfig(keyWidth, 3840)
                        App.setConfig(keyHeight, 2160)
                    }
                    else {
                        it.markAsInvalid()
                        App.setConfig(keyWidth, TerrarumScreenSize.defaultW)
                        App.setConfig(keyHeight, TerrarumScreenSize.defaultH)
                    }
                }
            }
        }
        else if (args.startsWith("typein")) {
            //args: none
            UIItemTextLineInput(parent, x, y, CONFIG_TYPEIN_WIDTH, defaultValue = { App.getConfigString(optionName) }) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else throw IllegalArgumentException(args)
    }

    private fun String.countLines() = this.count { it == '\n' } + 1

    private val linegap = 14
    private val panelgap = 20

    private val textLineHeight = App.fontGame.lineHeight.toInt()
    private val rowheightDiff = textLineHeight - panelgap

    private val h1MarginTop = 16
    private val h1MarginBottom = 4

    private val optionsYposCache = HashMap<String, IntArray>()
    private val optionsCache = HashMap<String, ControlPanelOptions>()

    fun register(ui: UICanvas, width: Int, identifier: String, options: ControlPanelOptions) {
        optionsCache[identifier] = options
        val optionsYpos = IntArray(options.size + 1)
        var akku = 0
        options.forEachIndexed { index, row ->
            val option = row[2]

            if (index > 0 && option == "h1") {
                akku += h1MarginTop
            }

            optionsYpos[index] = akku

            val realRowHeight = (row[1] as () -> String).invoke().countLines() * textLineHeight - rowheightDiff

            akku += when (option) {
                "h1" -> realRowHeight + linegap + h1MarginBottom
                "pp" -> realRowHeight - 7
                else -> realRowHeight + linegap
            }
        }
        optionsYpos[optionsYpos.lastIndex] = akku
        optionsYposCache[identifier] = optionsYpos

        val height = optionsYpos.last()
        val drawX = (App.scr.width - width) / 2
        val drawY = (App.scr.height - height) / 2

        options.forEachIndexed { index, args ->
            val (item, job) = makeButton(
                ui, args[2] as String,
                drawX + width / 2 + panelgap,
                drawY - 2 + optionsYpos[index],
                args[0] as String
            )
            job.invoke(item, args[0] as String)
            ui.addUIitem(item)
        }
    }

    private val hrule = CommonResourcePool.getAsTextureRegionPack("gui_hrule")

    fun getMenuHeight(identifier: String) = optionsYposCache[identifier]!!.last()

    fun render(identifier: String, width: Int, batch: SpriteBatch) {
        val height = optionsYposCache[identifier]!!.last()
        val drawX = (App.scr.width - width) / 2
        val drawY = (App.scr.height - height) / 2

        val optionsYpos = optionsYposCache[identifier]!!
        optionsCache[identifier]!!.forEachIndexed { index, args ->
            val mode = args[2]

            val font = if (mode == "h1") App.fontUITitle else App.fontGame

            val label = (args[1] as () -> String).invoke().lines()
            val labelWidth = label.maxOf { font.getWidth(it) }
            batch.color = when (mode) {
                "h1" -> Toolkit.Theme.COL_MOUSE_UP
                "p" -> Color.LIGHT_GRAY
                "emph" -> Toolkit.Theme.COL_RED
                else -> Color.WHITE
            }

            val xpos = if (mode == "p" || mode == "h1" || mode == "emph")
                drawX + (width - labelWidth)/2 // centre-aligned
            else
                drawX + width/2 - panelgap - labelWidth // right aligned at the middle of the panel, offset by panelgap

            label.forEachIndexed { rows, s ->
                font.draw(batch, s, xpos.toFloat(), drawY + optionsYpos[index] - 2f + textLineHeight * rows)
            }

            // draw hrule
            if (mode == "h1") {
                val ruleWidth = ((width - 24 - labelWidth) / 2).toFloat()
                batch.draw(hrule.get(0,0), xpos - 24f - ruleWidth, drawY + optionsYpos[index].toFloat(), ruleWidth, hrule.tileH.toFloat())
                batch.draw(hrule.get(0,1), xpos + 24f + labelWidth, drawY + optionsYpos[index].toFloat(), ruleWidth, hrule.tileH.toFloat())
            }
        }
    }

}