package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.gameactors.HumanoidNPC

/**
 * Created by minjaesong on 16-03-02.
 */
interface ActorAI {
    fun update(actor: HumanoidNPC, delta: Float)
}