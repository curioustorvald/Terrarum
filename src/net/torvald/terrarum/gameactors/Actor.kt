package net.torvald.terrarum.gameactors

import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex.ACTORID_MIN


typealias ActorID = Int

/**
 * @param renderOrder invisible/technical must use "Actor.RenderOrder.MIDDLE"
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class Actor(val renderOrder: RenderOrder) : Comparable<Actor>, Runnable {

    enum class RenderOrder {
        BEHIND, // tapestries, some particles (obstructed by terrain)
        MIDDLE, // actors
        MIDTOP, // bullets, thrown items
        FRONT,  // fake tiles
        OVERLAY, // screen overlay, not affected by lightmap
        WIRES
    }

    companion object {
        val RANGE_BEHIND = ReferencingRanges.ACTORS_BEHIND  // 1
        val RANGE_MIDDLE = ReferencingRanges.ACTORS_MIDDLE  // 3
        val RANGE_MIDTOP = ReferencingRanges.ACTORS_MIDTOP  // 1
        val RANGE_FRONT  = ReferencingRanges.ACTORS_FRONT   // 1
        val RANGE_OVERLAY= ReferencingRanges.ACTORS_OVERLAY // 0.9375
        val RANGE_WIRES = ReferencingRanges.ACTORS_WIRES // 0.0002
    }

    abstract fun update(delta: Float)

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: ActorID = Terrarum.generateUniqueReferenceID(renderOrder) // once this was nullable without initialiser. If you're going to revert to that, add the reason why this should be nullable.
    var actorValue = ActorValue(this) // FIXME cyclic reference on GSON
    @Volatile var flagDespawn = false

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        return referenceID == (other as Actor).referenceID
    }
    override fun hashCode() = referenceID
    override fun toString() =
            if (actorValue.getAsString("name").isNullOrEmpty())
                "${hashCode()}"
            else
                "${hashCode()} (${actorValue.getAsString("name")})"
    override fun compareTo(other: Actor): Int = (this.referenceID - other.referenceID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0


    /**
     * ActorValue change event handler
     *
     * @param value null if the key is deleted
     */
    abstract @Event fun onActorValueChange(key: String, value: Any?)

    abstract fun dispose()

}

annotation class Event
