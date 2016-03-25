package com.torvald.terrarum.gameactors

import com.torvald.terrarum.gameactors.ai.ActorAI

/**
 * Created by minjaesong on 16-03-14.
 */
interface AIControlled {
    fun attachAI(ai: ActorAI)
}