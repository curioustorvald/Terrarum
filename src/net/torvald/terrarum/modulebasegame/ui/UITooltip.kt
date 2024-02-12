package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
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

        if (isVisible && tooltipShowing.values.all { !it }) {
            INGAME.setTooltipMessage(null)
        }
    }

    override fun dispose() {
    }

}