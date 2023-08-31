package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.ui.Toolkit
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
        val keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
        val touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
        open var extraInfo: Any?,
        open protected val highlightEquippedItem: Boolean = true // for some UIs that only cares about getting equipped slot number but not highlighting
) : UIItem(parentUI, initialX, initialY) {
    abstract override fun update(delta: Float)
    abstract override fun render(batch: SpriteBatch, camera: OrthographicCamera)

    /** Custom highlight rule to highlight tihs button to primary accent colour (blue by default).
     * Set to `null` to use default rule:
     *
     * "`equippedSlot` defined and set to `highlightEquippedItem`" or "`forceHighlighted`" */
    var customHighlightRuleMain: ((UIItemInventoryCellBase) -> Boolean)? = null
    /** Custom highlight rule to highlight this button to secondary accent colour (yellow by default). Set to `null` to use default rule (which does nothing). */
    var customHighlightRule2: ((UIItemInventoryCellBase) -> Boolean)? = null

    var forceHighlighted = false
        /*set(value) {
            if (field != value) {
                printdbg(this, "forceHighlighted: ${field} -> ${value}; ${App.GLOBAL_RENDER_TIMER}")
                printStackTrace(this)
            }
            field = value
        }*/

    override fun keyDown(keycode: Int): Boolean {
        keyDownFun(item, amount, keycode, extraInfo, this)
        super.keyDown(keycode)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touchDownFun(item, amount, button, extraInfo, this)
        super.touchDown(screenX, screenY, pointer, button)
        return true
    }
}

object UIItemInventoryCellCommonRes {
    val meterColourMap = GdxColorMap(Gdx.files.internal("./assets/clut/health_bar_colouring_4096.tga"))
    val meterBackDarkening = Color(0x666666ff.toInt())

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

    val defaultInventoryCellTheme = InventoryCellColourTheme(
            Toolkit.Theme.COL_SELECTED,
            Toolkit.Theme.COL_LIST_DEFAULT,
            Toolkit.Theme.COL_MOUSE_UP,
            Toolkit.Theme.COL_INVENTORY_CELL_BORDER,
            Toolkit.Theme.COL_SELECTED,
            Color.WHITE,
            Toolkit.Theme.COL_MOUSE_UP,
            Color.WHITE,
    )
}

data class InventoryCellColourTheme(
        val cellHighlightMainCol: Color,
        val cellHighlightSubCol: Color,
        val cellHighlightMouseUpCol: Color,
        val cellHighlightNormalCol: Color,
        val textHighlightMainCol: Color,
        val textHighlightSubCol: Color,
        val textHighlightMouseUpCol: Color,
        val textHighlightNormalCol: Color,
)