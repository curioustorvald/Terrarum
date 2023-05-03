package net.torvald.terrarum.modulebasegame.icongen

import net.torvald.terrarum.Point2d

/**
 *
 *   .   _ end point
 *  / \  _ control point 1 LR
 *  | |  _ control point 2 LR
 *  | |  _ ...
 *  | |  _ control point 8 LR
 * ===== _ accessory (hilt)
 *   |   _ accessory (grip)
 *   O   _ accessory (pommel)
 *
 * Created by minjaesong on 2020-02-11.
 */
@JvmInline
value class IconGenMesh(val datapoints: Array<Point2d>) {

    operator fun times(other: PerturbMesh) {

    }

}

@JvmInline
value class PerturbMesh(val datapoints: Array<Point2d>) {

}