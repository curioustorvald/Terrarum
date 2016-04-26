package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.gamemap.GameMap
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.tileproperties.TilePropCodex
import net.torvald.spriteanimation.SpriteAnimation
import com.jme3.math.FastMath
import net.torvald.terrarum.tileproperties.TileNameCode
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Base class for every actor that has physical (or visible) body. This includes furnishings, paintings, gadgets, etc.
 *
 * Created by minjaesong on 16-03-14.
 */
open class ActorWithBody constructor() : Actor(), Visible {

    override var actorValue: ActorValue = ActorValue()

    var hitboxTranslateX: Float = 0.toFloat()// relative to spritePosX
    var hitboxTranslateY: Float = 0.toFloat()// relative to spritePosY
    var baseHitboxW: Int = 0
    var baseHitboxH: Int = 0

    /**
     * Velocity vector (broken down by axes) for newtonian sim.
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     */
    @Volatile var veloX: Float = 0.toFloat()
    @Volatile var veloY: Float = 0.toFloat()
    @Transient private val VELO_HARD_LIMIT = 10000f

    var grounded = false

    @Transient var sprite: SpriteAnimation? = null
    @Transient var spriteGlow: SpriteAnimation? = null
    /** Default to 'false'  */
    var isVisible = false
    /** Default to 'true'  */
    var isUpdate = true

    var isNoSubjectToGrav = false
    var isNoCollideWorld = false
    var isNoSubjectToFluidResistance = false

    internal var baseSpriteWidth: Int = 0
    internal var baseSpriteHeight: Int = 0

    override var referenceID: Int = generateUniqueReferenceID()
    /**
     * Positions: top-left point
     */
    override val hitbox = Hitbox(0f,0f,0f,0f)
    @Transient val nextHitbox = Hitbox(0f,0f,0f,0f)

