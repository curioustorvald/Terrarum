package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CreditSingleton
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextArea
import net.torvald.terrarum.ui.UIItemTextButtonList

class UITitleRemoConCredits(val superMenu: UICanvas) : UICanvas() {


    val menuLabels = arrayOf(
            "MENU_LABEL_CREDITS",
            "MENU_CREDIT_GPL_DNT",
            "MENU_LABEL_RETURN"
    )


    override var width: Int = UITitleRemoConRoot.remoConWidth
    override var height: Int = UITitleRemoConRoot.getRemoConHeight(menuLabels)
    override var openCloseTime: Second = 0f


    private val textAreaHMargin = 48
    private val textAreaWidth = (Terrarum.WIDTH * 0.75).toInt()
    private val textAreaHeight = Terrarum.HEIGHT - textAreaHMargin * 2
    private val textArea = UIItemTextArea(this,
            Terrarum.WIDTH - textAreaWidth, textAreaHMargin,
            textAreaWidth, textAreaHeight
    )
    private var drawTextArea = true


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
            defaultSelection = 0 // show CREDITS
    )

    init {
        uiItems.add(menubar)
        uiItems.add(textArea)



        ////////////////////////////



        // attach listeners
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_RETURN")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            menubar.selectedIndex = menubar.defaultSelection
            superMenu.setAsOpen()
        }

        menubar.selectionChangeListener = { _, newIndex ->
            textArea.scrollPos = 0

            if (newIndex == menuLabels.indexOf("MENU_LABEL_CREDITS")) {
                textArea.setWallOfText(CreditSingleton.credit)
                drawTextArea = true
            }
            else if (newIndex == menuLabels.indexOf("MENU_CREDIT_GPL_DNT")) {
                textArea.setWallOfText(CreditSingleton.gpl3)
                drawTextArea = true
            }
            else {
                drawTextArea = false
            }
        }
    }

    override fun updateUI(delta: Float) {
        menubar.update(delta)
        if (drawTextArea) {
            textArea.update(delta)
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        menubar.render(batch, camera)
        if (drawTextArea) {
            batch.color = Color.WHITE
            textArea.render(batch, camera)
        }
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