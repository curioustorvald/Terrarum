package net.torvald.terrarum.blockproperties

import net.torvald.gdx.graphics.Cvec

/**
 * Created by minjaesong on 2016-02-16.
 */
class BlockProp {

    var id: Int = 0

    var nameKey: String = ""

    /** 1.0f for 1023, 0.25f for 255 */
    var shadeColR = 0f
    var shadeColG = 0f
    var shadeColB = 0f
    var shadeColA = 0f

    lateinit var opacity: Cvec

    fun getOpacity(channel: Int) = when (channel) {
        0 -> shadeColR
        1 -> shadeColG
        2 -> shadeColB
        3 -> shadeColA
        else -> throw IllegalArgumentException("Invalid channel $channel")
    }

    var strength: Int = 0
    var density: Int = 0
    var viscosity: Int = 0
    var colour: Int = 0

    /** isSolid is NOT SAME AS !isOpaqueis
     * Like, don't ever use this vars to tell this block should be removed by water or something,
     * because PLANTS ARE ACTORS, TREES ARE BLOCKS, stupid myself!
     */
    var isSolid: Boolean = false
    //var isClear: Boolean = false
    var isPlatform: Boolean = false
    var isWallable: Boolean = false
    var isVertFriction: Boolean = false


    /** 1.0f for 1023, 0.25f for 255 */
    var lumColR = 0f
    var lumColG = 0f
    var lumColB = 0f
    var lumColA = 0f
    lateinit var internalLumCol: Cvec

    /**
     * @param luminosity
     */
    inline val luminosity: Cvec
        get() = BlockPropUtil.getDynamicLumFunc(internalLumCol, dynamicLuminosityFunction)

    fun getLum(channel: Int) = BlockPropUtil.getDynamicLumFuncByChan(
            when (channel) {
                0 -> lumColR
                1 -> lumColG
                2 -> lumColB
                3 -> lumColA
                else -> throw IllegalArgumentException("Invalid channel $channel")
            }, dynamicLuminosityFunction, channel
    )

    var drop: Int = 0

    var maxSupport: Int = -1 // couldn't use NULL at all...

    var friction: Int = 0

    var dynamicLuminosityFunction: Int = 0

    var material: String = ""
}