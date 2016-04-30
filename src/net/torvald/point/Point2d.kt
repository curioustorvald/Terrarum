package net.torvald.point

/**
 * Created by minjaesong on 16-01-15.
 */
class Point2d(x: Double, y: Double) {

    var x: Double = 0.toDouble()
        private set
    var y: Double = 0.toDouble()
        private set

    init {
        this.x = x
        this.y = y
    }

    operator fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }
}
