package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2017-11-25.
 */
class UITooltip : UICanvas() {

    override var openCloseTime: Second = 0f

    private val tooltipBackCol = Color(0xd5d4d3ff.toInt())
    private val tooltipForeCol = Color(0x404040ff)

    var message: String = ""
        set(value) {
            field = value
            msgWidth = font.getWidth(value)
        }

    private val font = AppLoader.fontGame
    private var msgWidth = 0

    val textMarginX = 4

    override var width: Int
        get() = msgWidth + (textMarginX + FloatDrawer.tile.tileW) * 2
        set(value) { throw Error("You are not supposed to set the width of the tooltip manually.") }
    override var height: Int
        get() = FloatDrawer.tile.tileH * 2 + font.lineHeight.toInt()
        set(value) { throw Error("You are not supposed to set the height of the tooltip manually.") }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val mouseX = 4f
        val mouseY = 6f

        val tooltipYoff = 50
        val tooltipY = mouseY - height + tooltipYoff

        val txtW = msgWidth + 2f * textMarginX

        batch.color = tooltipBackCol
        FloatDrawer(batch, mouseX - textMarginX, tooltipY, txtW, font.lineHeight)
        batch.color = tooltipForeCol
        font.draw(batch, message,
                mouseX,
                mouseY - height + tooltipYoff
        )
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