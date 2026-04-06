package net.torvald.terrarum

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemHorzSlider
import kotlin.math.max

/**
 * Draws the cold-boot splash screen: backdrop, logo, loading bar, subtitle, and health messages.
 * Consolidated from App.drawSplash() so all splash rendering lives here.
 *
 * Progress is time-based: p = t / (t + HALF_TIME_MS), snapping to 1.0 when
 * [loadingComplete] is set to true by App right before transitioning to the title screen.
 *
 * Created by minjaesong on 2026-04-06.
 */
object SplashScreen {

    /** Set to true by App when loading completes, to snap the bar to 100%. */
    @JvmField @Volatile var loadingComplete = false

    private const val HALF_TIME_MS = 1500L  // progress = t / (t + HALF_TIME_MS)

    private val stubParent = object : UICanvas() {
        override var width = 0
        override var height = 0
        override fun updateImpl(delta: Float) {}
        override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {}
        override fun dispose() {}
    }

    private var loadingBar: UIItemHorzSlider? = null
    private var lastBarWidth = 0
    private var startTime = 0L

    private val subtitleCol = Color(0.75f, 0.75f, 0.75f, 1f)

    @JvmStatic fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        App.setCameraPosition(0f, 0f)

        val batch = App.logoBatch
        val scr = App.scr
        val drawWidth = Toolkit.drawWidth
        val showHealth = App.getConfigBoolean("showhealthmessageonstartup")

        batch.color = Color.WHITE
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val safetyTextLen = if (showHealth) App.fontGame.getWidth(Lang["APP_WARNING_HEALTH_AND_SAFETY", true]) else 0
        val logoPosX0 = (drawWidth - App.splashScreenLogo.regionWidth - safetyTextLen) ushr 1
        val logoPosY = Math.round(scr.height / 15f)
        val textY = logoPosY + App.splashScreenLogo.regionHeight - 16

        // backdrop
        App.splashBackdrop?.let { backdrop ->
            batch.setShader(null)
            batch.inUse {
                val size = max(scr.width, scr.height).toFloat()
                val x = if (scr.width > scr.height) 0f else (scr.width - size) / 2f
                val y = if (scr.width > scr.height) (scr.height - size) / 2f else 0f
                batch.draw(backdrop, x, y, size, size)
            }
        }

        val logoPosX = logoPosX0

        // logo reflection
        batch.setShader(App.shaderReflect)
        batch.color = Color.WHITE
        batch.inUse {
            if (showHealth) {
                batch.draw(App.splashScreenLogo, logoPosX.toFloat(), (logoPosY + App.splashScreenLogo.regionHeight).toFloat())
            } else {
                batch.draw(App.splashScreenLogo,
                    (drawWidth - App.splashScreenLogo.regionWidth) / 2f,
                    (scr.height - App.splashScreenLogo.regionHeight * 2) / 2f + App.splashScreenLogo.regionHeight)
            }
        }

        // logo
        batch.setShader(null)
        batch.inUse {
            if (showHealth) {
                batch.draw(App.splashScreenLogo, logoPosX.toFloat(), logoPosY.toFloat())
            } else {
                batch.draw(App.splashScreenLogo,
                    (drawWidth - App.splashScreenLogo.regionWidth) / 2f,
                    (scr.height - App.splashScreenLogo.regionHeight * 2) / 2f)
            }
        }
        
        // loading bar+subtitle
        if (startTime == 0L) startTime = System.currentTimeMillis()

        val elapsed = System.currentTimeMillis() - startTime
        val progress = if (loadingComplete) 1f
                       else elapsed.toFloat() / (elapsed + HALF_TIME_MS).toFloat()

        val barWidth = (Toolkit.drawWidth * 0.4f).toInt().coerceAtLeast(200)
        val barX = (Toolkit.drawWidth - barWidth) / 2
        val barY = App.scr.height - App.scr.tvSafeGraphicsHeight - 24

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

        val subtitle = "Reticulating Splines..."
        val subtitleW = App.fontGame.getWidth(subtitle)
        val subtitleX = (Toolkit.drawWidth - subtitleW) / 2f
        val subtitleY = (barY - 32).toFloat()

        batch.inUse {
            batch.color = subtitleCol
            App.fontGame.draw(batch, subtitle, subtitleX, subtitleY)
            batch.color = Color.WHITE
            bar.render(0f, batch, camera)
        }

        // health messages
        if (showHealth) {
            batch.setShader(null)
            batch.inUse {
                batch.color = App.splashTextCol
                App.fontGame.draw(batch, Lang["APP_WARNING_HEALTH_AND_SAFETY", true],
                    (logoPosX + App.splashScreenLogo.regionWidth).toFloat(), textY.toFloat())

                val tex1 = CommonResourcePool.getAsTexture("title_health1")
                val tex2 = CommonResourcePool.getAsTexture("title_health2")
                val virtualHeight = scr.height - logoPosY - App.splashScreenLogo.regionHeight / 4
                val virtualHeightOffset = scr.height - virtualHeight
                batch.drawFlipped(tex1, ((drawWidth - tex1.width) ushr 1).toFloat(), (virtualHeightOffset + (virtualHeight ushr 1) - 16).toFloat(), tex1.width.toFloat(), (-tex1.height).toFloat())
                batch.drawFlipped(tex2, ((drawWidth - tex2.width) ushr 1).toFloat(), (virtualHeightOffset + (virtualHeight ushr 1) + 16 + tex2.height).toFloat(), tex2.width.toFloat(), (-tex2.height).toFloat())
            }
        }

        App.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)

    }
}
