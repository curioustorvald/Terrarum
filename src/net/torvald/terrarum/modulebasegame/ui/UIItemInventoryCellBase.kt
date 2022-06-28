package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Cross section of two inventory cell types
 *
 * Created by minjaesong on 2017-10-22.
 */
abstract class UIItemInventoryCellBase(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        open var item: GameItem?,
        open var amount: Long,
        open var itemImage: TextureRegion?,
        open var quickslot: Int? = null,
        open var equippedSlot: Int? = null,
        val keyDownFun: (GameItem?, Long, Int, Any?) -> Unit, // Item, Amount, Keycode, extra info
        val touchDownFun: (GameItem?, Long, Int, Any?) -> Unit, // Item, Amount, Button, extra info
        open var extraInfo: Any?
) : UIItem(parentUI, initialX, initialY) {
    abstract override fun update(delta: Float)
    abstract override fun render(batch: SpriteBatch, camera: Camera)

    override fun keyDown(keycode: Int): Boolean {
        keyDownFun(item, amount, keycode, extraInfo)
        super.keyDown(keycode)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touchDownFun(item, amount, button, extraInfo)
        super.touchDown(screenX, screenY, pointer, button)
        return true
    }
}

object UIItemInventoryCellCommonRes {
    val meterColourMap = GdxColorMap(Gdx.files.internal("./assets/clut/health_bar_colouring_4096.tga"))
    val meterBackDarkening = Color(0x828282ff.toInt())

    fun getHealthMeterColour(value: Float, start: Float, end: Float): Color {
        if (start > end) throw IllegalArgumentException("Start value is greater than end value: $start..$end")

        return if (value <= start)
            meterColourMap[0]
        else if (value >= end)
            meterColourMap[meterColourMap.width - 1]
        else {
            val scale = (value - start) / (end - start)
            meterColourMap[scale.times(meterColourMap.width - 1).roundToInt()]
        }
    }
    
    fun getHealthMeterColour(value: Int, start: Int, end: Int) = getHealthMeterColour(value.toFloat(), start.toFloat(), end.toFloat())

    fun Long.toItemCountText() = (if (this < 0) "-" else "") + when (this.absoluteValue) {
        in 0..999999 -> "$this"
        in 1_000_000..999_999_999 -> "${this / 1_000_000}.${this.rem(1_000_000) / 10_000}M"
        else -> "${this / 1_000_000_000}.${this.rem(1_000_000_000) / 10_000_000}B"
        
        // 1 000 000 000
        // 2 147 483 647
        
        // -2.14B
    }
}