package net.torvald.terrarum.gameactors

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.abs
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.sqrt
import org.dyn4j.geometry.Vector2

/**
 * Abstract base class for autonomously moving contraptions that carry other actors.
 *
 * Handles rider management (mount/dismount), momentum conservation, and
 * velocity-driven hitbox translation. Subclasses provide geometry-specific
 * mount detection via [isActorOnTop] and set [contraptionVelocity] before
 * calling `super.updateImpl(delta)`.
 *
 * Created by minjaesong on 2026-02-08.
 */
abstract class PhysContraption() : ActorWithBody() {

    /** Actors currently riding this contraption, stored by ActorID for serialisation. */
    protected val actorsRiding = ArrayList<ActorID>()

    /** Velocity the contraption intends to move this tick. Subclasses set this before calling super.updateImpl(). */
    protected val contraptionVelocity = Vector2(0.0, 0.0)

    /** Actual displacement applied this tick (after clampHitbox). */
    private val appliedVelocity = Vector2(0.0, 0.0)

    /** Tolerance above contraption top for "feet on top" detection (pixels). */
    @Transient protected open val MOUNT_TOLERANCE_ABOVE: Double = INGAME.world.gravitation.y.abs().sqrt()

    /** Tolerance below contraption top — how far feet can sink before dismount (pixels). */
    @Transient protected open val MOUNT_TOLERANCE_BELOW: Double = TILE_SIZED

    /** Minimum combined Y velocity to count as "jumping up" (prevents mount while jumping). */
    @Transient protected open val JUMP_THRESHOLD_Y: Double = -0.5

    /** Block whose friction this contraption impersonates. Riders use this for feet friction. */
    var surfaceBlock: ItemID = Block.STONE

    init {
        physProp = PhysProperties.MOBILE_OBJECT()
        collisionType = COLLISION_KINEMATIC
    }

    override fun updateImpl(delta: Float) {
        // Snapshot position before movement
        val oldX = hitbox.startX
        val oldY = hitbox.startY

        // Set externalV to our contraption velocity so super translates the hitbox
        externalV.set(contraptionVelocity.x, contraptionVelocity.y)

        // super.updateImpl handles:
        //   - sprite updates
        //   - hitbox.translate(externalV) (since usePhysics=false -> isNoCollideWorld=true)
        //   - clampHitbox
        //   - tilewise hitbox cache updates
        //   - position vector updates
        super.updateImpl(delta)

        // Compute actual displacement (clampHitbox may have wrapped coordinates)
        appliedVelocity.set(hitbox.startX - oldX, hitbox.startY - oldY)

        // --- Step 1: Move existing riders BEFORE mount detection ---
        // This keeps riders aligned with the contraption's new position so the
        // mount check doesn't fail when the contraption is moving fast.
        for (riderId in actorsRiding.toList()) {
            val rider = INGAME.getActorByID(riderId) as? ActorWithBody ?: continue

            val oldRiderX = rider.hitbox.startX
            val oldRiderY = rider.hitbox.startY

            // Apply horizontal displacement, then check for wall collision
            rider.hitbox.translatePosX(appliedVelocity.x)
            if (!rider.isNoCollideWorld && rider.isWalled(rider.hitbox, COLLIDING_LR)) {
                rider.hitbox.setPositionX(oldRiderX)
            }

            // Snap to contraption surface (sets Y), then check for ceiling collision
            snapRiderToSurface(rider)
            if (!rider.isNoCollideWorld && rider.isWalled(rider.hitbox, COLLIDING_TOP)) {
                rider.hitbox.setPositionY(oldRiderY)
            }

            if (rider.externalV.y > 0.0) {
                rider.externalV.y = 0.0
            }
            rider.walledBottom = true
        }

        // --- Step 2: Mount detection (riders are now at correct positions) ---

        val ridersToRemove = ArrayList<ActorID>()
        val currentRiders = ArrayList<ActorWithBody>()

        // Check all active actors + actorNowPlaying
        val candidates = ArrayList<ActorWithBody>()
        INGAME.actorContainerActive.forEach {
            if (it is ActorWithBody && it !== this && it !is PhysContraption) {
                candidates.add(it)
            }
        }
        INGAME.actorNowPlaying?.let { candidates.add(it) }

        for (actor in candidates) {
            if (isActorOnTop(actor)) {
                if (!actorsRiding.contains(actor.referenceID)) {
                    // New rider — mount and snap
                    mount(actor)
                    snapRiderToSurface(actor)
                    if (actor.externalV.y > 0.0) {
                        actor.externalV.y = 0.0
                    }
                    actor.walledBottom = true
                }
                currentRiders.add(actor)
            }
        }

        // --- Step 3: Dismount actors no longer on top ---
        val currentRiderIds = currentRiders.map { it.referenceID }.toSet()
        for (riderId in actorsRiding.toList()) {
            if (riderId !in currentRiderIds) {
                val rider = INGAME.getActorByID(riderId)
                if (rider is ActorWithBody) {
                    dismount(rider)
                }
                else {
                    ridersToRemove.add(riderId)
                }
            }
        }
        ridersToRemove.forEach { actorsRiding.remove(it) }
    }

    /**
     * Geometry check: is this actor positioned on top of the contraption such that
     * it should be considered a rider? Subclasses override for different geometries.
     */
    abstract fun isActorOnTop(actor: ActorWithBody): Boolean

    /**
     * Snap a rider's vertical position to this contraption's surface.
     * Default implementation places the rider on top (feet at contraption top).
     * Override for contraptions that carry riders differently.
     */
    open fun snapRiderToSurface(rider: ActorWithBody) {
        rider.hitbox.setPositionY(hitbox.startY - rider.hitbox.height)
    }

    /**
     * Add an actor to the rider list.
     */
    fun mount(actor: ActorWithBody) {
        if (!actorsRiding.contains(actor.referenceID)) {
            actorsRiding.add(actor.referenceID)
            actor.platformsRiding.add(this.referenceID)
        }
    }

    /**
     * Remove an actor from the rider list and apply dismount impulse.
     */
    fun dismount(actor: ActorWithBody) {
        actorsRiding.remove(actor.referenceID)
        actor.platformsRiding.remove(this.referenceID)

        // Conservation of momentum: add contraption velocity as impulse
        actor.externalV.x += contraptionVelocity.x
        actor.externalV.y += contraptionVelocity.y
    }
}
