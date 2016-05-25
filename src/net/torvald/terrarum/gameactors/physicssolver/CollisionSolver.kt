package net.torvald.terrarum.gameactors.physicssolver

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
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
    private var collCandidates = ArrayList<Pair<ActorWithBody, ActorWithBody>>(COLL_FINAL_CANDIDATES_SIZE)

    private val collCandidateStack = Stack<CollisionMarkings>()

    /**
     * to see what's going on here, visit
     * [this link](https://www.toptal.com/game/video-game-physics-part-ii-collision-detection-for-solid-objects)
     */
    fun process() {
        // TODO threading X and Y
        // clean up before we go
        collListX.clear()
        collListY.clear()
        collCandidateX.clear()
        collCandidateY.clear()

        // mark list x
        Terrarum.game.actorContainer.forEach { it ->
            if (it is ActorWithBody) {
                collListX.add(CollisionMarkings(it.hitbox.hitboxStart.x, STARTPOINT, it))
                collListX.add(CollisionMarkings(it.hitbox.hitboxEnd.x, ENDPOINT, it))
            }
        }
        // sort list x
        collListX.sortBy { it.pos }

        // set candidateX
        for (it in collListX) {
            if (it.kind == STARTPOINT) {
                collCandidateStack.push(it)
            }
            else if (it.kind == ENDPOINT) {
                val mark_this = it
                val mark_other = collCandidateStack.pop()
                val collCandidate: Pair<ActorWithBody, ActorWithBody>
                if (mark_this < mark_other) // make sure actor with lower pos comes left
                    collCandidate = Pair(mark_this.actor, mark_other.actor)
                else
                    collCandidate = Pair(mark_other.actor, mark_this.actor)

                collCandidateX.add(collCandidate)
            }
        }
        collCandidateStack.clear()

        // mark list y
        Terrarum.game.actorContainer.forEach { it ->
            if (it is ActorWithBody) {
                collListY.add(CollisionMarkings(it.hitbox.hitboxStart.y, STARTPOINT, it))
                collListY.add(CollisionMarkings(it.hitbox.hitboxEnd.y, ENDPOINT, it))
            }
        }
        // sort list y
        collListY.sortBy { it.pos }

        // set candidateY
        for (it in collListY) {
            if (it.kind == STARTPOINT) {
                collCandidateStack.push(it)
            }
            else if (it.kind == ENDPOINT) {
                val mark_this = it
                val mark_other = collCandidateStack.pop()
                val collCandidate: Pair<ActorWithBody, ActorWithBody>
                if (mark_this < mark_other) // make sure actor with lower pos comes left
                    collCandidate = Pair(mark_this.actor, mark_other.actor)
                else
                    collCandidate = Pair(mark_other.actor, mark_this.actor)

                collCandidateY.add(collCandidate)
            }
        }
        // look for overlaps in candidate X/Y and put them into collCandidates
        // overlapping in X and Y means they are actually overlapping physically
        collCandidateY.retainAll(collCandidateX) // list Y will have intersection of X and Y now
        collCandidates = collCandidateY // renaming. X and Y won't be used anyway.

        // solve collision for actors in collCandidates
        collCandidates.forEach { solveCollision(it.first, it.second) }
    }

    private fun solveCollision(a: ActorWithBody, b: ActorWithBody) {
        // some of the Pair(a, b) are either duplicates or erroneously reported.
        // e.g. (A, B), (B, C) and then (A, C);
        //      in some situation (A, C) will not making any contact with each other
        // we are going to filter them
        if (a isCollidingWith b) {
            // notify collision, but not solve it yet
            // (e.g. player vs mob, will pass by but still takes damage)


            // if they actually makes collision (e.g. player vs ball), solve it
            if (a makesCollisionWith b) {
                // assuming perfect elastic collision; ignoring 'var elasticity'
                val ux_1 = a.veloX
                val ux_2 = b.veloX
                val uy_1 = a.veloY
                val uy_2 = b.veloY
                val m1 = a.mass
                val m2 = b.mass

                val vx_1 = (ux_2 * (m1 - m2) + 2 * m2 * ux_2) / (m1 + m2)
                val vx_2 = (ux_2 * (m2 - m1) + 2 * m1 * ux_1) / (m1 + m2)
                val vy_1 = (uy_2 * (m1 - m2) + 2 * m2 * uy_2) / (m1 + m2)
                val vy_2 = (uy_2 * (m2 - m1) + 2 * m1 * uy_1) / (m1 + m2)

                /*a.veloX = vx_1
                a.veloY = vy_1
                b.veloX = vx_2
                b.veloY = vy_2*/
            }
        }
    }

    private infix fun ActorWithBody.makesCollisionWith(other: ActorWithBody): Boolean {
        return true
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

        var t_ax: Double; var t_ay: Double
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

    fun Double.abs() = if (this < 0) -this else this
    fun Double.sqr() = this * this

    class CollisionMarkings(
            val pos: Double,
            val kind: Int,
            val actor: ActorWithBody
    ) : Comparable<CollisionMarkings> {
        override fun compareTo(other: CollisionMarkings): Int =
                if (this.pos > other.pos) 1
                else if (this.pos < other.pos) -1
                else 0
    }

    /**
     * === Some useful physics knowledge ===
     *
     * * Momentum = mass × Velocity (p = mv, conserved)
     *
     * * Force = mass × acceleration (f = ma, conserved)
     *
     * * F_AB = -F_BA (Lex Tertia, does NOT apply to fictitious force like centrifugal)
     */
}