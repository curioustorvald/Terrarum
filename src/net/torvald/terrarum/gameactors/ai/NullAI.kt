package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.gameactors.Actor

/**
 * Created by minjaesong on 2018-06-07.
 */
class NullAI : ActorAI {
    override fun update(actor: Actor, delta: Float) {
        // null AI does nothing
    }
}