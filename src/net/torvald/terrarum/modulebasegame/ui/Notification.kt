package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2016-01-23.
 */
class Notification : UICanvas() {

    private val SHOWUP_MAX = 15000

    override var width: Int = 500

    internal var msgUI = MessageWindow(width, true)

    override var height: Int = msgUI.height
    private val visibleTime = Math.min(
            AppLoader.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    )
    private var displayTimer = 0f

    internal var message: Array<String> = Array(MessageWindow.MESSAGES_DISPLAY, { "" })

    override var openCloseTime: Second = MessageWindow.OPEN_CLOSE_TIME

    override fun updateUI(delta: Float) {
        if (handler.isOpened)
            displayTimer += delta

        if (displayTimer >= visibleTime) {
            handler.setAsClose()
            displayTimer = 0f
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        msgUI.render(batch, camera)
    }

    override fun doOpening(delta: Float) {
        doOpeningFade(this, openCloseTime)
    }

    override fun doClosing(delta: Float) {
        doClosingFade(this, openCloseTime)
    }

    override fun endOpening(delta: Float) {
        endOpeningFade(this)
    }

    override fun endClosing(delta: Float) {
        endClosingFade(this)
    }
    
    fun sendNotification(message: Array<String>) {
        this.message = message
        msgUI.setMessage(this.message)
        handler.openCloseCounter = 0f
        handler.opacity = 0f
        handler.setAsOpen()
    }

    override fun dispose() {
    }
}
