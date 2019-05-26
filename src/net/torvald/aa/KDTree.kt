package net.torvald.aa

import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.sqr

/**
 * Created by minjaesong on 2019-04-18.
 */
class KDTree(points: List<ActorWithBody>) {

    companion object {
        const val DIMENSION = 2
    }

    private val root: KDNode?

    init {
        root = create(points, 0)
    }

    fun findNearest(query: ActorWithBody) = getNearest(root!!, query.hitbox, 0)

    private fun create(points: List<ActorWithBody>, depth: Int): KDNode? {
        if (points.isEmpty()) {
            return null
        }
        else {
            val items = points.sortedBy { it.getDimensionalPoint(depth) }
            val currentIndex = items.size shr 1

            return KDNode(
                    create(items.subList(0, currentIndex), depth + 1),
                    create(items.subList(currentIndex + 1, items.size), depth + 1),
                    items[currentIndex],
                    items[currentIndex].hitbox.clone()
            )
        }
    }

    private fun getNearest(currentNode: KDNode, actorHitbox: Hitbox, depth: Int = 0): KDNode {
        val direction = currentNode.compare(actorHitbox, depth % DIMENSION)

        val next  = if (direction < 0) currentNode.left  else currentNode.right
        val other = if (direction < 0) currentNode.right else currentNode.left
        var best  = if (next == null) currentNode else getNearest(next, actorHitbox, depth + 1) // traverse to leaf

        if (currentNode.position.distSqr(actorHitbox) < best.position.distSqr(actorHitbox)) {
            best = currentNode
        }

        if (other != null) {
            if (currentNode.position.dimDistSqr(actorHitbox, depth % DIMENSION) < best.position.distSqr(actorHitbox)) {
                val bestCandidate = getNearest(other, actorHitbox, depth + 1)
                if (bestCandidate.position.distSqr(actorHitbox) < best.position.distSqr(actorHitbox)) {
                    best = bestCandidate
                }
            }
        }

        return best // work back up
    }

    data class KDNode(val left: KDNode?, val right: KDNode?, val actor: ActorWithBody, val position: Hitbox) {
        //fun compare(other: ActorWithBody, dimension: Int) = other.getDimensionalPoint(dimension) - this.position.getDimensionalPoint(dimension)
        fun compare(other: Hitbox, dimension: Int) = other.getDimensionalPoint(dimension) - this.position.getDimensionalPoint(dimension)
    }

    private fun Hitbox.distSqr(other: Hitbox): Double {
        var dist = 0.0
        for (i in 0 until DIMENSION)
            dist += (this.getDimensionalPoint(i) - other.getDimensionalPoint(i)).sqr()
        return dist
    }

    private fun Hitbox.dimDistSqr(other: Hitbox, dimension: Int) = other.getDimensionalPoint(dimension).minus(this.getDimensionalPoint(dimension)).sqr()
}

internal fun ActorWithBody.getDimensionalPoint(depth: Int) = this.hitbox.getDimensionalPoint(depth)
// TODO take ROUNDWORLD into account
internal fun Hitbox.getDimensionalPoint(depth: Int) =
        if (depth % KDTree.DIMENSION == 0) this.canonicalX else this.canonicalY