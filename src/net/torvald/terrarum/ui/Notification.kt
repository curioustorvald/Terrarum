package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.Second

/**
 * Created by minjaesong on 16-01-23.
 */
class Notification : UICanvas {

    private val SHOWUP_MAX = 15000

    override var width: Int = 500

    internal var msgUI = MessageWindow(width, true)

    override var height: Int = msgUI.height
    private val visibleTime = Math.min(
            Terrarum.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    )
    private var displayTimer = 0f

    internal var message: Array<String> = Array(MessageWindow.MESSAGES_DISPLAY, { "" })

    override var openCloseTime: Second = MessageWindow.OPEN_CLOSE_TIME

    override var handler: UIHandler? = null

    override fun update(delta: Float) {
        if (handler!!.isOpened)
            displayTimer += delta

        if (displayTimer >= visibleTime) {
            handler!!.setAsClose()
            displayTimer = 0f
        }
    }

    override fun render(batch: SpriteBatch) {
        msgUI.render(batch)
    }

    override fun processInput(delta: Float) {
    }

    override fun doOpening(delta: Float) {
        UICanvas.doOpeningFade(handler, openCloseTime)
    }

    override fun doClosing(delta: Float) {
        UICanvas.doClosingFade(handler, openCloseTime)
    }

    override fun endOpening(delta: Float) {
        UICanvas.endOpeningFade(handler)
    }

    override fun endClosing(delta: Float) {
        UICanvas.endClosingFade(handler)
    }
    
    fun sendNotification(message: Array<String>) {
        this.message = message
        msgUI.setMessage(this.message)
        handler!!.openCloseCounter = 0f
        handler!!.opacity = 0f
        handler!!.setAsOpen()
    }
}
