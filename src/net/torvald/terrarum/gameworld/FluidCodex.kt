package net.torvald.terrarum.gameworld

import net.torvald.terrarum.blockproperties.Fluid

/**
 * Created by minjaesong on 2016-08-06.
 */
object FluidCodex {

    operator fun get(type: FluidType): FluidProp {
        return if (type sameAs Fluid.NULL)
            nullProp
        else
            waterProp
    }

    // TODO temporary, should read from CSV
    val nullProp = FluidProp.getNullProp()
    val waterProp = FluidProp()

    init {
        waterProp.shadeColR = 0.1016f
        waterProp.shadeColG = 0.0744f
        waterProp.shadeColB = 0.0508f
        waterProp.shadeColA = 0.0508f

        waterProp.density = 1000
    }
}
