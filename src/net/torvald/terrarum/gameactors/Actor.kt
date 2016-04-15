package net.torvald.terrarum.gameactors

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-03-14.
 */
interface Actor {

    fun update(gc: GameContainer, delta_t: Int)

    /**
     * Valid RefID is equal to or greater than 32768.
     * @return Reference ID. (32768-0xFFFF_FFFF)
     */
    var referenceID: Int

    var actorValue: ActorValue
}