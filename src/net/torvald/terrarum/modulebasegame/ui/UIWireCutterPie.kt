package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import org.dyn4j.geometry.Vector2
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2024-03-14.
 */
class UIWireCutterPie : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

    private val cellSize = ItemSlotImageFactory.slotImage.tileW

    private val slotCount = 6

    private val slotDistanceFromCentre: Double
        get() = cellSize * 2.666 * handler.scale
    override var width: Int = cellSize * 7
    override var height: Int = width


    /**
     * In milliseconds
     */
    override var openCloseTime: Second = UIQuickslotBar.COMMON_OPEN_CLOSE

    private val smallenSize = 0.92f

    var selection: Int = -1

    override fun updateImpl(delta: Float) {
        if (selection >= 0 && (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying != null)
            (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying!!.actorValue[AVKey.__PLAYER_WIRECUTTERSEL] =
                selection % slotCount


        // update controls
        if (handler.isOpened || handler.isOpening) {
            val cursorPos = Vector2(Terrarum.mouseScreenX.toDouble(), Terrarum.mouseScreenY.toDouble())
            val centre = Vector2(Toolkit.hdrawWidth.toDouble(), App.scr.halfh.toDouble())
            val deg = -(centre - cursorPos).direction.toFloat()

            selection = Math.round(deg * slotCount / FastMath.TWO_PI)
            if (selection < 0) selection += slotCount

            // TODO add gamepad support
        }
    }

    private val drawColor = Color(1f, 1f, 1f, 1f)

    private fun getSprite(index: Int): TextureRegion {
        val (x, y) = when (index) {
            0 -> 1 to 3
            1 -> 11 to 2
            2 -> 12 to 2
            3 -> 13 to 2
            4 -> 14 to 2
            5 -> 15 to 2
            else -> throw IllegalArgumentException()
        }
        return CommonResourcePool.getAsItemSheet("basegame.items").get(x, y)
    }

    companion object {
        fun getWireItemID(index: Int): String {
            return when (index) {
                0 -> "__all__"
                1 -> "wire@basegame:8192"
                2 -> "wire@basegame:8193"
                3 -> "wire@basegame:8194"
                4 -> "wire@basegame:8195"
                5 -> "wire@basegame:8196"
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // draw radial thingies
        for (i in 0 until slotCount) {
            val sprite = getSprite(i)

            // set position
            val angle = Math.PI * 2.0 * (i.toDouble() / slotCount) + Math.PI // 180 deg monitor-wise
            val slotCentrePoint = Vector2(0.0, slotDistanceFromCentre).setDirection(-angle) // NOTE: NOT a center of circle!

            // draw cells
            val image = if (i == selection)
                ItemSlotImageFactory.produceLarge(false, null, sprite, false)
            else
                ItemSlotImageFactory.produce(true, null, sprite)

            val slotX = slotCentrePoint.x.toInt()
            val slotY = slotCentrePoint.y.toInt()

            drawColor.a = UIQuickslotBar.DISPLAY_OPACITY
            batch.color = drawColor
            image.draw(batch, slotX, slotY)
        }
    }

    override fun doOpening(delta: Float) {
        doOpeningFade(this, openCloseTime)
        handler.scale = smallenSize + (1f.minus(smallenSize) * handler.opacity)
    }

    override fun doClosing(delta: Float) {
        doClosingFade(this, openCloseTime)
        handler.scale = smallenSize + (1f.minus(smallenSize) * handler.opacity)
    }

    override fun endOpening(delta: Float) {
        endOpeningFade(this)
        handler.scale = 1f
    }

    override fun endClosing(delta: Float) {
        endClosingFade(this)
        handler.scale = 1f
    }

    override fun dispose() {
    }

}