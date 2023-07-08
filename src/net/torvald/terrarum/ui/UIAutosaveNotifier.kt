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
        const val WIDTH = 180
        const val HEIGHT = 24
    }

    override var width = WIDTH
    override var height = HEIGHT
    override var openCloseTime = 0.12f //COMMON_OPEN_CLOSE

    private val spinner = CommonResourcePool.getAsTextureRegionPack("inline_loading_spinner")
    private var spinnerTimer = 0f
    private var spinnerFrame = 0
    private val spinnerInterval = 1f / 60f

    private var errorTimer = 0f
    private var errorMax = App.getConfigInt("notificationshowuptime") / 1000f
    private var errored = false

    private var normalCol = Color.WHITE
    private var errorCol = Toolkit.Theme.COL_RED

    override fun updateUI(delta: Float) {
        spinnerTimer += delta
        while (spinnerTimer > spinnerInterval) {
            spinnerFrame = (spinnerFrame + 1) % 32
            spinnerTimer -= spinnerInterval
        }

        if (errored) {
            errorTimer += delta

            if (errorTimer >= errorMax) {
                this.setAsClose()
            }
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val spin = spinner.get(spinnerFrame % 8, spinnerFrame / 8)
        val offX = App.scr.width - WIDTH - (App.scr.tvSafeGraphicsWidth * 1.25f).roundToInt().toFloat()
        val offY = App.scr.height - HEIGHT - App.scr.tvSafeGraphicsHeight - 9f // +9 to align to quickslot and watch UI

        val text = if (errored) Lang["ERROR_GENERIC_TEXT"].replace(".","") else Lang["MENU_IO_SAVING"]
        if (!errored) {
            batch.color = normalCol
            batch.draw(spin, offX, offY)
        }
        else {
            batch.color = errorCol
            batch.draw(spinner.get(0,4), offX, offY)
        }
        batch.color = if (errored) errorCol else normalCol
        App.fontGame.draw(batch, text, offX + 30f, offY - 2f)
    }

    fun setAsError() {
        errored = true
    }

    override fun setAsOpen() {
        errored = false
        super.setAsOpen()
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