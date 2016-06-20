package net.torvald.terrarum.gameactors

import net.torvald.spriteanimation.SpriteAnimation

/**
 * Created by minjaesong on 16-06-17.
 */
open class FixturesBase : ActorWithBody() {
    /** Binary flags. Indicates that other actor (player) can pass in the direction.
     * - (0: No collision)
     * - 1: Top
     * - 2: Right
     * - 4: Bottom
     * - 8: Left
     * For example, flag of 4 is good for tables; player can stand on, which means
     * downward movement is blocked within the fixtures' AABB.
     */
    var collisionFlag: Int = 0

    /**
     * Normally if player is standing on the fixtures (with flag 4), pressing DOWN wiil allow
     * player to get down. Setting this flag TRUE will block such movement (player cannot get down)
     */
    var cannotPassThru = false

}