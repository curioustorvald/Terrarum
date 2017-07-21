package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameworld.GameWorld
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-08-29.
 */
class ProjectileHoming(
        world: GameWorld,
        type: Int,
        fromPoint: Vector2, // projected coord
        toPoint: Vector2    // arriving coord
) : ProjectileSimple(world, type, fromPoint, toPoint) {



}