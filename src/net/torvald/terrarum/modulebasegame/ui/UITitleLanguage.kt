package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButtonList

class UITitleLanguage : UICanvas() {

    val menuLabels = arrayOf(
            "MENU_LABEL_RETURN"
    )


    override var openCloseTime: Second = 0f


    private val textAreaHMargin = 48
    override var width = (AppLoader.screenSize.screenW * 0.75).toInt()
    override var height = AppLoader.screenSize.screenH - textAreaHMargin * 2

    private val localeList = Lang.languageList.toList().sorted()
    private val textArea = UIItemTextButtonList(this,
            24,
            localeList.map { Lang.langpack["MENU_LANGUAGE_THIS_$it"] ?: "!ERR: $it" }.toTypedArray(),
            AppLoader.screenSize.screenW - width, textAreaHMargin,
            width, height,
            textAreaWidth = width,
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
        textArea.selectionChangeListener = { _, newSelectionIndex ->
            AppLoader.GAME_LOCALE = localeList[newSelectionIndex]
        }


    }

    override fun updateUI(delta: Float) {
        textArea.update(delta)

        //AppLoader.printdbg(this, "should be printing indefinitely")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        batch.color = Color.WHITE
        textArea.render(batch, camera)
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