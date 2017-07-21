package net.torvald.terrarum.virtualcomputer.worldobject.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UICanvas.Companion.OPENCLOSE_GENERIC
import net.torvald.terrarum.virtualcomputer.terminal.Terminal

/**
 * Created by minjaesong on 16-09-08.
 */
class UITextTerminal(val terminal: Terminal) : UICanvas() {


    override var width: Int = terminal.displayW// + some
    override var height: Int = terminal.displayH// + frame



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

    /**
     * Do not modify handler.openCloseCounter here.
     */
    override fun doOpening(delta: Float) {
    }

    /**
     * Do not modify handler.openCloseCounter here.
     */
    override fun doClosing(delta: Float) {
    }

    /**
     * Do not modify handler.openCloseCounter here.
     */
    override fun endOpening(delta: Float) {
    }

    /**
     * Do not modify handler.openCloseCounter here.
     */
    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}