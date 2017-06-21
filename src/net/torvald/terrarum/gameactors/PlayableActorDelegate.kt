package net.torvald.terrarum.gameactors

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