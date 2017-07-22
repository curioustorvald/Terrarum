package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum

class UITitleRemoConRoot : UICanvas() {

    companion object {
        val remoConWidth = 240
        fun getRemoConHeight(menu: Array<String>) = 36 * menu.size.plus(1)
        val menubarOffY: Int; get() = Terrarum.HEIGHT / 2 - (Terrarum.fontGame.lineHeight * 1.5).toInt()
    }


    /** Contains STRING_IDs */
    val menuLabels = arrayOf(
            "MENU_MODE_SINGLEPLAYER",
            "MENU_OPTIONS",
            "MENU_MODULES",
            "MENU_LABEL_LANGUAGE",
            "MENU_LABEL_CREDITS",
            "MENU_LABEL_QUIT"
    )


    override var width: Int = remoConWidth
    override var height: Int = getRemoConHeight(menuLabels)
    override var openCloseTime = 0f


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


    //private val paneCredits = UIHandler()
    private val remoConCredits = UIHandler(UITitleRemoConCredits(this))


    init {
        uiItems.add(menubar)


        // attach listeners
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_QUIT")].clickOnceListener = { _, _, _ -> System.exit(0) }
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