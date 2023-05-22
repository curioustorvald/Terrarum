package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.savegame.toBigEndian
import net.torvald.terrarum.utils.PasswordBase32


typealias ActorID = Int

/**
 * @param renderOrder invisible/technical must use "Actor.RenderOrder.MIDDLE"
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class Actor : Comparable<Actor>, Runnable {

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: ActorID = 0 // in old time this was nullable without initialiser. If you're going to revert to that, add the reason why this should be nullable.

    /**
     * RenderOrder does not affect ReferenceID "too much" (ID generation will still depend on it, but it's just because of ye olde tradition by now)
     *
     * IngameRenderer will only look for RenderOrder and won't look for referenceID, so if you want to change the RenderOrder, just modify this field and not the referenceID.
     */
    var renderOrder = RenderOrder.MIDDLE

    protected constructor()

    // needs zero-arg constructor for serialiser to work
    constructor(renderOrder: RenderOrder, id: ActorID?) : this() {
        referenceID = id ?: Terrarum.generateUniqueReferenceID(renderOrder)
    }


    enum class RenderOrder {
        BEHIND, // tapestries, some particles (obstructed by terrain)
        MIDDLE, // actors
        MIDTOP, // bullets, thrown items
        FRONT,  // fake tiles
        OVERLAY // screen overlay, not affected by lightmap
    }

    companion object {
        val RANGE_BEHIND = ReferencingRanges.ACTORS_BEHIND  // 1
        val RANGE_MIDDLE = ReferencingRanges.ACTORS_MIDDLE  // 3
        val RANGE_MIDTOP = ReferencingRanges.ACTORS_MIDTOP  // 1
        val RANGE_FRONT  = ReferencingRanges.ACTORS_FRONT   // 0.9375
        val RANGE_OVERLAY= ReferencingRanges.ACTORS_OVERLAY // 0.9375
    }

    abstract fun update(delta: Float)

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

    /* called when the actor is loaded from the save; one use of this function is to "re-sync" the
     * Transient variables such as `mainUI` of FixtureBase
     */
    open fun reload() {
        actorValue.actor = this

        if (this is Pocketed)
            inventory.actor = this
    }

    /**
     * ActorValue change event handler
     *
     * @param value null if the key is deleted
     */
    abstract @Event fun onActorValueChange(key: String, value: Any?)

    abstract fun dispose()

    @Transient val localHash = HQRNG().nextInt()
    @Transient val localHashStr = PasswordBase32.encode(localHash.toBigEndian()).substringBefore('=')
}

annotation class Event
