package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex
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
        FRONT   // fake tiles
    }

    companion object {
        val RANGE_BEHIND = ACTORID_MIN..0x1FFF_FFFF
        val RANGE_MIDDLE = 0x2000_0000..0x5FFF_FFFF
        val RANGE_MIDTOP = 0x6000_0000..0x6FFF_FFFF
        val RANGE_FRONT  = 0x7000_0000..0x7FFF_FFFF
    }

    abstract fun update(delta: Float)

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: ActorID? = null
    var actorValue = ActorValue(this)
    @Volatile var flagDespawn = false

    override fun equals(other: Any?) = referenceID == (other as Actor).referenceID
    override fun hashCode() = referenceID!!
    override fun toString() =
            if (actorValue.getAsString("name").isNullOrEmpty())
                "${hashCode()}"
            else
                "${hashCode()} (${actorValue.getAsString("name")})"
    override fun compareTo(other: Actor): Int = (this.referenceID!! - other.referenceID!!).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0


    /**
     * ActorValue change event handler
     *
     * @param value null if the key is deleted
     */
    abstract @Event fun onActorValueChange(key: String, value: Any?)

}

annotation class Event
