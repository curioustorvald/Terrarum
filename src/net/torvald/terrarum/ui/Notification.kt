package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.SlickException

/**
 * Created by minjaesong on 16-01-23.
 */
class Notification @Throws(SlickException::class)
constructor() : UICanvas {

    private val SHOWUP_MAX = 15000

    override var width: Int = 500

    internal var msgUI = MessageWindow(width, true)

    override var height: Int = msgUI.height
    private val visibleTime = Math.min(
            Terrarum.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    )
    private var displayTimer = 0

    internal var message: Array<String> = Array(MessageWindow.MESSAGES_DISPLAY, { i -> ""})

    override var openCloseTime: Int = MessageWindow.OPEN_CLOSE_TIME

    override var handler: UIHandler? = null

    init {
    }

    override fun update(gc: GameContainer, delta: Int) {
        if (handler!!.isOpened)
            displayTimer += delta

        if (displayTimer >= visibleTime)
            handler!!.setAsClose()
    }

    override fun render(gc: GameContainer, g: Graphics) {
        msgUI.render(gc, g)
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        handler!!.opacity = FastMath.interpolateLinear(handler!!.openCloseCounter.toFloat() / openCloseTime.toFloat(),
                0f, 1f
        )
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        handler!!.opacity = FastMath.interpolateLinear(handler!!.openCloseCounter.toFloat() / openCloseTime.toFloat(),
                1f, 0f
        )
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        handler!!.opacity = 1f
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        handler!!.opacity = 0f
    }

    override fun processInput(input: Input) {
    }

    fun sendNotification(message: Array<String>) {
        this.message = message
        msgUI.setMessage(this.message)
        handler!!.openCloseCounter = 0
        handler!!.opacity = 0f
        handler!!.setAsOpen()
    }
}
