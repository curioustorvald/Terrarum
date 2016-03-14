package com.Torvald.Terrarum.Actors

import com.Torvald.Terrarum.Actors.AI.ActorAI

/**
 * Created by minjaesong on 16-03-14.
 */
interface AIControlled {
    fun attachAI(ai: ActorAI)
}