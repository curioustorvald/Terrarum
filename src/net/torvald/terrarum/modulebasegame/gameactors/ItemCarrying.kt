package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2016-04-26.
 */
class ItemCarrying : ActorWithBody, Luminous {

    var itemID: ItemID = ""; private set

    private constructor()

    constructor(itemID: ItemID) : super(RenderOrder.MIDTOP, PhysProperties.IMMOBILE) {
        this.itemID = itemID
    }


    // just let the solver use AABB; it's cheap but works just enough

    /**
     * Recommended implementation:
     *
    override var color: Int
    get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
    set(value) {
    actorValue[AVKey.LUMINOSITY] = value
    }
     */
    private var color: Cvec
        get() = throw UnsupportedOperationException()
        set(value) {
        }
    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList: List<Lightbox>
        get() = throw UnsupportedOperationException()
    override val shadeBoxList: List<Lightbox>
        get() = throw UnsupportedOperationException()

    init {

    }
}