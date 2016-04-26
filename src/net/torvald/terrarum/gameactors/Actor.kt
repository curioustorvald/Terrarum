package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-03-14.
 */
abstract class Actor : Comparable<Actor>, Runnable {

    abstract protected fun update(gc: GameContainer, delta_t: Int) // use start() for multithreaded env

    protected var thread: Thread? = null

    fun start() {
        thread = Thread(this, "ID: $referenceID")
        thread!!.run()
    }

    /**
     * Valid RefID is equal to or greater than 32768.
     * @return Reference ID. (32768-0xFFFF_FFFF)
     */
    abstract var referenceID: Int

    abstract var actorValue: ActorValue

    override fun equals(other: Any?) = referenceID == (other as Actor).referenceID
    override fun hashCode() = referenceID
    override fun toString() = if (actorValue.getAsString(AVKey.NAME).isNullOrEmpty())
        "ID: ${hashCode()}"
    else
        "ID: ${hashCode()} (${actorValue.getAsString(AVKey.NAME)})"
    override fun compareTo(other: Actor): Int = this.referenceID - other.referenceID

    /**
     * Usage:
     *
     * override var referenceID: Int = generateUniqueReferenceID()
     */
    fun generateUniqueReferenceID(): Int {
        fun Int.abs() = if (this < 0) -this else this
        var ret: Int
        do {
            ret = HQRNG().nextInt().abs() // set new ID
        } while (Terrarum.game.hasActor(ret)) // check for collision
        return ret
    }
}