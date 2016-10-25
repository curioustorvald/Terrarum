package net.torvald.terrarum.gameactors

/**
 * Note: AI-controlled actor must be 'Controllable'
 *
 * Created by minjaesong on 16-01-31.
 */
interface AIControlled {
    val scriptPath: String

    fun moveLeft()
    fun moveRight()
    fun moveUp()
    fun moveDown()
    fun moveJump()

    /** fly toward arbitrary angle  WARNING: the map is looped! */
    fun moveTo(bearing: Double)
    /** fly toward arbitrary coord  WARNING: the map is looped! */
    fun moveTo(toX: Double, toY: Double)
}