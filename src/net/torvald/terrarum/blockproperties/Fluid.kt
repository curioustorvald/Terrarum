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


    fun getFluidTileFrom(type: FluidType) = GameWorld.TILES_SUPPORTED - type.abs()
    private val fluidTilesRange = 4094..4095
    fun isThisTileFluid(tileid: Int) = tileid in fluidTilesRange
}