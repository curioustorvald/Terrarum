package net.torvald.point

/**
 * Created by minjaesong on 16-01-15.
 */
class Point2d(var x: Double, var y: Double) : Cloneable {

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }
}
