package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.ActorValue
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex
import org.newdawn.slick.GameContainer

typealias ActorID = Int

/**
 * @param renderOrder invisible/technical must use "Actor.RenderOrder.MIDDLE"
 *
 * Created by minjaesong on 15-12-31.
 */
abstract class Actor(val renderOrder: RenderOrder) : Comparable<Actor>, Runnable {

    enum class RenderOrder {
        BEHIND, // tapestries, some particles (obstructed by terrain)
        MIDDLE, // actors
        MIDTOP, // bullets, thrown items
        FRONT   // fake tiles
    }

    abstract fun update(gc: GameContainer, delta: Int)

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: ActorID = generateUniqueReferenceID()
    var actorValue = ActorValue()
    @Volatile var flagDespawn = false

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
    fun generateUniqueReferenceID(): ActorID {
        fun hasCollision(value: ActorID) =
                try {
                    Terrarum.ingame!!.theGameHasActor(value) ||
                    value < ItemCodex.ACTOR_ID_MIN ||
                    value < when (renderOrder) {
                        RenderOrder.BEHIND -> ItemCodex.ACTOR_ID_MIN
                        RenderOrder.MIDDLE -> 0x10000000
                        RenderOrder.MIDTOP -> 0x60000000
                        RenderOrder.FRONT  -> 0x70000000
                    } ||
                    value > when (renderOrder) {
                        RenderOrder.BEHIND -> 0x0FFFFFFF
                        RenderOrder.MIDDLE -> 0x5FFFFFFF
                        RenderOrder.MIDTOP -> 0x6FFFFFFF
                        RenderOrder.FRONT  -> 0x7FFFFFFF
                    }
                }
                catch (gameNotInitialisedException: KotlinNullPointerException) {
                    false
                }

        var ret: Int
        do {
            ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
        } while (hasCollision(ret)) // check for collision
        return ret
    }

}