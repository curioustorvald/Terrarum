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
    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keyPressed(key: Int, c: Char) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keyReleased(key: Int, c: Char) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
    }

    override var width: Int = terminal.displayW// + some
    override var height: Int = terminal.displayH// + frame

    override fun mousePressed(button: Int, x: Int, y: Int) {
        // monitor on/off, reset switch
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
    }

    override fun mouseWheelMoved(change: Int) {
    }

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
}