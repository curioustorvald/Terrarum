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
 * TODO: in the future this must be generalised as a PhysContraption
 *
 * Created by minjaesong on 2022-02-28.
 */
open class ActorMovingPlatform() : ActorWithBody() {

    protected var tilewiseWidth = 3

    constructor(newTilewiseWidth: Int) : this() {
        this.tilewiseWidth = newTilewiseWidth

        physProp = PhysProperties.MOBILE_OBJECT()
        collisionType = COLLISION_KINEMATIC

        setHitboxDimension(TILE_SIZE * newTilewiseWidth, TILE_SIZE, 0, 0)
    }

    /** Actors currently riding this platform, stored by ActorID for serialisation. */
    @Transient protected val actorsRiding = ArrayList<ActorID>()

    /** Velocity the platform intends to move this tick. Subclasses set this before calling super.updateImpl(). */
    @Transient protected val platformVelocity = Vector2(0.0, 0.0)

    /** Actual displacement applied this tick (after clampHitbox). */
    @Transient private val appliedVelocity = Vector2(0.0, 0.0)

    /** Tolerance above platform top for "feet on top" detection (pixels). */
    @Transient private val MOUNT_TOLERANCE_ABOVE = (TILE_SIZE / 2).toDouble()//2.0

    /** Tolerance below platform top — how far feet can sink before dismount (pixels). */
    @Transient private val MOUNT_TOLERANCE_BELOW = (TILE_SIZE / 2).toDouble()

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

        // --- Step 1: Move existing riders BEFORE mount detection ---
        // This keeps riders aligned with the platform's new position so the
        // mount check doesn't fail when the platform is moving fast.
        for (riderId in actorsRiding.toList()) {
            val rider = INGAME.getActorByID(riderId) as? ActorWithBody
            if (rider != null) {
                rider.hitbox.translate(appliedVelocity)
                rider.hitbox.setPositionY(hitbox.startY - rider.hitbox.height)
                if (rider.externalV.y > 0.0) {
                    rider.externalV.y = 0.0
                }
                rider.walledBottom = true
            }
        }

        // --- Step 2: Mount detection (riders are now at correct positions) ---

        val ridersToRemove = ArrayList<ActorID>()
        val currentRiders = ArrayList<ActorWithBody>()

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
            val headY = actor.hitbox.startY
            val platTop = hitbox.startY

            // Feet are near platform top: slightly above or sunk partway in
            val feetNearPlatTop = feetY >= platTop - MOUNT_TOLERANCE_ABOVE &&
                                  feetY <= platTop + MOUNT_TOLERANCE_BELOW

            // Actor's head must be above platform top (prevents mounting from below)
            val comingFromAbove = headY < platTop

            // Check horizontal overlap
            val horizontalOverlap = actor.hitbox.endX > hitbox.startX && actor.hitbox.startX < hitbox.endX

            // Check not jumping upward
            val combinedVelY = actor.externalV.y + (actor.controllerV?.y ?: 0.0)
            val notJumping = combinedVelY >= JUMP_THRESHOLD_Y

            if (feetNearPlatTop && comingFromAbove && horizontalOverlap && notJumping) {
                if (!actorsRiding.contains(actor.referenceID)) {
                    // New rider — mount and snap
                    mount(actor)
                    actor.hitbox.setPositionY(hitbox.startY - actor.hitbox.height)
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
