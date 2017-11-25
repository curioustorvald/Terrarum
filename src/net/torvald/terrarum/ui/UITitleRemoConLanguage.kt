package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppLoader
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.langpack.Lang

class UITitleRemoConLanguage(val superMenu: UICanvas) : UICanvas() {

    val menuLabels = arrayOf(
            "MENU_LABEL_RETURN"
    )


    override var width: Int = UITitleRemoConRoot.remoConWidth
    override var height: Int = UITitleRemoConRoot.getRemoConHeight(menuLabels)
    override var openCloseTime: Second = 0f


    private val menubar = UIItemTextButtonList(
            this,
            menuLabels,
            0, UITitleRemoConRoot.menubarOffY,
            this.width, this.height,
            textAreaWidth = this.width,
            readFromLang = true,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )


    private val textAreaHMargin = 48
    private val textAreaWidth = (Terrarum.WIDTH * 0.75).toInt()
    private val textAreaHeight = Terrarum.HEIGHT - textAreaHMargin * 2
    /*private val textArea = UIItemTextArea(this,
            Terrarum.WIDTH - textAreaWidth, textAreaHMargin,
            textAreaWidth, textAreaHeight,
            align = UIItemTextArea.Align.CENTRE
    )*/
    private val localeList = Lang.languageList.toList().sorted()
    private val textArea = UIItemTextButtonList(this,
            localeList.map { Lang.langpack["MENU_LANGUAGE_THIS_$it"] ?: "!ERR: $it" }.toTypedArray(),
            Terrarum.WIDTH - textAreaWidth, textAreaHMargin,
            textAreaWidth, textAreaHeight,
            textAreaWidth = textAreaWidth,
            readFromLang = false,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )


    init {
        uiItems.add(menubar)


        //textArea.entireText = Lang.languageList.toList().sorted().map { Lang.langpack["MENU_LANGUAGE_THIS_$it"] ?: "!ERR: $it" }

        ////////////////////////////




        // attach listeners
        textArea.selectionChangeListener = { _, newSelectionIndex ->
            TerrarumAppLoader.GAME_LOCALE = localeList[newSelectionIndex]
        }

        menubar.buttons[menuLabels.indexOf("MENU_LABEL_RETURN")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            menubar.selectedIndex = menubar.defaultSelection
            superMenu.setAsOpen()
        }
    }

    override fun updateUI(delta: Float) {
        menubar.update(delta)
        textArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        menubar.render(batch, camera)

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