    /**
     * Physical properties.
     * Values derived from ActorValue must be @Transient.
     */
    @Transient var scale = 1f
    @Transient var mass = 2f
    @Transient private val MASS_LOWEST = 2f
    /** Valid range: [0, 1]  */
    var elasticity = 0f
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithBody] Invalid elasticity value: $value; valid elasticity value is [0, 1].")
            else if (value > 1) {
                println("[ActorWithBody] Elasticity were capped to 1.")
                field = ELASTICITY_MAX
            }
            else
                field = value * ELASTICITY_MAX
        }
    @Transient private val ELASTICITY_MAX = 0.993f
    private var density = 1000f

    /**
     * Gravitational Constant G. Load from gamemap.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    @Transient private val METER = 24f
    /**
     * [m / s^2] * SI_TO_GAME_ACC -> [px / InternalFrame^2]
     */
    @Transient private val SI_TO_GAME_ACC = METER / FastMath.sqr(Terrarum.TARGET_FPS.toFloat())
    /**
     * [m / s] * SI_TO_GAME_VEL -> [px / InternalFrame]
     */
    @Transient private val SI_TO_GAME_VEL = METER / Terrarum.TARGET_FPS

    @Transient private var gravitation: Float = 0.toFloat()
    @Transient private val DRAG_COEFF = 1f

    @Transient private val CONTACT_AREA_TOP = 0
    @Transient private val CONTACT_AREA_RIGHT = 1
    @Transient private val CONTACT_AREA_BOTTOM = 2
    @Transient private val CONTACT_AREA_LEFT = 3

    @Transient private val UD_COMPENSATOR_MAX = TSIZE
    @Transient private val LR_COMPENSATOR_MAX = TSIZE

    /**
     * A constant to make falling faster so that the game is more playable
     */
    @Transient private val G_MUL_PLAYABLE_CONST = 1.4142f

    @Transient private val EVENT_MOVE_TOP = 0
    @Transient private val EVENT_MOVE_RIGHT = 1
    @Transient private val EVENT_MOVE_BOTTOM = 2
    @Transient private val EVENT_MOVE_LEFT = 3
    @Transient private val EVENT_MOVE_NONE = -1

    @Transient internal var eventMoving = EVENT_MOVE_NONE // cannot collide both X-axis and Y-axis, or else jump control breaks up.

    /**
     * Post-hit invincibility, in milliseconds
     */
    @Transient val INVINCIBILITY_TIME: Int = 500

    @Transient private val map: GameMap

    @Transient private val MASS_DEFAULT: Float = 60f

    internal val physSleep: Boolean
        get() = veloX.abs() < 0.5 && veloY.abs() < 0.5

    /**
     * for collide-to-world compensation
     */
    @Transient private var posAdjustX = 0
    @Transient private var posAdjustY = 0

    @Transient private val BASE_FRICTION = 0.3f

    init {
        map = Terrarum.game.map
    }

    /**

     * @param w
     * *
     * @param h
     * *
     * @param tx +: translate drawn sprite to LEFT.
     * *
     * @param ty +: translate drawn sprite to DOWN.
     * *
     * @see ActorWithBody.drawBody
     * @see ActorWithBody.drawGlow
     */
    fun setHitboxDimension(w: Int, h: Int, tx: Int, ty: Int) {
        baseHitboxH = h
        baseHitboxW = w
        hitboxTranslateX = tx.toFloat()
        hitboxTranslateY = ty.toFloat()
    }

    /**
     * Set hitbox position from bottom-center point
     * @param x
     * *
     * @param y
     */
    fun setPosition(x: Float, y: Float) {
        hitbox.set(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale,
                y - (baseHitboxH - hitboxTranslateY) * scale,
                baseHitboxW * scale,
                baseHitboxH * scale)

        nextHitbox.set(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale,
                y - (baseHitboxH - hitboxTranslateY) * scale,
                baseHitboxW * scale,
                baseHitboxH * scale)
    }

    private fun updatePhysicalInfos() {
        scale = actorValue.getAsFloat(AVKey.SCALE) ?: 1f
        mass = (actorValue.getAsFloat(AVKey.BASEMASS) ?: MASS_DEFAULT) * FastMath.pow(scale, 3f)
        if (elasticity != 0f) elasticity = 0f
    }

    override fun run() = update(Terrarum.appgc, Terrarum.game.DELTA_T)

    override fun update(gc: GameContainer, delta_t: Int) {
        if (isUpdate) {
            /**
             * Update variables
             */
            updatePhysicalInfos()

            // make NoClip work for player
            if (this is Player) {
                isNoSubjectToGrav = isPlayerNoClip
                isNoCollideWorld = isPlayerNoClip
                isNoSubjectToFluidResistance = isPlayerNoClip
            }

            // clamp to the minimum possible mass
            if (mass < MASS_LOWEST) mass = MASS_LOWEST

            // set sprite dimension vars if there IS sprite for the actor
            if (sprite != null) {
                baseSpriteHeight = sprite!!.height
                baseSpriteWidth = sprite!!.width
            }

            // copy gravitational constant from the map the actor is in
            gravitation = map.gravitation

            /**
             * Actual physics thing (altering velocity) starts from here
             */
            // Actors are subject to the gravity and the buoyancy if they are not levitating
            if (!isNoSubjectToGrav) {
                applyGravitation()
                //applyBuoyancy()
            }

            // hard limit velocity
            if (veloX > VELO_HARD_LIMIT) veloX = VELO_HARD_LIMIT
            if (veloY > VELO_HARD_LIMIT) veloY = VELO_HARD_LIMIT

            if (!physSleep) {
                // Set 'next' position (hitbox) to fiddle with
                updateNextHitboxFromVelo()

                // if not horizontally moving then ...
                //if (Math.abs(veloX) < 0.5) { // fix for special situations (see fig. 1 at the bottom of the source)
                //    updateVerticalPos();
                //    updateHorizontalPos();
                //}
                //else {
                // compensate for colliding
                updateHorizontalCollision()
                updateVerticalCollision()

                setHorizontalFriction()
                //}

                // apply our compensation to actual hitbox
                updateHitboxX()
                updateHitboxY()

                // make sure the actor does not go out of the map
                clampNextHitbox()
                clampHitbox()
            }
        }
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)

     * Apply only if not grounded; normal force is not implemented (and redundant)
     * so we manually reset G to zero (not applying G. force) if grounded.
     */
    // FIXME abnormal jump behaviour if mass < 2, same thing happens if mass == 0 (but zero mass is invalid anyway).
    private fun applyGravitation() {
        if (!grounded) {
            /**
             * weight; gravitational force in action
             * W = mass * G (9.8 [m/s^2])
             */
            val W = gravitation * mass
            /**
             * Drag of atmosphere
             * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity) * A (area)
             */
            val A = scale * scale
            val D = DRAG_COEFF * 0.5f * 1.292f * veloY * veloY * A

            veloY += clampCeil(
                    (W - D) / mass * SI_TO_GAME_ACC * G_MUL_PLAYABLE_CONST, VELO_HARD_LIMIT)
        }
    }

    private fun setHorizontalFriction() {
        val friction = BASE_FRICTION * tileFriction.tileFrictionToMult()
        if (veloX < 0) {
            veloX += friction
            if (veloX > 0) veloX = 0f // compensate overshoot
        }
        else if (veloX > 0) {
            veloX -= friction
            if (veloX < 0) veloX = 0f // compensate overshoot
        }
    }

    private fun updateVerticalCollision() {
        if (!isNoCollideWorld) {
            if (veloY >= 0) { // check downward
                if (isColliding(CONTACT_AREA_BOTTOM)) { // the ground has dug into the body
                    adjustHitBottom()
                    veloY = 0f // reset veloY, simulating normal force
                    elasticReflectY()
                    grounded = true
                }
                else if (isColliding(CONTACT_AREA_BOTTOM, 0, 1)) { // the actor is standing ON the ground
                    veloY = 0f // reset veloY, simulating normal force
                    elasticReflectY()
                    grounded = true
                }
                else { // the actor is not grounded at all
                    grounded = false
                }
            }
            else if (veloY < 0) { // check upward
                grounded = false
                if (isColliding(CONTACT_AREA_TOP)) { // the ceiling has dug into the body
                    adjustHitTop()
                    veloY = 0f // reset veloY, simulating normal force
                    elasticReflectY()
                }
                else if (isColliding(CONTACT_AREA_TOP, 0, -1)) { // the actor is touching the ceiling
                    veloY = 0f // reset veloY, simulating normal force
                    elasticReflectY() // reflect on ceiling, for reversed gravity
                }
                else { // the actor is not grounded at all
                }
            }
        }
    }

    private fun adjustHitBottom() {
        val newX = nextHitbox.pointedX // look carefully, getPos or getPointed
        // int-ify posY of nextHitbox
        nextHitbox.setPositionYFromPoint(FastMath.floor(nextHitbox.pointedY).toFloat())

        var newYOff = 0 // always positive

        // count up Y offset until the actor is not touching the ground
        var colliding: Boolean
        do {
            newYOff += 1
            colliding = isColliding(CONTACT_AREA_BOTTOM, 0, -newYOff)
        } while (colliding)

        posAdjustY = -newYOff
        val newY = nextHitbox.pointedY - newYOff
        nextHitbox.setPositionFromPoint(newX, newY)
    }

    private fun adjustHitTop() {
        val newX = nextHitbox.posX
        // int-ify posY of nextHitbox
        nextHitbox.setPositionY(FastMath.ceil(nextHitbox.posY).toFloat())

        var newYOff = 0 // always positive

        // count up Y offset until the actor is not touching the ceiling
        var colliding: Boolean
        do {
            newYOff += 1
            colliding = isColliding(CONTACT_AREA_TOP, 0, newYOff)
        } while (colliding)

        posAdjustY = newYOff
        val newY = nextHitbox.posY + newYOff
        nextHitbox.setPosition(newX, newY)
    }

    private fun updateHorizontalCollision() {
        if (!isNoCollideWorld) {
            if (veloX >= 0.5) { // check right
                if (isColliding(CONTACT_AREA_RIGHT) && !isColliding(CONTACT_AREA_LEFT)) {
                    // the actor is embedded to the wall
                    adjustHitRight()
                    veloX = 0f // reset veloX, simulating normal force
                    elasticReflectX()
                }
                else if (isColliding(CONTACT_AREA_RIGHT, 2, 0) && !isColliding(CONTACT_AREA_LEFT, 0, 0)) { // offset by +1, to fix directional quarks
                    // the actor is touching the wall
                    veloX = 0f // reset veloX, simulating normal force
                    elasticReflectX()
                }
                else {
                }
            }
            else if (veloX <= -0.5) { // check left
                // System.out.println("collidingleft");
                if (isColliding(CONTACT_AREA_LEFT) && !isColliding(CONTACT_AREA_RIGHT)) {
                    // the actor is embedded to the wall
                    adjustHitLeft()
                    veloX = 0f // reset veloX, simulating normal force
                    elasticReflectX()
                }
                else if (isColliding(CONTACT_AREA_LEFT, -1, 0) && !isColliding(CONTACT_AREA_RIGHT, 1, 0)) {
                    // the actor is touching the wall
                    veloX = 0f // reset veloX, simulating normal force
                    elasticReflectX()
                }
                else {
                }
            }
            else { // check both sides?
                // System.out.println("updatehorizontal - |velo| < 0.5");
                //if (isColliding(CONTACT_AREA_LEFT) || isColliding(CONTACT_AREA_RIGHT)) {
                //    veloX = 0f // reset veloX, simulating normal force
                //    elasticReflectX()
                //}
            }
        }
    }

    private fun adjustHitRight() {
        val newY = nextHitbox.posY // look carefully, posY or pointedY
        // int-ify posY of nextHitbox
        nextHitbox.setPositionX(FastMath.floor(nextHitbox.posX + nextHitbox.width) - nextHitbox.width)

        var newXOff = 0 // always positive

        // count up Y offset until the actor is not touching the wall
        var colliding: Boolean
        do {
            newXOff += 1
            colliding = isColliding(CONTACT_AREA_BOTTOM, -newXOff + 1, 0) // offset by +1, to fix directional quarks
        } while (newXOff < TSIZE && colliding)

        val newX = nextHitbox.posX - newXOff // -1: Q&D way to prevent the actor sticking to the wall and won't detach
        nextHitbox.setPosition(newX, newY)
    }

    private fun adjustHitLeft() {
        val newY = nextHitbox.posY
        // int-ify posY of nextHitbox
        nextHitbox.setPositionX(FastMath.ceil(nextHitbox.posX).toFloat())

        var newXOff = 0 // always positive

        // count up Y offset until the actor is not touching the wall
        var colliding: Boolean
        do {
            newXOff += 1
            colliding = isColliding(CONTACT_AREA_TOP, newXOff, 0)
        } while (newXOff < TSIZE && colliding)

        posAdjustX = newXOff
        val newX = nextHitbox.posX + newXOff // +1: Q&D way to prevent the actor sticking to the wall and won't detach
        nextHitbox.setPosition(newX, newY)
    }

    private fun elasticReflectX() {
        if (veloX != 0f && (veloX * elasticity).abs() > 0.5) {
            veloX = -veloX * elasticity

        }
    }

    private fun elasticReflectY() {
        if (veloY != 0f && (veloY * elasticity).abs() > 0.5) {
            veloY = -veloY * elasticity
        }
    }

    private fun isColliding(side: Int, tx: Int = 0, ty: Int = 0): Boolean = getContactingArea(side, tx, ty) > 1

    private fun getContactingArea(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..(if (side % 2 == 0) nextHitbox.width else nextHitbox.height).roundToInt() - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt()
                        + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxEnd.y.roundToInt() + translateY)
            }
            else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt()
                        + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt() + translateY)
            }
            else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxEnd.x.roundToInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt()
                        + i + translateY)
            }
            else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt()
                        + i + translateY)
            }
            else {
                throw IllegalArgumentException(side.toString() + ": Wrong side input")
            }

            // evaluate
            if (TilePropCodex.getProp(map.getTileFromTerrain(tileX, tileY) ?: TileNameCode.STONE).isSolid) {
                contactAreaCounter += 1
            }
        }

        return contactAreaCounter
    }

    private fun getContactingAreaFluid(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..(if (side % 2 == 0) nextHitbox.width else nextHitbox.height).roundToInt() - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxEnd.y.roundToInt() + translateY)
            }
            else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt() + translateY)
            }
            else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxEnd.x.roundToInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt()
                                                 + i + translateY)
            }
            else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundToInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundToInt()
                                                 + i + translateY)
            }
            else {
                throw IllegalArgumentException(side.toString() + ": Wrong side input")
            }

            // evaluate
            if (TilePropCodex.getProp(map.getTileFromTerrain(tileX, tileY)).isFluid) {
                contactAreaCounter += 1
            }
        }

        return contactAreaCounter
    }

    /**
     * [N] = [kg * m / s^2]
     * F(bo) = density * submerged_volume * gravitational_acceleration [N]
     */
    /*private fun applyBuoyancy() {
        val fluidDensity = tileDensity
        val submergedVolume = submergedVolume

        if (!isPlayerNoClip && !grounded) {
            // System.out.println("density: "+density);
            veloY -= ((fluidDensity - this.density).toDouble()
                    * map.gravitation.toDouble() * submergedVolume.toDouble()
                    * Math.pow(mass.toDouble(), -1.0)
                    * SI_TO_GAME_ACC.toDouble()).toFloat()
        }
    }*/

    /*private val submergedVolume: Float
        get() = submergedHeight * hitbox.width * hitbox.width

    private val submergedHeight: Float
        get() = Math.max(
                getContactingAreaFluid(CONTACT_AREA_LEFT),
                getContactingAreaFluid(CONTACT_AREA_RIGHT)
        ).toFloat()*/


    /**
     * Get highest friction value from feet tiles.
     * @return
     */
    private val tileFriction: Int
        get() {
            var friction = 0

            //get highest friction
            val tilePosXStart = (hitbox.posX / TSIZE).roundToInt()
            val tilePosXEnd = (hitbox.hitboxEnd.x / TSIZE).roundToInt()
            val tilePosY = (hitbox.pointedY.plus(1) / TSIZE).roundToInt()
            for (x in tilePosXStart..tilePosXEnd) {
                val tile = map.getTileFromTerrain(x, tilePosY)
                val thisFriction = TilePropCodex.getProp(tile).friction

                if (thisFriction > friction) friction = thisFriction
            }

            return friction
        }
    fun Int.tileFrictionToMult() = this / 16f

    /**
     * Get highest density (specific gravity) value from tiles that the body occupies.
     * @return
     */
    private val tileDensity: Int
        get() {
            var density = 0

            //get highest fluid density
            val tilePosXStart = (nextHitbox.posX / TSIZE).roundToInt()
            val tilePosYStart = (nextHitbox.posY / TSIZE).roundToInt()
            val tilePosXEnd = (nextHitbox.hitboxEnd.x / TSIZE).roundToInt()
            val tilePosYEnd = (nextHitbox.hitboxEnd.y / TSIZE).roundToInt()
            for (y in tilePosYStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)
                    val thisFluidDensity = TilePropCodex.getProp(tile).density

                    if (thisFluidDensity > density) density = thisFluidDensity
                }
            }

            return density
        }

    /**
     * Get highest fluid resistance value from tiles that the body occupies.
     * @return
     */
    private val fluidResistance: Int
        get() {
            var resistance = 0

            //get highest fluid density
            val tilePosXStart = (nextHitbox.posX / TSIZE).roundToInt()
            val tilePosYStart = (nextHitbox.posY / TSIZE).roundToInt()
            val tilePosXEnd = (nextHitbox.hitboxEnd.x / TSIZE).roundToInt()
            val tilePosYEnd = (nextHitbox.hitboxEnd.y / TSIZE).roundToInt()
            for (y in tilePosYStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)

                    if (TilePropCodex.getProp(tile).isFluid) {
                        val thisResistance = TilePropCodex.getProp(tile).movementResistance

                        if (thisResistance > resistance) resistance = thisResistance
                    }
                }
            }

            return resistance
        }
    fun Int.resistanceToMult(): Float = 1f / (1 + this / 16f)

    private fun clampHitbox() {
        hitbox.setPositionFromPoint(
                clampW(hitbox.pointedX), clampH(hitbox.pointedY))
    }

    private fun clampNextHitbox() {
        nextHitbox.setPositionFromPoint(
                clampW(nextHitbox.pointedX), clampH(nextHitbox.pointedY))
    }

    private fun updateNextHitboxFromVelo() {

        nextHitbox.set(
                  (hitbox.posX + veloX)
                , (hitbox.posY + veloY)
                , (baseHitboxW * scale)
                , (baseHitboxH * scale)
        )
    }

    private fun updateHitboxX() {
        hitbox.setDimension(
                nextHitbox.width, nextHitbox.height)
        hitbox.setPositionX(nextHitbox.posX)
    }

    private fun updateHitboxY() {
        hitbox.setDimension(
                nextHitbox.width, nextHitbox.height)
        hitbox.setPositionY(nextHitbox.posY)
    }

    override fun drawGlow(gc: GameContainer, g: Graphics) {
        if (isVisible && spriteGlow != null) {
            if (!sprite!!.flippedHorizontal()) {
                spriteGlow!!.render(g, hitbox.posX - hitboxTranslateX * scale, hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            } else {
                spriteGlow!!.render(g, hitbox.posX - scale, hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            }
        }
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        if (isVisible && sprite != null) {
            if (!sprite!!.flippedHorizontal()) {
                sprite!!.render(g, hitbox.posX - hitboxTranslateX * scale, hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            } else {
                sprite!!.render(g, hitbox.posX - scale, hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            }
        }
    }

    override fun updateGlowSprite(gc: GameContainer, delta: Int) {
        if (spriteGlow != null) spriteGlow!!.update(delta)
    }

    override fun updateBodySprite(gc: GameContainer, delta: Int) {
        if (sprite != null) sprite!!.update(delta)
    }

    private fun clampW(x: Float): Float =
        if (x < TSIZE + nextHitbox.width / 2) {
            TSIZE + nextHitbox.width / 2
        } else if (x >= (map.width * TSIZE).toFloat() - TSIZE.toFloat() - nextHitbox.width / 2) {
            (map.width * TSIZE).toFloat() - 1f - TSIZE.toFloat() - nextHitbox.width / 2
        } else {
            x
        }

    private fun clampH(y: Float): Float =
        if (y < TSIZE + nextHitbox.height) {
            TSIZE + nextHitbox.height
        } else if (y >= (map.height * TSIZE).toFloat() - TSIZE.toFloat() - nextHitbox.height) {
            (map.height * TSIZE).toFloat() - 1f - TSIZE.toFloat() - nextHitbox.height
        } else {
            y
        }

    private fun clampWtile(x: Int): Int =
        if (x < 0) {
            0
        } else if (x >= map.width) {
            map.width - 1
        } else {
            x
        }

    private fun clampHtile(x: Int): Int =
        if (x < 0) {
            0
        } else if (x >= map.height) {
            map.height - 1
        } else {
            x
        }

    private val isPlayerNoClip: Boolean
        get() = this is Player && this.isNoClip()

    private fun quantiseTSize(v: Float): Int = FastMath.floor(v / TSIZE) * TSIZE

    fun setDensity(density: Int) {
        if (density < 0)
            throw IllegalArgumentException("[ActorWithBody] $density: density cannot be negative.")

        this.density = density.toFloat()
    }

    private val AUTO_CLIMB_RATE: Int
        get() = Math.min(TSIZE / 8 * FastMath.sqrt(scale), TSIZE.toFloat()).toInt()

    fun Float.round() = Math.round(this).toFloat()
    fun Float.roundToInt(): Int = Math.round(this)
    fun Float.abs() = FastMath.abs(this)
    fun Int.abs() = if (this < 0) -this else this

    companion object {

        @Transient private val TSIZE = MapDrawer.TILE_SIZE

        private fun div16TruncateToMapWidth(x: Int): Int {
            if (x < 0)
                return 0
            else if (x >= Terrarum.game.map.width shl 4)
                return Terrarum.game.map.width - 1
            else
                return x and 0x7FFFFFFF shr 4
        }

        private fun div16TruncateToMapHeight(y: Int): Int {
            if (y < 0)
                return 0
            else if (y >= Terrarum.game.map.height shl 4)
                return Terrarum.game.map.height - 1
            else
                return y and 0x7FFFFFFF shr 4
        }

        private fun clampCeil(x: Float, ceil: Float): Float {
            return if (Math.abs(x) > ceil) ceil else x
        }
    }
}
/**
 * Give new random ReferenceID and initialise ActorValue
 */

/**

=                  = ↑
===                ===@!
=↑                 =↑
=↑                 =
=↑                 =
=@ (pressing R)    =
==================  ==================

Fig. 1: the fix was not applied
 */