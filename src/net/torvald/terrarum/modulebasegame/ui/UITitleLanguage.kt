package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButtonList

class UITitleLanguage : UICanvas() {

    val menuLabels = arrayOf(
            "MENU_LABEL_RETURN"
    )


    override var openCloseTime: Second = 0f


    private val textButtonLineHeight = 32

    private val localeList = Lang.languageList.toList().sorted()
    private val localeFirstHalf = localeList.subList(0, localeList.size / 2)
    private val localeSecondHalf = localeList.subList(localeList.size / 2, localeList.size)

    override var width = 480
    override var height = maxOf(localeFirstHalf.size, localeSecondHalf.size) * textButtonLineHeight


    private val textArea1 = UIItemTextButtonList(this,
            textButtonLineHeight,
            localeFirstHalf.map { Lang.getByLocale("MENU_LANGUAGE_THIS", it, true) ?: "!ERR: $it" }.toTypedArray(),
            (App.scr.width - width) / 2, (App.scr.height - height) / 2,
            width / 2, height,
            textAreaWidth = width / 2,
            readFromLang = false,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )
    private val textArea2 = UIItemTextButtonList(this,
            textButtonLineHeight,
            localeSecondHalf.map { Lang.getByLocale("MENU_LANGUAGE_THIS", it, true) ?: "!ERR: $it" }.toTypedArray(),
            (App.scr.width - width) / 2 + (width / 2), (App.scr.height - height) / 2,
            width / 2, height,
            textAreaWidth = width / 2,
            readFromLang = false,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )


    init {


        //textArea.entireText = Lang.languageList.toList().sorted().map { Lang.langpack["MENU_LANGUAGE_THIS_$it"] ?: "!ERR: $it" }

        ////////////////////////////




        // attach listeners
        textArea1.selectionChangeListener = { _, newSelectionIndex ->
            App.GAME_LOCALE = localeList[newSelectionIndex]
            App.setConfig("language", localeList[newSelectionIndex])
            textArea2.deselect()
        }
        textArea2.selectionChangeListener = { _, newSelectionIndex ->
            App.GAME_LOCALE = localeList[newSelectionIndex + localeFirstHalf.size]
            App.setConfig("language", localeList[newSelectionIndex + localeFirstHalf.size])
            textArea1.deselect()
        }

        // highlight initial
        textArea1.buttons.forEachIndexed { index, it ->
            if (it.labelText == Lang["MENU_LANGUAGE_THIS"]) {
                textArea1.select(index)
            }
        }
        textArea2.buttons.forEachIndexed { index, it ->
            if (it.labelText == Lang["MENU_LANGUAGE_THIS"]) {
                textArea2.select(index)
            }
        }

    }

    override fun updateUI(delta: Float) {
        textArea1.update(delta)
        textArea2.update(delta)

        //AppLoader.printdbg(this, "should be printing indefinitely")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        batch.color = Color.WHITE
        textArea1.render(batch, camera)
        textArea2.render(batch, camera)
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