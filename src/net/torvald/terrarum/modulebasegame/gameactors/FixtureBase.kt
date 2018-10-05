package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase(physics: Boolean = true) :
        ActorWBMovable(RenderOrder.BEHIND, immobileBody = true, usePhysics = physics) {
    /**
     * 0: Open
     * 1: Blocked
     * 2: Platform; can be stood on, press DOWN to go down. Also allows other blocks can be places on top of it (e.g. torch)
     * 3: Wall_left; blocks rightward movement
     * 4: Wall_right: blocks leftward movement
     * 5: Same as 2 but player CANNOT go down
     * For example, flag of 4 is good for tables; player can stand on, which means
     * downward movement is blocked within the fixtures' AABB.
     */
    var collisionFlag: Int = 0

    companion object {
        val COLLISION_OPEN = 0
        val COLLISION_BLOCKED = 1
        val COLLISION_PLATFORM = 2
        val COLLISION_WALL_LEFT = 3
        val COLLISION_WALL_RIGHT = 4
        val COLLISION_PLATFORM_NOGODOWN = 5
    }
}