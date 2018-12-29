package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.gameworld.FluidType

/**
 * Created by minjaesong on 2016-08-06.
 */
object Fluid {

    val NULL = FluidType(0)

    val WATER = FluidType(1)
    val STATIC_WATER = FluidType(-1)

    val LAVA = FluidType(2)
    val STATIC_LAVA = FluidType(-2)


    val fluidRange = 1..2 // TODO MANUAL UPDATE
}