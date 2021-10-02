package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.langpack.Lang
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2021-10-01.
 */
class UIAutosaveNotifier : UICanvas() {

    companion object {
        const val WIDTH = 240
        const val HEIGHT = 24
    }

    override var width = WIDTH
    override var height = HEIGHT
    override var openCloseTime = 0.12f //COMMON_OPEN_CLOSE

    private val spinner = CommonResourcePool.getAsTextureRegionPack("inline_loading_spinner")
    private var spinnerTimer = 0f
    private var spinnerFrame = 0
    private val spinnerInterval = 1f / 60f

    override fun updateUI(delta: Float) {
        spinnerTimer += delta
        while (spinnerTimer > spinnerInterval) {
            spinnerFrame = (spinnerFrame + 1) % 32
            spinnerTimer -= spinnerInterval
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val spin = spinner.get(spinnerFrame % 8, spinnerFrame / 8)
        val offX = (App.scr.tvSafeGraphicsWidth * 1.25f).roundToInt().toFloat()
        val offY = App.scr.tvSafeGraphicsHeight + 4f
        
        batch.color = Color.WHITE
        batch.draw(spin, offX, offY)
        App.fontGame.draw(batch, Lang["MENU_IO_SAVING"], offX + 30f, offY)
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

    override fun dispose() {
    }
}