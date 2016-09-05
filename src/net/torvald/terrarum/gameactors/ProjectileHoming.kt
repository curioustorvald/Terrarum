package net.torvald.terrarum.gameactors

import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-08-29.
 */
class ProjectileHoming(
        type: Int,
        fromPoint: Vector2, // projected coord
        toPoint: Vector2, // arriving coord
        override var luminosity: Int = 0) : ProjectileSimple(type, fromPoint, toPoint, luminosity) {



}