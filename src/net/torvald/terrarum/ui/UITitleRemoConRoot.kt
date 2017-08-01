package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
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
            0, menubarOffY,
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
    private val remoConCredits = UITitleRemoConCredits(this)

    private val remoConLanguage = UITitleRemoConLanguage(this)


    init {
        remoConLanguage.setPosition(0, 0)
        remoConCredits.setPosition(0, 0)



        addSubUI(remoConLanguage)
        addSubUI(remoConCredits)


        ////////////////////////////

        uiItems.add(menubar)



        // attach listeners
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_LANGUAGE")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            remoConLanguage.setAsOpen()
        }
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_CREDITS")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            remoConCredits.setAsOpen()
        }
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_QUIT")].clickOnceListener = { _, _, _ -> Thread.sleep(50); System.exit(0) }
    }

    override fun updateUI(delta: Float) {
        menubar.update(delta)
        //println("UITitleRemoConRoot bro u even updatez")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
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