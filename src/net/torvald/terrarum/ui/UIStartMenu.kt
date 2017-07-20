package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum

class UIStartMenu : UICanvas() {

    companion object {
        /** Contains STRING_IDs */
        val menuLabels = arrayOf(
                "MENU_MODE_SINGLEPLAYER",
                "MENU_OPTIONS",
                "MENU_MODULES",
                "MENU_LABEL_LANGUAGE",
                "MENU_LABEL_EXIT"
        )

        val menubarOffY = Terrarum.HEIGHT - 180 - 40 * menuLabels.size.plus(1)
    }



    override var width: Int = 240
    override var height: Int = 40 * menuLabels.size.plus(1)
    override var handler: UIHandler? = null
    override var openCloseTime = 0f


    private val menubar = UIItemTextButtonList(
            this,
            menuLabels,
            240, this.height,
            textAreaWidth = 240,
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
        menubar.buttons[3].clickOnceListener = { _, _, _ -> System.exit(0) }
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

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }
}