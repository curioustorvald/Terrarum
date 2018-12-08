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
) : UIItem(parent) {

    constructor(
            parent: UICanvas,
            initValue: Int,
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


    override var posX: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var posY: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override val mouseUp: Boolean
        get() = super.mouseUp
    override val mousePushed: Boolean
        get() = super.mousePushed
    override val mouseOverCall: UICanvas?
        get() = super.mouseOverCall
    override var updateListener: ((Float) -> Unit)?
        get() = super.updateListener
        set(value) {}
    override var keyDownListener: ((Int) -> Unit)?
        get() = super.keyDownListener
        set(value) {}
    override var keyUpListener: ((Int) -> Unit)?
        get() = super.keyUpListener
        set(value) {}
    override var mouseMovedListener: ((Int, Int) -> Unit)?
        get() = super.mouseMovedListener
        set(value) {}
    override var touchDraggedListener: ((Int, Int, Int) -> Unit)?
        get() = super.touchDraggedListener
        set(value) {}
    override var touchDownListener: ((Int, Int, Int, Int) -> Unit)?
        get() = super.touchDownListener
        set(value) {}
    override var touchUpListener: ((Int, Int, Int, Int) -> Unit)?
        get() = super.touchUpListener
        set(value) {}
    override var scrolledListener: ((Int) -> Unit)?
        get() = super.scrolledListener
        set(value) {}
    override var clickOnceListener: ((Int, Int, Int) -> Unit)?
        get() = super.clickOnceListener
        set(value) {}
    override var clickOnceListenerFired: Boolean
        get() = super.clickOnceListenerFired
        set(value) {}
    override var controllerInFocus: Boolean
        get() = super.controllerInFocus
        set(value) {}

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}