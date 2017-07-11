package net.torvald.terrarum.blockproperties

import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 16-02-16.
 */
class BlockProp {

    var id: Int = 0

    var nameKey: String = ""


    var shadeColR = 0f
    var shadeColG = 0f
    var shadeColB = 0f
    var shadeColA = 0f

    /**
     * @param opacity Raw RGB value, without alpha
     */
    inline val opacity: Color
        get() = Color(shadeColR, shadeColG, shadeColB, shadeColA)

    var strength: Int = 0
    var density: Int = 0
    var viscosity: Int = 0

    var isFluid: Boolean = false
    var isSolid: Boolean = false
    var isWallable: Boolean = false
    var isVertFriction: Boolean = false


    var lumColR = 0f
    var lumColG = 0f
    var lumColB = 0f
    var lumColA = 0f

    /**
     * @param luminosity Raw RGB value, without alpha
     */
    inline val luminosity: Color
        get() = BlockPropUtil.getDynamicLumFunc(Color(lumColR, lumColG, lumColB, lumColA), dynamicLuminosityFunction)

    var drop: Int = 0

    var isFallable: Boolean = false

    var friction: Int = 0

    var dynamicLuminosityFunction: Int = 0

    var material: String = ""
}