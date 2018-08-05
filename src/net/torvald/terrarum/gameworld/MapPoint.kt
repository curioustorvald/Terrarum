package net.torvald.terrarum.gameworld

import net.torvald.terrarum.Point2d


class MapPoint {
    var startPoint: Point2d? = null
        private set
    var endPoint: Point2d? = null
        private set

    constructor() {

    }

    constructor(p1: Point2d, p2: Point2d) {
        setPoint(p1, p2)
    }

    constructor(x1: Int, y1: Int, x2: Int, y2: Int) {
        setPoint(x1, y1, x2, y2)
    }

    fun setPoint(p1: Point2d, p2: Point2d) {
        startPoint = p1
        endPoint = p2
    }

    fun setPoint(x1: Int, y1: Int, x2: Int, y2: Int) {
        startPoint = Point2d(x1.toDouble(), y1.toDouble())
        endPoint = Point2d(x2.toDouble(), y2.toDouble())
    }
}
