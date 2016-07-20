package net.torvald.terrarum.ui

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-07-20.
 */
class UIQuickBar : UICanvas {
    override var width: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
    override var height: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
    /**
     * In milliseconds
     */
    override var openCloseTime: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
    override var openCloseTimer: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }

    override var handler: UIHandler? = null

    override fun update(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun render(gc: GameContainer, g: Graphics) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun processInput(input: Input) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val SLOT_COUNT = 10
    }
}