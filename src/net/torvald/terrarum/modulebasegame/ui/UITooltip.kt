package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINotControllable

/**
 * Created by minjaesong on 2017-11-25.
 */
@UINotControllable
class UITooltip : UICanvas() {

    init {
        handler.allowESCtoClose = false
        handler.alwaysUpdate = true
    }

    override var openCloseTime: Second = OPENCLOSE_GENERIC * 0.72f

    private val tooltipBackCol = Color.WHITE
    private val tooltipForeCol = Color(0xfafafaff.toInt())

    private var msgBuffer = ArrayList<String>()
    var message: String = ""
        set(value) {
            field = value
            msgBuffer.clear()

            msgBuffer.addAll(value.split('\n'))
            msgWidth = msgBuffer.maxOf { font.getWidth(it) }
        }

    private val font = App.fontGame
    private var msgWidth = 0

    val textMarginX = 4

    override var width: Int
        get() = msgWidth + (textMarginX + 36) * 2
        set(value) { throw Error("You are not supposed to set the width of the tooltip manually.") }
    override var height: Int
        get() = 36 * 2 + font.lineHeight.toInt()
        set(value) { throw Error("You are not supposed to set the height of the tooltip manually.") }


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        val mouseXoff = 28f
        val mouseYoff = 0f
        val txtW = msgWidth + 2f * textMarginX

        val tooltipW = txtW
        val tooltipH = font.lineHeight * msgBuffer.size

        val tooltipX = mouseXoff
        val tooltipY = mouseYoff - (tooltipH / 2)


        batch.color = tooltipBackCol

        Toolkit.drawBaloon(batch,
            tooltipX - textMarginX,
            tooltipY,
            tooltipW,
            font.lineHeight * msgBuffer.size,
            Notification.OPACITY
        )

        batch.color = tooltipForeCol

        msgBuffer.forEachIndexed { index, s ->
            font.draw(batch, s,
                    tooltipX,
                    tooltipY + font.lineHeight * index
            )
        }
    }

//    private var lastOpenSignalTime = 0L
//    private var debounceTime = 100*1000000L // miliseconds

    override fun setAsOpen() {
        handler.setAsOpen()
//        lastOpenSignalTime = System.nanoTime()
//        printdbg(this, "start open")
    }

    override fun setAsClose() {
//        printdbg(this, "Close called, time since last open: ${System.nanoTime() - lastOpenSignalTime}")
//        if (System.nanoTime() - lastOpenSignalTime >= debounceTime) {
//            printdbg(this, "start close")
            handler.setAsClose()
//        }
    }

    override fun updateImpl(delta: Float) {
        setPosition(Terrarum.mouseScreenX, Terrarum.mouseScreenY)

        if (isVisible && (TooltipManager.tooltipShowing.isEmpty() || TooltipManager.tooltipShowing.values.all { !it })) {
            INGAME.setTooltipMessage(null)
        }
    }

    override fun doOpening(delta: Float) {
        handler.opacity = handler.openCloseCounter / openCloseTime
    }

    override fun doClosing(delta: Float) {
        handler.opacity = (openCloseTime - handler.openCloseCounter) / openCloseTime
    }

    override fun endOpening(delta: Float) {
        handler.opacity = 1f
        // Tooltip must not acquire control of itself
    }

    override fun endClosing(delta: Float) {
        handler.opacity = 0f
        // Tooltip must not acquire control of itself
    }

    override fun show() {
        openingClickLatched = true
        // Tooltip must not acquire control of itself
        uiItems.forEach { it.show() }
        handler.subUIs.forEach { it.show() }
    }

    override fun hide() {
        uiItems.forEach { it.hide() }
        handler.subUIs.forEach { it.hide() }
        openingClickLatched = true // just in case `justOpened` detection fails
    }

    override fun dispose() {
    }

}