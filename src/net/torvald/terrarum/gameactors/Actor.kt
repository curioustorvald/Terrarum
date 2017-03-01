package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.ActorValue
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex
import org.newdawn.slick.GameContainer

/**
 * @param renderOrder invisible/technical -> ActorOrder.MIDDLE
 *
 * Created by minjaesong on 15-12-31.
 */
abstract class Actor(val renderOrder: ActorOrder) : Comparable<Actor>, Runnable {

    abstract fun update(gc: GameContainer, delta: Int)

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: Int = generateUniqueReferenceID()
    abstract var actorValue: ActorValue
    abstract var flagDespawn: Boolean

    override fun equals(other: Any?) = referenceID == (other as Actor).referenceID
    override fun hashCode() = referenceID
    override fun toString() =
            if (actorValue.getAsString(AVKey.NAME).isNullOrEmpty())
                "${hashCode()}"
            else
                "${hashCode()} (${actorValue.getAsString(AVKey.NAME)})"
    override fun compareTo(other: Actor): Int = (this.referenceID - other.referenceID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    /**
     * Usage:
     *
     * override var referenceID: Int = generateUniqueReferenceID()
     */
    fun generateUniqueReferenceID(): Int {
        fun checkForCollision(value: Int) =
                Terrarum.ingame.theGameHasActor(value) ||
                value < ItemCodex.ITEM_COUNT_MAX ||
                value < when (renderOrder) {
                    ActorOrder.BEHIND -> ItemCodex.ITEM_COUNT_MAX
                    ActorOrder.MIDDLE -> 0x10000000
                    ActorOrder.MIDTOP -> 0x60000000
                    ActorOrder.FRONT  -> 0x70000000
                } ||
                value > when (renderOrder) {
                    ActorOrder.BEHIND -> 0x0FFFFFFF
                    ActorOrder.MIDDLE -> 0x5FFFFFFF
                    ActorOrder.MIDTOP -> 0x6FFFFFFF
                    ActorOrder.FRONT  -> 0x7FFFFFFF
                }

        var ret: Int
        do {
            ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
        } while (checkForCollision(ret)) // check for collision
        return ret
    }

}

enum class ActorOrder {
    BEHIND, // tapestries, some particles (obstructed by terrain)
    MIDDLE, // actors
    MIDTOP, // bullets, thrown items
    FRONT   // fake tiles
}