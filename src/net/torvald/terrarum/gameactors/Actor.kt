package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex
import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 15-12-31.
 */
abstract class Actor : Comparable<Actor>, Runnable {

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
    override fun toString() = "Actor, " +
            if (actorValue.getAsString(AVKey.NAME).isNullOrEmpty())
                "ID: ${hashCode()}"
            else
                "ID: ${hashCode()} (${actorValue.getAsString(AVKey.NAME)})"
    override fun compareTo(other: Actor): Int = (this.referenceID - other.referenceID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    /**
     * Usage:
     *
     * override var referenceID: Int = generateUniqueReferenceID()
     */
    fun generateUniqueReferenceID(): Int {
        var ret: Int
        do {
            ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
        } while (Terrarum.ingame.hasActor(ret) || ret < ItemCodex.ITEM_COUNT_MAX) // check for collision
        return ret
    }
}