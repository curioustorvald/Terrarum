package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-08-06.
 */
object Fluid {

    val NULL = FluidType(0)

    val WATER = FluidType(1)
    val STATIC_WATER = FluidType(-1)

    val LAVA = FluidType(2)
    val STATIC_LAVA = FluidType(-2)

    val fluidTilesRange = 4094..4095 // MANUAL UPDATE
    val fluidRange = 1..2 // MANUAL UPDATE


    fun getFluidTileFrom(type: FluidType) = GameWorld.TILES_SUPPORTED - type.abs()
    fun isThisTileFluid(tileid: Int) = tileid in fluidTilesRange
}