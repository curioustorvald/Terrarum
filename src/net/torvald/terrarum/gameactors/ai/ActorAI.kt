package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.gameactors.Actor


/**
 * Created by minjaesong on 2016-03-02.
 */
interface ActorAI {
    fun update(actor: Actor, delta: Float)
}