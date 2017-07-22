package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameactors.Second

class UITitleRemoConCredits(val superMenu: UICanvas) : UICanvas() {


    val menuLabels = arrayOf(
            "MENU_LABEL_CREDITS",
            "MENU_CREDIT_GPL_DNT",
            "MENU_LABEL_RETURN"
    )


    override var width: Int = UITitleRemoConRoot.remoConWidth
    override var height: Int = UITitleRemoConRoot.getRemoConHeight(menuLabels)
    override var openCloseTime: Second = 0f


    private val menubar = UIItemTextButtonList(
            this,
            menuLabels,
            this.width, this.height,
            textAreaWidth = this.width,
            readFromLang = true,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )

    init {
        uiItems.add(menubar)


        // attach listeners
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_RETURN")].clickOnceListener = { _, _, _ ->
            superMenu.handler.setAsOpen()
            this.handler.setAsClose()
        }
    }

    override fun update(delta: Float) {
        menubar.update(delta)
    }

    override fun render(batch: SpriteBatch) {
        menubar.render(batch)
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