package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.Second
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import kotlin.math.max

/**
 * This UI must be always updated; the easiest way is to set `handler.alwaysUpdate` to `true`
 *
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
            App.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    ) / 1000f
    private var displayTimer = 0f

    internal val messageQueue = Queue<List<String>>()
    private var messageDisplaying: List<String>? = null

    private val timeGaugeCol = Color(0x707070ff)

    init {
        handler.alwaysUpdate = true
    }

    override fun updateUI(delta: Float) {
        if (messageDisplaying == null && messageQueue.notEmpty() && handler.isClosed) {
            messageDisplaying = messageQueue.removeFirst()
            displayTimer = 0f
            handler.openCloseCounter = 0f
            handler.opacity = 0f
            handler.setAsOpen()
        }

        if (handler.isOpened)
            displayTimer += delta

        if (displayTimer >= visibleTime) {
            handler.setAsClose()
            displayTimer = 0f
            messageDisplaying = null
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)
        fontCol.a = handler.opacity * OPACITY


        if (messageDisplaying != null) {

            val realTextWidth = 12 + if (messageDisplaying!!.size == 1)
                App.fontGame.getWidth(messageDisplaying!![0])
            else
                messageDisplaying!!.map { App.fontGame.getWidth(it) }.sorted().last()
            val displayedTextWidth = max(240, realTextWidth)

            // force the UI to the centre of the screen
            this.posX = (App.scr.width - displayedTextWidth) / 2
            val textHeight = messageDisplaying!!.size * App.fontGame.lineHeight



            Toolkit.drawBaloon(
                batch,
                0f,
                -textHeight,
                displayedTextWidth.toFloat(),
                textHeight,
                handler.opacity * OPACITY
            )

            // draw time gauge
            if (displayTimer != 0f) {
                batch.color = timeGaugeCol
                val time = 1f - (displayTimer / visibleTime)
                val bw = displayedTextWidth * time
                val bx = (displayedTextWidth - bw) / 2
                Toolkit.drawStraightLine(batch, bx, 2f, bx + bw, 2f, false)
            }

            // draw texts
            batch.color = fontCol
            messageDisplaying!!.forEachIndexed { index, s ->
                val xoff = 6 + (displayedTextWidth - realTextWidth) / 2
                val y = -textHeight + App.fontGame.lineHeight * index
                App.fontGame.draw(batch, s, LRmargin + xoff, y - 1)
            }
        }


        // dunno why, it doesn't work without this.
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
        messageQueue.addLast(message)
    }

    override fun dispose() {
    }

    companion object {
        // private int messagesShowingIndex = 0;
        const val OPEN_CLOSE_TIME = 0.16f
        const val OPACITY = 0.9f
    }
}
