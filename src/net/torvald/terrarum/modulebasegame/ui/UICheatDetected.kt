package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2017-12-06.
 */
class UICheatDetected : UICanvas() {

    override var width: Int
        get() = App.scr.width
        set(value) { throw UnsupportedOperationException() }

    override var height: Int
        get() = App.scr.height
        set(value) { throw UnsupportedOperationException() }

    override var openCloseTime: Second = 0f



    private val backgroundCol = Color(0x181818C0)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        Terrarum.ingame?.consoleHandler?.setAsClose()
        Terrarum.ingame?.consoleHandler?.isVisible = false

        batch.color = backgroundCol
        batch.fillRect(0f, 0f, width.toFloat(), height.toFloat())

        batch.color = Color.WHITE
        val txt = Lang["ERROR_GENERIC_CHEATING"]
        val txtW = App.fontGame.getWidth(txt)
        val txtH = App.fontGame.lineHeight.toInt()

        App.fontGame.draw(batch, txt, width.minus(txtW).ushr(1).toFloat(), height.minus(txtH).ushr(1).toFloat())
    }

    override fun updateUI(delta: Float) {
    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.paused = true
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