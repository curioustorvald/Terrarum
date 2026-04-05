package net.torvald.terrarum

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemHorzSlider

/**
 * Draws the loading bar and subtitle on the cold-boot splash screen.
 * Called from App.drawSplash() (Java static context) on the GL thread.
 *
 * Created by minjaesong on 2026-04-06.
 */
object SplashScreen {

    private val stubParent = object : UICanvas() {
        override var width = 0
        override var height = 0
        override fun updateImpl(delta: Float) {}
        override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {}
        override fun dispose() {}
    }

    private var loadingBar: UIItemHorzSlider? = null
    private var lastBarWidth = 0

    private val subtitleCol = Color(0.75f, 0.75f, 0.75f, 1f)

    @JvmStatic
    fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        val progress = CommonResourcePool.loadingProgress
        val barWidth = (Toolkit.drawWidth * 0.4f).toInt().coerceAtLeast(200)
        val barX = (Toolkit.drawWidth - barWidth) / 2
        val barY = App.scr.height - 48

        if (loadingBar == null || lastBarWidth != barWidth) {
            lastBarWidth = barWidth
            loadingBar = UIItemHorzSlider(stubParent, barX, barY, 0.0, 0.0, 1.0, barWidth).also {
                it.suppressHaptic = true
                it.isEnabled = false
            }
        }

        val bar = loadingBar ?: return
        bar.posX = barX
        bar.posY = barY
        bar.handleWidth = (progress * barWidth).toInt().coerceIn(12, barWidth)

        val subtitle = "${App.GAME_NAME}  ${App.getVERSION_STRING()}"
        val subtitleW = App.fontGame.getWidth(subtitle)
        val subtitleX = (Toolkit.drawWidth - subtitleW) / 2f
        val subtitleY = (barY - 20).toFloat() // leave gap above the bar

        batch.begin()
        batch.color = subtitleCol
        App.fontGame.draw(batch, subtitle, subtitleX, subtitleY)
        batch.color = Color.WHITE
        bar.render(0f, batch, camera)
        batch.end()
    }
}
