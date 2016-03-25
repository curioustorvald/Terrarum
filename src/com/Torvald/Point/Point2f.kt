package com.torvald.point

/**
 * Created by minjaesong on 16-01-15.
 */
class Point2f(x: Float, y: Float) {

    var x: Float = 0.toFloat()
        private set
    var y: Float = 0.toFloat()
        private set

    init {
        this.x = x
        this.y = y
    }

    operator fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }
}
