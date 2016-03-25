package com.torvald.terrarum.ui

import com.torvald.terrarum.Terrarum
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.SlickException

/**
 * Created by minjaesong on 16-01-23.
 */
class Notification @Throws(SlickException::class)
constructor() : UICanvas {

    override var width: Int = 0
    override var height: Int = 0
    internal var visibleTime: Int
    internal var showupTimeConuter = 0

    internal var isShowing = false
    internal var message: Array<String> = Array(MessageWindow.MESSAGES_DISPLAY, { i -> ""})

    internal var msgUI: MessageWindow

    override var openCloseTime: Int = MessageWindow.OPEN_CLOSE_TIME

    private val SHOWUP_MAX = 15000

    init {
        width = 500
        msgUI = MessageWindow(width, true)
        height = msgUI.height
        visibleTime = Math.min(
                Terrarum.getConfigInt("notificationshowuptime"),
                SHOWUP_MAX
        )
    }

    override fun update(gc: GameContainer, delta: Int) {
        if (showupTimeConuter >= visibleTime && isShowing) {
            // invoke closing mode
            doClosing(gc, delta)
            // check if msgUI is fully fade out
            if (msgUI.opacity <= 0.001f) {
                endClosing(gc, delta)
                isShowing = false
            }
        }

        if (isShowing) {
            showupTimeConuter += delta
        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        if (isShowing) {
            msgUI.render(gc, g)
        }
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        msgUI.doOpening(gc, delta)
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        msgUI.doClosing(gc, delta)
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        msgUI.endOpening(gc, delta)
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        msgUI.endClosing(gc, delta)
    }

    override fun processInput(input: Input) {

    }

    fun sendNotification(gc: GameContainer, delta: Int, message: Array<String>) {
        isShowing = true
        this.message = message
        msgUI.setMessage(this.message)
        showupTimeConuter = 0
    }
}
