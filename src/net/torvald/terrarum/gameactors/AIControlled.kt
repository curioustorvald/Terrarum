package net.torvald.terrarum.gameactors

/**
 * Note: AI-controlled actor must be 'Controllable'
 *
 * Created by minjaesong on 16-01-31.
 */
interface AIControlled {
    val scriptPath: String

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