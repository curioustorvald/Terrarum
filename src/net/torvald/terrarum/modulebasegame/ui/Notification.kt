package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2016-01-23.
 */
class Notification : UICanvas() {

    private var fontCol: Color = Color.WHITE // assuming alpha of 1.0

    override var openCloseTime: Second = OPEN_CLOSE_TIME

    private val LRmargin = 0f // there's "base value" of 8 px for LR (width of segment tile)


    private val SHOWUP_MAX = 6500

    override var width: Int = 500

    override var height: Int = 0
    private val visibleTime = Math.min(
            AppLoader.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    ) / 1000f
    private var displayTimer = 0f

    internal var message: List<String> = listOf("")


    init {
    }

    override fun updateUI(delta: Float) {
        if (handler.isOpened)
            displayTimer += delta

        if (displayTimer >= visibleTime) {
            handler.setAsClose()
            displayTimer = 0f
        }
    }

    private val drawColor = Color(1f, 1f, 1f, 1f)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)
        drawColor.a = handler.opacity
        fontCol.a = handler.opacity

        val realTextWidth = 12 + if (message.size == 1)
            AppLoader.fontGame.getWidth(message[0])
        else
            message.map { AppLoader.fontGame.getWidth(it) }.sorted().last()
        val displayedTextWidth = maxOf(240, realTextWidth)

        // force the UI to the centre of the screen
        this.posX = (AppLoader.screenSize.screenW - displayedTextWidth) / 2

        val textHeight = message.size * AppLoader.fontGame.lineHeight

        batch.color = drawColor

        FloatDrawer(batch, 0f, -textHeight, displayedTextWidth.toFloat(), textHeight)

        batch.color = fontCol
        message.forEachIndexed { index, s ->
            val xoff = 6 + (displayedTextWidth - realTextWidth) / 2
            val y = -textHeight + AppLoader.fontGame.lineHeight * index
            AppLoader.fontGame.draw(batch, s, LRmargin + xoff, y)
        }


        // dunno why, it doesn't work without this.
        drawColor.a = 1f
        fontCol.a = 1f
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
    
    fun sendNotification(message: List<String>) {
        this.message = message
        handler.openCloseCounter = 0f
        handler.opacity = 0f
        handler.setAsOpen()
    }

    override fun dispose() {
    }

    companion object {
        // private int messagesShowingIndex = 0;
        val OPEN_CLOSE_TIME = 0.16f
    }
}
