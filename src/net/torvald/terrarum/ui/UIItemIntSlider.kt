package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.GdxColorMap
import java.awt.Color

/**
 * Created by minjaesong on 2018-10-02.
 */
class UIItemIntSlider(
        parent: UICanvas,
        initValue: Int,
        initialX: Int,
        initialY: Int,

        override val width: Int,
        override val height: Int,

        val minValue: Int,
        val maxValue: Int,
        val step: Int,

        // BASIC OPTIONS //

        /** Show prev- and next values (if any) */
        var showNotches: Boolean,
        var showMinMaxValues: Boolean,
        val isVertical: Boolean,
        val meterStyle: Int,

        val sliderCol: Color,
        val sliderBlend: BlendMode,

        val notchCol: Color,
        val barCol: Color,
        val barAndNotchBlend: BlendMode,

        // EXTENDED OPTIONS //

        val sliderUseColourMap: GdxColorMap? = null,
        val sliderUseTexture: Texture? = null
) : UIItem(parent, initialX, initialY) {

    constructor(
            parent: UICanvas,
            initValue: Int,
            initialX: Int,
            initialY: Int,
            values: IntRange,
            width: Int,
            height: Int,
            showNotches: Boolean,
            showMinMaxValues: Boolean,
            isVertical: Boolean,
            meterStyle: Int,

            sliderCol: Color,
            sliderBlend: BlendMode,

            notchCol: Color,
            barCol: Color,
            barAndNotchBlend: BlendMode
    ) : this(
            parent,
            initValue,
            initialX,
            initialY,
            values.first,
            values.last,
            values.step,
            width, height, showNotches, showMinMaxValues, isVertical, meterStyle, sliderCol, sliderBlend, notchCol, barCol, barAndNotchBlend
    )


    var value = initValue


    init {
        if (sliderUseColourMap != null && sliderUseTexture != null) {
            throw IllegalArgumentException("Can't use colour map and texture at the same time -- ColorMap: $sliderUseColourMap, Texture: $sliderUseTexture")
        }
    }


    // TODO unimplemented


    override val mouseUp: Boolean
        get() = super.mouseUp
    override val mousePushed: Boolean
        get() = super.mousePushed
    override val mouseOverCall: UICanvas?
        get() = super.mouseOverCall
    override var updateListener: ((Float) -> Unit)?
        get() = super.updateListener
        set(_) {}
    override var keyDownListener: ((Int) -> Unit)?
        get() = super.keyDownListener
        set(_) {}
    override var keyUpListener: ((Int) -> Unit)?
        get() = super.keyUpListener
        set(_) {}
    override var touchDraggedListener: ((Int, Int, Int) -> Unit)?
        get() = super.touchDraggedListener
        set(_) {}
    override var touchDownListener: ((Int, Int, Int, Int) -> Unit)?
        get() = super.touchDownListener
        set(_) {}
    override var touchUpListener: ((Int, Int, Int, Int) -> Unit)?
        get() = super.touchUpListener
        set(_) {}
    override var scrolledListener: ((Float, Float) -> Unit)?
        get() = super.scrolledListener
        set(_) {}
    override var clickOnceListener: ((Int, Int, Int) -> Unit)?
        get() = super.clickOnceListener
        set(_) {}
    override var clickOnceListenerFired: Boolean
        get() = super.clickOnceListenerFired
        set(_) {}
    override var controllerInFocus: Boolean
        get() = super.controllerInFocus
        set(_) {}

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}