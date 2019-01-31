package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2016-01-23.
 */
class Notification : UICanvas() {

    private val segment = SEGMENT_BLACK

    private var fontCol: Color = Color.WHITE // assuming alpha of 1.0

    override var openCloseTime: Second = OPEN_CLOSE_TIME

    private val LRmargin = 0f // there's "base value" of 8 px for LR (width of segment tile)


    private val SHOWUP_MAX = 6500

    override var width: Int = 500

    override var height: Int = segment.tileH
    private val visibleTime = Math.min(
            AppLoader.getConfigInt("notificationshowuptime"),
            SHOWUP_MAX
    ) / 1000f
    private var displayTimer = 0f

    internal var message: Array<String> = Array(MESSAGES_DISPLAY) { "" }


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

    private val textAreaHeight = 48f
    private val imageToTextAreaDelta = (segment.tileH - textAreaHeight) / 2

    private val drawColor = Color(1f,1f,1f,1f)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)
        drawColor.a = handler.opacity
        fontCol.a = handler.opacity

        val textWidth = width//maxOf(width, messagesList.map { Terrarum.fontGame.getWidth(it) }.sorted()[1])

        batch.color = drawColor

        batch.draw(segment.get(0, 0), -segment.tileW.toFloat(), 0f)
        batch.draw(segment.get(1, 0), 0f, 0f, textWidth.toFloat(), segment.tileH.toFloat())
        batch.draw(segment.get(2, 0), textWidth.toFloat(), 0f)

        batch.color = fontCol
        message.forEachIndexed { index, s ->
            val y = imageToTextAreaDelta + index * (textAreaHeight / 2) + (textAreaHeight / 2 - Terrarum.fontGame.lineHeight) / 2
            Terrarum.fontGame.draw(batch, s, LRmargin, y)
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
    
    fun sendNotification(message: Array<String>) {
        this.message = message
        handler.openCloseCounter = 0f
        handler.opacity = 0f
        handler.setAsOpen()
    }

    override fun dispose() {
    }

    companion object {
        // private int messagesShowingIndex = 0;
        val MESSAGES_DISPLAY = 2
        val OPEN_CLOSE_TIME = 0.16f


        // will be disposed by Terrarum (application main instance)
        val SEGMENT_BLACK = TextureRegionPack("assets/graphics/gui/message_black.tga", 8, 56)
        val SEGMENT_WHITE = TextureRegionPack("assets/graphics/gui/message_white.tga", 8, 56)
    }
}
