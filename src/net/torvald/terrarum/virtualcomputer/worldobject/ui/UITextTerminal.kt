package net.torvald.terrarum.virtualcomputer.worldobject.ui

import net.torvald.terrarum.ui.*
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-09-08.
 */
class UITextTerminal(val terminal: Terminal) : UICanvas, KeyboardControlled, MouseControlled {
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

    override var width: Int = terminal.displayW// + some
    override var height: Int = terminal.displayH// + frame
    private var terminalDisplay = Image(terminal.displayW, terminal.displayH)

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
    override var openCloseTime: Int = OPENCLOSE_GENERIC

    override fun update(gc: GameContainer, delta: Int) {
        terminal.update(gc, delta)
    }

    override fun render(gc: GameContainer, g: Graphics) {
        terminal.render(gc, terminalDisplay.graphics)
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun doOpening(gc: GameContainer, delta: Int) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun doClosing(gc: GameContainer, delta: Int) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun endOpening(gc: GameContainer, delta: Int) {
    }

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    override fun endClosing(gc: GameContainer, delta: Int) {
    }
}