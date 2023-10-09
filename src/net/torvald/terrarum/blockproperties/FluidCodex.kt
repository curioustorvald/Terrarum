package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-10-09.
 */
class FluidCodex {

    @Transient val blockProps = HashMap<ItemID, FluidProp>()

    @Transient private val nullProp = FluidProp()


    operator fun get(fluidID: ItemID?): FluidProp {
        if (fluidID == null || fluidID == Fluid.NULL) {
            return nullProp
        }

        try {
            return if (fluidID.startsWith("fluid@"))
                blockProps[fluidID.substring(6)]!!
            else
                blockProps[fluidID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Fluidprop with id $fluidID does not exist.")
        }
    }

}