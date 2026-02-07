package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import org.dyn4j.geometry.Vector2

/**
 * Base class for autonomously moving platforms that carry other actors standing on them.
 *
 * Subclasses must set [platformVelocity] before calling `super.updateImpl(delta)`.
 *
 * Created by minjaesong on 2022-02-28.
 */
open class ActorMovingPlatform() : ActorWithBody() {

    protected var tilewiseWidth = 3

    constructor(newTilewiseWidth: Int) : this() {
        this.tilewiseWidth = newTilewiseWidth
    }

    /** Actors currently riding this platform, stored by ActorID for serialisation. */
    @Transient protected val actorsRiding = ArrayList<ActorID>()

    /** Velocity the platform intends to move this tick. Subclasses set this before calling super.updateImpl(). */
    @Transient protected val platformVelocity = Vector2(0.0, 0.0)

    /** Actual displacement applied this tick (after clampHitbox). */
    @Transient private val appliedVelocity = Vector2(0.0, 0.0)

    /** Tolerance in pixels for "feet on top of platform" detection. */
    @Transient private val MOUNT_TOLERANCE_Y = 2.0

    /** Minimum combined Y velocity to count as "jumping up" (prevents mount while jumping). */
    @Transient private val JUMP_THRESHOLD_Y = -0.5

    @Transient private var platformTexture: Texture? = null
    @Transient private var platformTextureRegion: TextureRegion? = null

    init {
        physProp = PhysProperties.MOBILE_OBJECT()
        collisionType = COLLISION_KINEMATIC

        setHitboxDimension(TILE_SIZE * tilewiseWidth, TILE_SIZE, 0, 0)
    }

    private fun ensureTexture() {
        if (platformTexture == null) {
            val w = TILE_SIZE * tilewiseWidth
            val h = TILE_SIZE
            val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
            // grey-blue colour
            pixmap.setColor(Color(0.45f, 0.55f, 0.65f, 1f))
            pixmap.fill()
            // slightly darker border
            pixmap.setColor(Color(0.35f, 0.45f, 0.55f, 1f))
            pixmap.drawRectangle(0, 0, w, h)
            platformTexture = Texture(pixmap)
            platformTextureRegion = TextureRegion(platformTexture)
            pixmap.dispose()
        }
    }

    override fun updateImpl(delta: Float) {
        // Snapshot position before movement
        val oldX = hitbox.startX
        val oldY = hitbox.startY

        // Set externalV to our platform velocity so super translates the hitbox
        externalV.set(platformVelocity.x, platformVelocity.y)

        // super.updateImpl handles:
        //   - sprite updates
        //   - hitbox.translate(externalV) (since usePhysics=false -> isNoCollideWorld=true)
        //   - clampHitbox
        //   - tilewise hitbox cache updates
        //   - position vector updates
        super.updateImpl(delta)

        // Compute actual displacement (clampHitbox may have wrapped coordinates)
        appliedVelocity.set(hitbox.startX - oldX, hitbox.startY - oldY)

        // --- Mount detection and rider management ---

        val ridersToRemove = ArrayList<ActorID>()

        // Build set of actors currently on top of this platform
        val newRiders = ArrayList<ActorWithBody>()

        // Check all active actors + actorNowPlaying
        val candidates = ArrayList<ActorWithBody>()
        INGAME.actorContainerActive.forEach {
            if (it is ActorWithBody && it !== this && it !is ActorMovingPlatform) {
                candidates.add(it)
            }
        }
        INGAME.actorNowPlaying?.let { candidates.add(it) }

        for (actor in candidates) {
            val feetY = actor.hitbox.endY
            val platTop = hitbox.startY

            // Check vertical proximity: feet within tolerance of platform top
            val verticallyAligned = Math.abs(feetY - platTop) <= MOUNT_TOLERANCE_Y

            // Check horizontal overlap
            val horizontalOverlap = actor.hitbox.endX > hitbox.startX && actor.hitbox.startX < hitbox.endX

            // Check not jumping upward
            val combinedVelY = actor.externalV.y + (actor.controllerV?.y ?: 0.0)
            val notJumping = combinedVelY >= JUMP_THRESHOLD_Y

            if (verticallyAligned && horizontalOverlap && notJumping) {
                if (!actorsRiding.contains(actor.referenceID)) {
                    mount(actor)
                }
                newRiders.add(actor)
            }
        }

        // Dismount actors that are no longer on top
        val newRiderIds = newRiders.map { it.referenceID }.toSet()
        for (riderId in actorsRiding.toList()) {
            if (riderId !in newRiderIds) {
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

        // Move riders and suppress their gravity
        for (rider in newRiders) {
            // Translate rider by platform's actual displacement
            rider.hitbox.translate(appliedVelocity)

            // Snap rider's feet to platform top
            rider.hitbox.setPositionY(hitbox.startY - rider.hitbox.height)

            // Suppress gravity for this tick
            rider.walledBottom = true
        }
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

        // Conservation of momentum: add platform velocity as impulse
        actor.externalV.x += platformVelocity.x
        actor.externalV.y += platformVelocity.y
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible) {
            ensureTexture()
            platformTextureRegion?.let {
                drawTextureInGoodPosition(frameDelta, it, batch)
            }
        }
    }

    override fun dispose() {
        platformTexture?.dispose()
        platformTexture = null
        platformTextureRegion = null
        super.dispose()
    }
}
