package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2017-11-25.
 */
class UITooltip : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

    override var openCloseTime: Second = 0f

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

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
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

    override fun updateUI(delta: Float) {
        setPosition(Terrarum.mouseScreenX, Terrarum.mouseScreenY)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }

}