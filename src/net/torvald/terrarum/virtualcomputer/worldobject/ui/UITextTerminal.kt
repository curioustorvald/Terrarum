package net.torvald.terrarum.virtualcomputer.worldobject.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UICanvas.Companion.OPENCLOSE_GENERIC
import net.torvald.terrarum.virtualcomputer.terminal.Terminal

/**
 * Created by minjaesong on 16-09-08.
 */
class UITextTerminal(val terminal: Terminal) : UICanvas, KeyControlled, MouseControlled {

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        return false
    }

    override var width: Int = terminal.displayW// + some
    override var height: Int = terminal.displayH// + frame

   

    /**
     * Usage: (in StateInGame:) uiHandlerField.ui.handler = uiHandlerField
     */
    override var handler: UIHandler? = null

    /**
     * In milliseconds
     *
     * Timer itself is implemented in the handler.
     */
    override var openCloseTime: Second = OPENCLOSE_GENERIC

    override fun update(delta: Float) {
        terminal.update(delta)
    }

    override fun render(batch: SpriteBatch) {
        //terminal.render(gc, terminalDisplay.graphics)
    }

    override fun processInput(delta: Float) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun doOpening(delta: Float) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun doClosing(delta: Float) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun endOpening(delta: Float) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}