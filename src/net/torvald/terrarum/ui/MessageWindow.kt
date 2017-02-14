package net.torvald.terrarum.ui

import net.torvald.imagefont.GameFontImpl
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
            segmentLeft = Image("./assets/graphics/gui/message_twoline_white_left.tga")
            segmentRight = Image("./assets/graphics/gui/message_twoline_white_right.tga")
            segmentBody = Image("./assets/graphics/gui/message_twoline_white_body.tga")
        }
        else {
            segmentLeft = Image("./assets/graphics/gui/message_black_left.tga")
            segmentRight = Image("./assets/graphics/gui/message_black_right.tga")
            segmentBody = Image("./assets/graphics/gui/message_black_body.tga")
        }
        uiFont = GameFontImpl()
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
        blendNormal()
        drawSegments(g)

        g.font = uiFont
        
        for (i in 0..Math.min(messagesList.size, MESSAGES_DISPLAY) - 1) {
            g.color = fontCol
            g.drawString(messagesList[i], (messageWindowRadius + 4).toFloat(), (messageWindowRadius + GLYPH_HEIGHT * i).toFloat())
        }

        blendNormal()
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {

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
