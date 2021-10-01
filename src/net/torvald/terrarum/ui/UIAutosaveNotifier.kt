package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool

/**
 * Created by minjaesong on 2021-10-01.
 */
class UIAutosaveNotifier : UICanvas() {

    override var width: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var height: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var openCloseTime = 0.2f

    private val spinner = CommonResourcePool.getAsTextureRegionPack("inline_loading_spinner")
    private var spinnerTimer = 0f
    private var spinnerFrame = 0
    private val spinnerInterval = 1f / 60f

    override fun updateUI(delta: Float) {
        spinnerTimer += delta
        if (spinnerTimer > spinnerInterval) {
            spinnerFrame = (spinnerFrame + 1) % 32
            spinnerTimer -= spinnerInterval
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val spin = spinner.get(spinnerFrame % 8, spinnerFrame / 8)

        val inlineOffsetY = if (App.GAME_LOCALE.startsWith("th")) 0f
        else if (App.GAME_LOCALE.startsWith("ko")) 0f
        else 1f

        batch.draw(spin, posX.toFloat(), posY.toFloat())
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