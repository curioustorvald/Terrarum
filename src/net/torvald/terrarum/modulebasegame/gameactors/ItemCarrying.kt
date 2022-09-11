package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2016-04-26.
 */
class ItemCarrying : ActorWithBody {

    var itemID: ItemID = ""; private set

    private constructor()

    constructor(itemID: ItemID) : super(RenderOrder.MIDTOP, PhysProperties.IMMOBILE) {
        this.itemID = itemID
    }


    // just let the solver use AABB; it's cheap but works just enough

    init {

    }
}