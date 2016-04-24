package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import java.util.*

/**
 * Created by minjaesong on 16-04-22.
 */
object CollisionSolver {

    private const val STARTPOINT = 1
    private const val ENDPOINT = 2

    private const val COLL_LIST_SIZE = 256
    private const val COLL_CANDIDATES_SIZE = 128
    private const val COLL_FINAL_CANDIDATES_SIZE = 16

    private val collListX = ArrayList<CollisionMarkings>(COLL_LIST_SIZE)
    private val collListY = ArrayList<CollisionMarkings>(COLL_LIST_SIZE)

    private val collCandidateX = ArrayList<Pair<ActorWithBody, ActorWithBody>>(COLL_CANDIDATES_SIZE)
    private val collCandidateY = ArrayList<Pair<ActorWithBody, ActorWithBody>>(COLL_CANDIDATES_SIZE)
    private val collCandidates = ArrayList<Pair<ActorWithBody, ActorWithBody>>(COLL_FINAL_CANDIDATES_SIZE)

    /**
     * @link https://www.toptal.com/game/video-game-physics-part-ii-collision-detection-for-solid-objects
     */
    fun process() {
        // mark list x
        Terrarum.game.actorContainer.forEach { it ->
            if (it is ActorWithBody) {
                collListX.add(CollisionMarkings(it.hitbox.hitboxStart.x, STARTPOINT, it.referenceID))
                collListX.add(CollisionMarkings(it.hitbox.hitboxEnd.x, ENDPOINT, it.referenceID))
            }
        }

        // sort list x
        collListX.sortBy { it.pos }

        // set candidateX

        // mark list y
        Terrarum.game.actorContainer.forEach { it ->
            if (it is ActorWithBody) {
                collListY.add(CollisionMarkings(it.hitbox.hitboxStart.y, STARTPOINT, it.referenceID))
                collListY.add(CollisionMarkings(it.hitbox.hitboxEnd.y, ENDPOINT, it.referenceID))
            }
        }

        // sort list y
        collListY.sortBy { it.pos }

        // set candidateY

        // look for overlaps in candidate X/Y and put them into collCandidates

        // solve collision for actors in collCandidates
    }

    private fun solveCollision(a: ActorWithBody, b: ActorWithBody) {

    }

    private infix fun ActorWithBody.isCollidingWith(other: ActorWithBody): Boolean {
        val ax = this.hitbox.centeredX
        val ay = this.hitbox.centeredY
        val bx = other.hitbox.centeredX
        val by = other.hitbox.centeredY

        // will refer 'actor_dist_t' as 't' afterward
        val actor_dist_t_sqr = ((ay - by).sqr() + (ax - bx).sqr()) // no sqrt; 'power' is slower than 'times'
        val dist_x = (ax - bx).abs() // 'tx'
        val dist_y = (ay - by).abs() // 'ty'
        val tangent = dist_y / dist_x

        var t_ax: Float; var t_ay: Float
        if (dist_x > dist_y) {
            t_ax = this.hitbox.width / 2
            t_ay = t_ax * tangent
        }
        else {
            t_ay = this.hitbox.height / 2
            t_ax = t_ay * tangent
        }

        return (t_ax.sqr() + t_ay.sqr()) < actor_dist_t_sqr
    }

    fun Float.abs() = if (this < 0) -this else this
    fun Float.sqr() = this * this

    data class CollisionMarkings(
            val pos: Float,
            val kind: Int,
            val actorID: Int
    )

    /**
     * === Some useful physics knowledge ===
     *
     * * Momentum = mass × Velocity
     */
}