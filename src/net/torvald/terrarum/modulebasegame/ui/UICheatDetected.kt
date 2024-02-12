package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2017-12-06.
 */
class UICheatDetected : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

    override var width: Int
        get() = App.scr.width
        set(value) { throw UnsupportedOperationException() }

    override var height: Int
        get() = App.scr.height
        set(value) { throw UnsupportedOperationException() }

    override var openCloseTime: Second = 0f



    private val backgroundCol = Color(0x00000080)

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        Terrarum.ingame?.consoleHandler?.setAsClose()
        Terrarum.ingame?.consoleHandler?.isVisible = false

        Toolkit.blurEntireScreen(batch, camera as OrthographicCamera, 2f, 0, 0, width, height)
        batch.color = backgroundCol
        Toolkit.fillArea(batch, 0f, 0f, width.toFloat(), height.toFloat())

        batch.color = Color.WHITE
        val txt = Lang["ERROR_GENERIC_CHEATING"]
        val txtW = App.fontGame.getWidth(txt)
        val txtH = App.fontGame.lineHeight.toInt()

        App.fontGame.draw(batch, txt, width.minus(txtW).ushr(1).toFloat(), height.minus(txtH).ushr(1).toFloat())
    }

    override fun updateImpl(delta: Float) {
        INGAME.pause()
    }

    override fun doOpening(delta: Float) {
        INGAME.pause()
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