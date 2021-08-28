package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ai.ActorAI

/**
 * Note: AI-controlled actor must be 'Controllable'
 *
 * Created by minjaesong on 2016-01-31.
 */
interface AIControlled {
    var ai: ActorAI

    fun moveLeft(amount: Float = 1f)
    fun moveRight(amount: Float = 1f)
    fun moveUp(amount: Float = 1f)
    fun moveDown(amount: Float = 1f)
    fun moveJump(amount: Float = 1f)

    /** fly toward arbitrary angle  WARNING: the map is looped! */
    fun moveTo(bearing: Double)
    /** fly toward arbitrary coord  WARNING: the map is looped! */
    fun moveTo(toX: Double, toY: Double)
}