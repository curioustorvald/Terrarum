package com.torvald.terrarum.ui

import com.torvald.imagefont.GameFontWhite
import com.jme3.math.FastMath
import com.torvald.terrarum.setBlendDisable
import com.torvald.terrarum.setBlendNormal
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*

/**
 * Created by minjaesong on 16-01-27.
 */
class MessageWindow @Throws(SlickException::class)
constructor(override var width: Int, isBlackVariant: Boolean) : UICanvas {

    private var segmentLeft: Image? = null
    private var segmentRight: Image? = null
    private var segmentBody: Image? = null

    private lateinit var messagesList: Array<String>
    override var height: Int = 0
    private val messageWindowRadius: Int

    private var uiFont: Font? = null
    private var fontCol: Color = if (!isBlackVariant) Color.black else Color.white
    private val GLYPH_HEIGHT = 20

    override var openCloseTime: Int = OPEN_CLOSE_TIME

    internal var opacity = 0f
    internal var openCloseCounter = 0

    private lateinit var uidrawCanvas: Image // render all the images and fonts here; will be faded

    init {
        if (!isBlackVariant) {
            segmentLeft = Image("./res/graphics/gui/message_twoline_white_left.png");
            segmentRight = Image("./res/graphics/gui/message_twoline_white_right.png");
            segmentBody = Image("./res/graphics/gui/message_twoline_white_body.png");
        }
        else {
            segmentLeft = Image("./res/graphics/gui/message_twoline_black_left.png")
            segmentRight = Image("./res/graphics/gui/message_twoline_black_right.png")
            segmentBody = Image("./res/graphics/gui/message_twoline_black_body.png")
        }
        uiFont = GameFontWhite()
        height = segmentLeft!!.height
        messageWindowRadius = segmentLeft!!.width
        messagesList = arrayOf("", "")
        uidrawCanvas = Image(FastMath.nearestPowerOfTwo(width), FastMath.nearestPowerOfTwo(height))
    }

    fun setMessage(messagesList: Array<String>) {
        this.messagesList = messagesList
    }

    override fun update(gc: GameContainer, delta: Int) {

    }

    override fun render(gc: GameContainer, g: Graphics) {
        val canvasG = uidrawCanvas.graphics

        setBlendDisable()
        drawSegments(canvasG)
        canvasG.setDrawMode(Graphics.MODE_ALPHA_MAP)
        drawSegments(canvasG)

        canvasG.font = uiFont

        canvasG.setDrawMode(Graphics.MODE_NORMAL)
        for (i in 0..Math.min(messagesList.size, MESSAGES_DISPLAY) - 1) {
            canvasG.color = fontCol
            canvasG.drawString(messagesList[i], (messageWindowRadius + 4).toFloat(), (messageWindowRadius + GLYPH_HEIGHT * i).toFloat())
        }

        setBlendNormal()
        g.drawImage(uidrawCanvas, 0f, 0f, Color(1f,1f,1f,opacity))

        canvasG.clear()
    }

    override fun processInput(input: Input) {

    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        openCloseCounter += delta
        opacity = FastMath.interpolateLinear(openCloseCounter.toFloat() / openCloseTime.toFloat(),
                0f, 1f
        )
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        openCloseCounter += delta
        opacity = FastMath.interpolateLinear(openCloseCounter.toFloat() / openCloseTime.toFloat(),
                1f, 0f
        )
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        opacity = 1f
        openCloseCounter = 0
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        opacity = 0f
        openCloseCounter = 0
    }

    private fun drawSegments(g: Graphics) {
        g.drawImage(segmentLeft, 0f, 0f)
        val scaledSegCentre = segmentBody!!.getScaledCopy(
                width - (segmentRight!!.width + segmentLeft!!.width), segmentLeft!!.height)
        g.drawImage(scaledSegCentre, segmentLeft!!.width.toFloat(), 0f)
        g.drawImage(segmentRight, (width - segmentRight!!.width).toFloat(), 0f)
    }

    companion object {
        // private int messagesShowingIndex = 0;
        val MESSAGES_DISPLAY = 2
        val OPEN_CLOSE_TIME = 160
    }
}
