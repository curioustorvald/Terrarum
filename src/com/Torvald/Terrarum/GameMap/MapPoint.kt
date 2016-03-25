package com.torvald.terrarum.gamemap

import com.torvald.point.Point2f

import java.io.Serializable


class MapPoint {
    var startPoint: Point2f? = null
        private set
    var endPoint: Point2f? = null
        private set

    constructor() {

    }

    constructor(p1: Point2f, p2: Point2f) {
        setPoint(p1, p2)
    }

    constructor(x1: Int, y1: Int, x2: Int, y2: Int) {
        setPoint(x1, y1, x2, y2)
    }

    fun setPoint(p1: Point2f, p2: Point2f) {
        startPoint = p1
        endPoint = p2
    }

    fun setPoint(x1: Int, y1: Int, x2: Int, y2: Int) {
        startPoint = Point2f(x1.toFloat(), y1.toFloat())
        endPoint = Point2f(x2.toFloat(), y2.toFloat())
    }
}
