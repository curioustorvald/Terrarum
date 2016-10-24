package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ai.ActorAI

/**
 * Note: AI-controlled actor must be 'Controllable'
 *
 * Created by minjaesong on 16-03-14.
 */
interface AIControlled {
    val scriptPath: String
}