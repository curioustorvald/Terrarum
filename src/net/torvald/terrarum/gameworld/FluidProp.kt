package net.torvald.terrarum.gameworld

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.BlockPropUtil

/**
 * Created by minjaesong on 2018-12-29.
 */
class FluidProp {

    var density: Int = 0

    var dynamicLuminosityFunction: Int = 0

    /** 1.0f for 1023, 0.25f for 255 */
    var shadeColR = 0f
    var shadeColG = 0f
    var shadeColB = 0f
    var shadeColA = 0f

    /** 1.0f for 1023, 0.25f for 255 */
    var lumColR = 0f
    var lumColG = 0f
    var lumColB = 0f
    var lumColA = 0f

    inline val opacity: Color
        get() = Color(shadeColR, shadeColG, shadeColB, shadeColA)

    inline val luminosity: Color
        get() = BlockPropUtil.getDynamicLumFunc(Color(lumColR, lumColG, lumColB, lumColA), dynamicLuminosityFunction)


    companion object {
        fun getNullProp(): FluidProp {
            val p = FluidProp()

            p.shadeColR = BlockCodex[Block.AIR].shadeColR
            p.shadeColG = BlockCodex[Block.AIR].shadeColG
            p.shadeColB = BlockCodex[Block.AIR].shadeColB
            p.shadeColA = BlockCodex[Block.AIR].shadeColA

            return p
        }
    }
}