package net.torvald.terrarum.ui

import net.torvald.imagefont.GameFontWhite
import com.jme3.math.FastMath
import net.torvald.terrarum.blendDisable
import net.torvald.terrarum.blendNormal
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

    override var handler: UIHandler? = null

    init {
        if (!isBlackVariant) {
            segmentLeft = Image("./assets/graphics/gui/message_twoline_white_left.png");
            segmentRight = Image("./assets/graphics/gui/message_twoline_white_right.png");
            segmentBody = Image("./assets/graphics/gui/message_twoline_white_body.png");
        }
        else {
            segmentLeft = Image("./assets/graphics/gui/message_twoline_black_left.png")
            segmentRight = Image("./assets/graphics/gui/message_twoline_black_right.png")
            segmentBody = Image("./assets/graphics/gui/message_twoline_black_body.png")
        }
        uiFont = GameFontWhite()
        height = segmentLeft!!.height
        messageWindowRadius = segmentLeft!!.width
        messagesList = arrayOf("", "")
    }

    fun setMessage(messagesList: Array<String>) {
        this.messagesList = messagesList
    }

    override fun update(gc: GameContainer, delta: Int) {

    }

    override fun render(gc: GameContainer, g: Graphics) {
        blendDisable()

        drawSegments(g)
        g.setDrawMode(Graphics.MODE_ALPHA_MAP)
        drawSegments(g)

        g.font = uiFont

        g.setDrawMode(Graphics.MODE_NORMAL)
        for (i in 0..Math.min(messagesList.size, MESSAGES_DISPLAY) - 1) {
            g.color = fontCol
            g.drawString(messagesList[i], (messageWindowRadius + 4).toFloat(), (messageWindowRadius + GLYPH_HEIGHT * i).toFloat())
        }

        blendNormal()
    }

    override fun processInput(input: Input) {

    }

    override fun doOpening(gc: GameContainer, delta: Int) {
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
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
