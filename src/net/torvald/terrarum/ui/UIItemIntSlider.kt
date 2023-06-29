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



    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}