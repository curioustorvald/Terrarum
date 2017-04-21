package net.torvald.terrarum.gameactors

import net.torvald.terrarum.AmmoMeterProxy
import net.torvald.terrarum.gameactors.ActorHumanoid
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input

/**
 * A wrapper to support instant player changing (or possessing other NPCs maybe)
 *
 * @param actor : here you 'attach' the actor you wish to control
 * Created by minjaesong on 16-10-23.
 */
class PlayableActorDelegate(val actor: ActorHumanoid) {

    init {
        if (actor !is Controllable)
            throw IllegalArgumentException("Player must be 'Controllable'!")
    }

}