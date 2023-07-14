package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import net.torvald.terrarum.App
import net.torvald.terrarum.ui.*

/**
 * Created by minjaesong on 2023-07-14.
 */
object ControlPanelCommon {

    var CONFIG_SPINNER_WIDTH = 140
    var CONFIG_TYPEIN_WIDTH = 240

    // @return Pair of <UIItem, Init job for the item>
    fun makeButton(parent: UICanvas, args: String, x: Int, y: Int, optionName: String): Pair<UIItem, (UIItem, String) -> Unit> {
        return if (args.startsWith("h1") || args.startsWith("p")) {
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
        else if (args.startsWith("spinner,")) {
            val arg = args.split(',')
            UIItemSpinner(parent, x, y, App.getConfigInt(optionName), arg[1].toInt(), arg[2].toInt(), arg[3].toInt(), CONFIG_SPINNER_WIDTH, numberToTextFunction = { "${it.toLong()}" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerd,")) {
            val arg = args.split(',')
            UIItemSpinner(parent, x, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), arg[3].toDouble(), CONFIG_SPINNER_WIDTH, numberToTextFunction = { "${((it as Double)*100).toInt()}%" }) to { it: UIItem, optionStr: String ->
                (it as UIItemSpinner).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("sliderd,")) {
            val arg = args.split(',')
            UIItemHorzSlider(parent, x, y, App.getConfigDouble(optionName), arg[1].toDouble(), arg[2].toDouble(), CONFIG_SPINNER_WIDTH) to { it: UIItem, optionStr: String ->
                (it as UIItemHorzSlider).selectionChangeListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else if (args.startsWith("spinnerimul,")) {
            val arg = args.split(',')
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
            val keyWidth = optionName.substringBefore(',')
            val keyHeight = optionName.substringAfter(',')
            UIItemTextLineInput(parent, x, y, CONFIG_SPINNER_WIDTH,
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
            UIItemTextLineInput(parent, x, y, CONFIG_TYPEIN_WIDTH, defaultValue = { App.getConfigString(optionName) }) to { it: UIItem, optionStr: String ->
                (it as UIItemTextLineInput).textCommitListener = {
                    App.setConfig(optionStr, it)
                }
            }
        }
        else throw IllegalArgumentException(args)
    }

}