package net.torvald.terrarum.gameactors

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-03-14.
 */
abstract class Actor : Comparable<Actor> {

    abstract fun update(gc: GameContainer, delta_t: Int)

    /**
     * Valid RefID is equal to or greater than 32768.
     * @return Reference ID. (32768-0xFFFF_FFFF)
     */
    abstract var referenceID: Int

    abstract var actorValue: ActorValue

    override fun equals(other: Any?) = referenceID == (other as Actor).referenceID
    override fun hashCode() = referenceID
    override fun toString() = "ID: ${hashCode()}"
    override fun compareTo(other: Actor): Int = this.referenceID - other.referenceID
}