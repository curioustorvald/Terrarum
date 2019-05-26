package net.torvald.aa

import net.torvald.terrarum.Point2d
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.sqr

/**
 * k-d Tree that uses binary heap instead of binary tree to improve data locality
 *
 *
 * -- I couldn't observe any significant boost in performance but this one seems
 *    to give 3-4 more frames per second.
 *
 * Created by minjaesong on 2017-01-02.
 *
 *
 * Remarks:
 * - NOT using the fullCodePage with 2x2 mode makes it slower... skewed tree generation?
 */
class KDHeapifiedTree(actors: List<ActorWBMovable>) {

    private val dimension = 2
    private val initialSize = 128
    private val nodes = Array<ActorWBMovable?>(initialSize, { null })

    private val root: Int = 0

    fun findNearestActor(query: Point2d): ActorWBMovable =
                getNearest(root, query, 0).getActor()!!

    private fun Int.get() = nodes[this]?.feetPosPoint
    private fun Int.getActor() = nodes[this]
    private fun Int.getLeft() = this * 2 + 1
    private fun Int.getRight() = this * 2 + 2
    private fun Int.set(value: ActorWBMovable?) {
        try {
            nodes[this] = value
        }
        catch (_: ArrayIndexOutOfBoundsException) {
            // modification of the private fun expandArray()
            val prevNodes = nodes.copyOf() + value
            Array<ActorWBMovable?>(prevNodes.size * 2, { null })
            create(prevNodes.toList(), 0, 0)
        }
    }
    private fun Int.setLeftChild(value: ActorWBMovable?) { nodes[this.getLeft()] = value }
    private fun Int.setRightChild(value: ActorWBMovable?) { nodes[this.getRight()] = value }

    private val zeroPoint = Point2d(0.0, 0.0)

    init {
        create(actors, 0, 0)
    }

    private fun create(points: List<ActorWBMovable?>, depth: Int, index: Int): ActorWBMovable? {
        if (points.isEmpty()) {
            index.set(null)

            return null
        }
        else {
            val items = points.sortedBy {
                if (it != null) it.feetPosPoint[depth % dimension]
                else            Double.POSITIVE_INFINITY
            }
            val halfItems = items.size shr 1

            index.setLeftChild(create(items.subList(0, halfItems), depth + 1, index.getLeft()))
            index.setRightChild(create(items.subList(halfItems + 1, items.size), depth + 1, index.getRight()))
            index.set(items[halfItems])

            return index.getActor()
        }
    }

    private fun getNearest(currentNode: Int, query: Point2d, depth: Int): Int {
        //println("depth, $depth")

        val direction = currentNode.compare(query, depth % dimension)

        val next  = if (direction < 0) currentNode.getLeft()  else currentNode.getRight()
        val other = if (direction < 0) currentNode.getRight() else currentNode.getLeft()
        var best  = if (next.get() == null)
            currentNode
        else
            getNearest(next, query, depth + 1) // traverse to leaf

        if (currentNode.get()!!.distSqr(query) < best.get()!!.distSqr(query)) {
            best = currentNode
        }

        if (other.get() != null) {
            if (currentNode.get()!!.dimDistSqr(query, depth % dimension) < best.get()!!.distSqr(query)) {
                val bestCandidate = getNearest(other, query, depth + 1)
                if (bestCandidate.get()!!.distSqr(query) < best.get()!!.distSqr(query)) {
                    best = bestCandidate
                }
            }
        }

        return best // work back up
    }

    private fun expandArray() {
        val prevNodes = nodes.copyOf()
        Array<ActorWBMovable?>(prevNodes.size * 2, { null })
        create(prevNodes.toList(), 0, 0)
    }

    fun Int.compare(other: Point2d, dimension: Int) =
            other[dimension] - this.get()!![dimension]

    private fun Point2d.dimDistSqr(other: Point2d, dimension: Int) =
            other[dimension].minus(this[dimension]).sqr()

    private operator fun Point2d.get(index: Int) = if (index == 0) this.x else this.y
}
