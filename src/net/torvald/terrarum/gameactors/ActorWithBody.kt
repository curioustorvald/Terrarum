package net.torvald.terrarum.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.gamemap.GameMap
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.tileproperties.TilePropCodex
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.tileproperties.TileNameCode
import org.dyn4j.Epsilon
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Base class for every actor that has physical (or visible) body. This includes furnishings, paintings, gadgets, etc.
 *
 * Created by minjaesong on 16-03-14.
 */
open class ActorWithBody : Actor(), Visible {

    override var referenceID: Int = generateUniqueReferenceID()
    override var actorValue: ActorValue = ActorValue()

    @Transient var sprite: SpriteAnimation? = null
    @Transient var spriteGlow: SpriteAnimation? = null

    @Transient private val map: GameMap = Terrarum.game.map

    var hitboxTranslateX: Double = 0.0// relative to spritePosX
    var hitboxTranslateY: Double = 0.0// relative to spritePosY
    var baseHitboxW: Int = 0
    var baseHitboxH: Int = 0
    internal var baseSpriteWidth: Int = 0
    internal var baseSpriteHeight: Int = 0
    /**
     * * Position: top-left point
     * * Unit: pixel
     */
    override val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)
    @Transient val nextHitbox = Hitbox(0.0, 0.0, 0.0, 0.0)

    /**
     * Velocity vector for newtonian sim.
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     */
    internal val velocity = Vector2(0.0, 0.0)
    var veloX: Double
        get() = velocity.x
        private set(value) { velocity.x = value }
    var veloY: Double
        get() = velocity.y
        private set(value) { velocity.y = value }
    var walkX: Double = 0.0
    var walkY: Double = 0.0
    val moveDelta = Vector2(0.0, 0.0)
    @Transient private val VELO_HARD_LIMIT = 100.0

    /**
     * Physical properties.
     * Values derived from ActorValue must be @Transient.
     */
    var scale: Double
        get() = actorValue.getAsDouble(AVKey.SCALE) ?: 1.0
        set(value) = actorValue.set(AVKey.SCALE, value)
    @Transient val MASS_LOWEST = 0.1 // Kilograms
    var mass: Double
        get() = actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT * Math.pow(scale, 3.0)
        set(value) {
            if (value <= 0)
                throw IllegalArgumentException("mass cannot be less than or equal to zero.")
            else if (value < MASS_LOWEST) {
                println("[ActorWithBody] input too small; using $MASS_LOWEST instead.")
                actorValue[AVKey.BASEMASS] = MASS_LOWEST
            }

            actorValue[AVKey.BASEMASS] = value
        }
    @Transient private val MASS_DEFAULT: Double = 60.0
    /** Valid range: [0, 1]  */
    var elasticity: Double = 0.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("invalid elasticity value $value; valid elasticity value is [0, 1].")
            else if (value >= ELASTICITY_MAX) {
                println("[ActorWithBody] Elasticity were capped to $ELASTICITY_MAX.")
                field = ELASTICITY_MAX
            }
            else
                field = value * ELASTICITY_MAX
        }
    @Transient private val ELASTICITY_MAX = 0.993 // No perpetual motion!

    /**
     * what pretty much every physics engine has, instead of my 'elasticity'
     *
     * This is just a simple macro for 'elasticity'.
     *
     * Formula: restitution = 1.0 - elasticity
     */
    var restitution: Double
        set(value) { elasticity = 1.0 - value }
        get() = 1.0 - elasticity

    private var density = 1000.0

    /**
     * Flags and Properties
     */

    var grounded = false
    /** Default to 'false'  */
    var isVisible = false
    /** Default to 'true'  */
    var isUpdate = true
    var isNoSubjectToGrav = false
    var isNoCollideWorld = false
    var isNoSubjectToFluidResistance = false
    /** Time-freezing. The actor won't even budge but the velocity will accumulate
     * EXCEPT FOR friction, gravity and buoyancy SHOULD NOT.
     *
     * (this would give something to do to the player otherwise it would be dull to travel far,
     * think of a grass cutting on the Zelda games. It would also make a great puzzle to solve.
     * --minjaesong)
     */
    var isChronostasis = false

    /**
     * Constants
     */

    @Transient private val METER = 24.0
    /**
     * [m / s^2] * SI_TO_GAME_ACC -> [px / InternalFrame^2]
     */
    @Transient private val SI_TO_GAME_ACC = METER / (Terrarum.TARGET_FPS * Terrarum.TARGET_FPS).toDouble()
    /**
     * [m / s] * SI_TO_GAME_VEL -> [px / InternalFrame]
     */
    @Transient private val SI_TO_GAME_VEL = METER / Terrarum.TARGET_FPS
    /**
     * Gravitational Constant G. Load from gamemap.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    @Transient private val gravitation: Vector2 = map.gravitation
    @Transient val DRAG_COEFF_DEFAULT = 1.2
    /** Drag coefficient. Parachutes have much higher value than bare body (1.2) */
    private var DRAG_COEFF: Double
        get() = actorValue.getAsDouble(AVKey.DRAGCOEFF) ?: DRAG_COEFF_DEFAULT
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithBody] drag coefficient cannot be negative.")
            actorValue[AVKey.DRAGCOEFF] = value
        }

    @Transient private val UD_COMPENSATOR_MAX = TSIZE
    @Transient private val LR_COMPENSATOR_MAX = TSIZE

    /**
     * Post-hit invincibility, in milliseconds
     */
    @Transient val INVINCIBILITY_TIME: Int = 500

    @Transient internal val BASE_FRICTION = 0.3

    @Transient val KINEMATIC = 1 // does not be budged by external forces
    @Transient val DYNAMIC = 2
    @Transient val STATIC = 3 // does not be budged by external forces, target of collision
    var collisionType = DYNAMIC

    @Transient private val CCD_TICK = 1.0 / 16.0
    @Transient private val CCD_TRY_MAX = 12800

    // just some trivial magic numbers
    @Transient private val A_PIXEL = 1.4
    @Transient private val COLLIDING_TOP = 0
    @Transient private val COLLIDING_RIGHT = 1
    @Transient private val COLLIDING_BOTTOM = 2
    @Transient private val COLLIDING_LEFT = 3
    @Transient private val COLLIDING_UD = 4
    @Transient private val COLLIDING_LR = 5
    @Transient private val COLLIDING_ALLSIDE = 6

    /**
     * Temporary variables
     */

    @Transient private var assertPrinted = false

    // to use with Controller (incl. player)
    internal var walledLeft = false
    internal var walledRight = false

    var ccdCollided = false

    init {
        // some initialiser goes here...
    }

    /**
     * @param w
     * @param h
     * @param tx +: translate drawn sprite to LEFT.
     * @param ty +: translate drawn sprite to DOWN.
     * @see ActorWithBody.drawBody
     * @see ActorWithBody.drawGlow
     */
    fun setHitboxDimension(w: Int, h: Int, tx: Int, ty: Int) {
        baseHitboxH = h
        baseHitboxW = w
        hitboxTranslateX = tx.toDouble()
        hitboxTranslateY = ty.toDouble()
    }

    /**
     * Set hitbox position from bottom-center point
     * @param x
     * @param y
     */
    fun setPosition(x: Double, y: Double) {
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

    override fun run() = update(Terrarum.appgc, Terrarum.game.DELTA_T)

    /**
     * Add vector value to the velocity, in the time unit of single frame.
     *
     * Since we're adding some value every frame, the value is equivalent to the acceleration.
     * Look for Newton's second law for the background knowledge.
     * @param vec : Acceleration in Vector2
     */
    fun applyForce(vec: Vector2) {
        velocity += vec
    }

    override fun update(gc: GameContainer, delta: Int) {
        if (isUpdate) {

            /**
             * Temporary variables to reset
             */
            ccdCollided = false
            /******************************/

            if (!assertPrinted) assertInit()

            // make NoClip work for player
            if (this is Player) {
                isNoSubjectToGrav = isPlayerNoClip
                isNoCollideWorld = isPlayerNoClip
                isNoSubjectToFluidResistance = isPlayerNoClip
            }

            // set sprite dimension vars if there IS sprite for the actor
            if (sprite != null) {
                baseSpriteHeight = sprite!!.height
                baseSpriteWidth = sprite!!.width
            }

            /**
             * Actual physics thing (altering velocity) starts from here
             */
            // Actors are subject to the gravity and the buoyancy if they are not levitating
            if (!isNoSubjectToGrav) {
                applyGravitation()
                //applyBuoyancy()
            }

            // hard limit velocity
            veloX = veloX.bipolarClamp(VELO_HARD_LIMIT)
            veloY = veloY.bipolarClamp(VELO_HARD_LIMIT)

            moveDelta.set(velocity + Vector2(walkX, walkY))

            if (!isChronostasis) {
                // Set 'next' position (hitbox) from canonical and walking velocity
                setNewNextHitbox()

                /**
                 * solveCollision()?
                 * If and only if:
                 *     This body is NON-STATIC and the other body is STATIC
                 */
                displaceByCCD()
                applyNormalForce()

                setHorizontalFriction()
                if (isPlayerNoClip) // or hanging on the rope, etc.
                    setVerticalFriction()

                // apply our compensation to actual hitbox
                updateHitbox()

                // make sure the actor does not go out of the map
                clampHitbox()
            }

            walledLeft =  isColliding(hitbox, COLLIDING_LEFT)
            walledRight = isColliding(hitbox, COLLIDING_RIGHT)
        }
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is precessed separately.
     */
    private fun applyGravitation() {
        if (!grounded) {//(!isColliding(COLLIDING_BOTTOM)) { // or !grounded
            /**
             * weight; gravitational force in action
             * W = mass * G (9.8 [m/s^2])
             */
            val W: Vector2 = gravitation * mass
            /**
             * Area
             */
            val A: Double = scale * scale
            /**
             * Drag of atmosphere
             * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity sqr) * A (area)
             */
            val D: Vector2 = velocity * DRAG_COEFF * 0.5 * A// * tileDensityFluid.toDouble()

            val V: Vector2 = (W - D) / mass * SI_TO_GAME_ACC

            applyForce(V)
        }
    }

    private fun applyNormalForce() {
        if (!isNoCollideWorld) {
            // axis Y
            if (moveDelta.y >= 0.0) { // was moving downward?
                if (ccdCollided) { // actor hit something on its bottom
                    hitAndReflectY()
                    grounded = true
                }
                else { // the actor is not grounded at all
                    grounded = false
                }
            }
            else if (moveDelta.y < 0.0) { // or was moving upward?
                grounded = false
                if (ccdCollided) { // actor hit something on its top
                    hitAndReflectY()
                }
                else { // the actor is not grounded at all
                }
            }
            // axis X
            if (ccdCollided && moveDelta.x != 0.0) { // check right and left
                // the actor is hitting the wall
                hitAndReflectX()
            }
        }
    }

    /**
     * nextHitbox must NOT be altered before this method is called!
     */
    private fun displaceByCCD() {
        if (!isNoCollideWorld){
            // do some CCD between hitbox and nextHitbox
            val ccdBox = hitbox.clone()
            val ccdDelta = hitbox.toVector() to nextHitbox.toVector()
            val ccdStep = Math.max(ccdDelta.x, ccdDelta.y)

            for (step in 0..ccdStep.floorInt()) {
                ccdBox.translate(ccdDelta * (step.toDouble() / ccdStep))

                println(ccdDelta * (step.toDouble() / ccdStep))
                println(ccdBox)

                if (isColliding(ccdBox)) {
                    ccdCollided = true
                    break
                }
            }

            nextHitbox.reassign(ccdBox)
        }
        else {
            ccdCollided = false
        }
    }

    private fun hitAndReflectX() {
        if ((veloX * elasticity).abs() > Epsilon.E) {
            veloX *= -elasticity
            walkX *= -elasticity
        }
        else {
            veloX = 0.0
            walkX = 0.0
        }
    }

    private fun hitAndReflectY() {
        if ((veloY * elasticity).abs() > Epsilon.E) {
          veloY *= -elasticity
          walkY *= -elasticity
        }
        else {
            veloY = 0.0
            walkY *= 0.0
        }
    }

    private fun isColliding(hitbox: Hitbox) = isColliding(hitbox, 0)

    private fun isColliding(hitbox: Hitbox, option: Int): Boolean {
        if (isNoCollideWorld) return false

        // offsets will stretch and shrink detection box according to the argument
        val x1: Double; val x2: Double; val y1: Double; val y2: Double
        if (option == COLLIDING_LR || option == COLLIDING_UD) {
            val offsetX = if (option == COLLIDING_LR) A_PIXEL else 0.0
            val offsetY = if (option == COLLIDING_UD) A_PIXEL else 0.0

            x1 = hitbox.posX - offsetX + offsetY
            x2 = hitbox.posX + offsetX - offsetY + hitbox.width
            y1 = hitbox.posY + offsetX - offsetY
            y2 = hitbox.posY - offsetX + offsetY + hitbox.height
        }
        else {
            if (option == COLLIDING_LEFT) {
                x1 = hitbox.posX - A_PIXEL
                x2 = hitbox.posX - A_PIXEL + hitbox.width
                y1 = hitbox.posY + A_PIXEL
                y2 = hitbox.posY - A_PIXEL + hitbox.height
            }
            else if (option == COLLIDING_RIGHT) {
                x1 = hitbox.posX + A_PIXEL
                x2 = hitbox.posX + A_PIXEL + hitbox.width
                y1 = hitbox.posY + A_PIXEL
                y2 = hitbox.posY - A_PIXEL + hitbox.height
            }
            else if (option == COLLIDING_TOP) {
                x1 = hitbox.posX + A_PIXEL
                x2 = hitbox.posX - A_PIXEL + hitbox.width
                y1 = hitbox.posY - A_PIXEL
                y2 = hitbox.posY - A_PIXEL + hitbox.height
            }
            else if (option == COLLIDING_BOTTOM) {
                x1 = hitbox.posX + A_PIXEL
                x2 = hitbox.posX - A_PIXEL + hitbox.width
                y1 = hitbox.posY + A_PIXEL
                y2 = hitbox.posY + A_PIXEL + hitbox.height
            }
            else {
                x1 = hitbox.posX
                x2 = hitbox.posX + hitbox.width
                y1 = hitbox.posY
                y2 = hitbox.posY + hitbox.height
            }
        }

        val txStart = x1.div(TSIZE).floorInt()
        val txEnd = x2.div(TSIZE).floorInt()
        val tyStart = y1.div(TSIZE).floorInt()
        val tyEnd = y2.div(TSIZE).floorInt()

        for (y in tyStart..tyEnd) {
            for (x in txStart..txEnd) {
                val tile = map.getTileFromTerrain(x, y)
                if (TilePropCodex.getProp(tile).isSolid)
                    return true
            }
        }

        return false
    }

    private fun getContactingAreaFluid(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..(if (side % 2 == 0) nextHitbox.width else nextHitbox.height).roundInt() - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == COLLIDING_LEFT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxEnd.y.roundInt() + translateY)
            }
            else if (side == COLLIDING_TOP) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundInt() + translateY)
            }
            else if (side == COLLIDING_RIGHT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxEnd.x.roundInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundInt()
                                                 + i + translateY)
            }
            else if (side == COLLIDING_LEFT) {
                tileX = div16TruncateToMapWidth(nextHitbox.hitboxStart.x.roundInt() + translateX)
                tileY = div16TruncateToMapHeight(nextHitbox.hitboxStart.y.roundInt()
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

    private fun setHorizontalFriction() {
        val friction = if (isPlayerNoClip)
            BASE_FRICTION * TilePropCodex.getProp(TileNameCode.STONE).friction.tileFrictionToMult()
        else
            BASE_FRICTION * tileFriction.tileFrictionToMult()

        if (veloX < 0) {
            veloX += friction
            if (veloX > 0) veloX = 0.0 // compensate overshoot
        }
        else if (veloX > 0) {
            veloX -= friction
            if (veloX < 0) veloX = 0.0 // compensate overshoot
        }

        if (walkX < 0) {
            walkX += friction
            if (walkX > 0) walkX = 0.0
        }
        else if (walkX > 0) {
            walkX -= friction
            if (walkX < 0) walkX = 0.0
        }
    }

    private fun setVerticalFriction() {
        val friction = if (isPlayerNoClip)
            BASE_FRICTION * TilePropCodex.getProp(TileNameCode.STONE).friction.tileFrictionToMult()
        else
            BASE_FRICTION * tileFriction.tileFrictionToMult()

        if (veloY < 0) {
            veloY += friction
            if (veloY > 0) veloX = 0.0 // compensate overshoot
        }
        else if (veloY > 0) {
            veloY -= friction
            if (veloY < 0) veloY = 0.0 // compensate overshoot
        }

        if (walkY < 0) {
            walkY += friction
            if (walkY > 0) walkY = 0.0
        }
        else if (walkY > 0) {
            walkY -= friction
            if (walkY < 0) walkY = 0.0
        }
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
                    * SI_TO_GAME_ACC.toDouble()).toDouble()
        }
    }*/

    /*private val submergedVolume: Double
        get() = submergedHeight * hitbox.width * hitbox.width

    private val submergedHeight: Double
        get() = Math.max(
                getContactingAreaFluid(COLLIDING_LEFT),
                getContactingAreaFluid(COLLIDING_RIGHT)
        ).toDouble()*/


    /**
     * Get highest friction value from feet tiles.
     * @return
     */
    internal val tileFriction: Int
        get() {
            var friction = 0

            // take highest value
            val tilePosXStart = (hitbox.posX / TSIZE).roundInt()
            val tilePosXEnd = (hitbox.hitboxEnd.x / TSIZE).roundInt()
            val tilePosY = (hitbox.pointedY.plus(1) / TSIZE).roundInt()
            for (x in tilePosXStart..tilePosXEnd) {
                val tile = map.getTileFromTerrain(x, tilePosY)
                val thisFriction = TilePropCodex.getProp(tile).friction

                if (thisFriction > friction) friction = thisFriction
            }

            return friction
        }
    fun Int.tileFrictionToMult() = this / 16.0

    /**
     * Get highest tile density from occupying tiles, fluid only
     */
    private val tileDensityFluid: Int
        get() {
            var density = 0

            // take highest value
            val tilePosXStart = (hitbox.posX / TSIZE).roundInt()
            val tilePosXEnd = (hitbox.hitboxEnd.x / TSIZE).roundInt()
            val tilePosYStart = (hitbox.posY / TSIZE).roundInt()
            val tilePosYEnd = (hitbox.hitboxEnd.y / TSIZE).roundInt()
            for (y in tilePosXStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)
                    val prop = TilePropCodex.getProp(tile)

                    if (prop.isFluid && prop.density > density)
                        density = prop.density
                }
            }
            return density
        }

    /**
     * Get highest density (specific gravity) value from tiles that the body occupies.
     * @return
     */
    private val tileDensity: Int
        get() {
            var density = 0

            //get highest fluid density
            val tilePosXStart = (nextHitbox.posX / TSIZE).roundInt()
            val tilePosYStart = (nextHitbox.posY / TSIZE).roundInt()
            val tilePosXEnd = (nextHitbox.hitboxEnd.x / TSIZE).roundInt()
            val tilePosYEnd = (nextHitbox.hitboxEnd.y / TSIZE).roundInt()
            for (y in tilePosYStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)
                    val thisFluidDensity = TilePropCodex.getProp(tile).density

                    if (thisFluidDensity > density) density = thisFluidDensity
                }
            }

            return density
        }

    private fun clampHitbox() {
        hitbox.setPositionFromPoint(
                clampW(hitbox.pointedX), clampH(hitbox.pointedY))
    }

    private fun setNewNextHitbox() {
        nextHitbox.set(
                hitbox.posX + moveDelta.x,
                hitbox.posY + moveDelta.y,
                baseHitboxW * scale,
                baseHitboxH * scale
        )
    }

    private fun updateHitbox() = hitbox.reassign(nextHitbox)

    override fun drawGlow(gc: GameContainer, g: Graphics) {
        if (isVisible && spriteGlow != null) {
            if (!sprite!!.flippedHorizontal()) {
                spriteGlow!!.render(g,
                        (hitbox.posX - hitboxTranslateX * scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            } else {
                spriteGlow!!.render(g,
                        (hitbox.posX - scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
        }
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        if (isVisible && sprite != null) {
            if (!sprite!!.flippedHorizontal()) {
                sprite!!.render(g,
                        (hitbox.posX - hitboxTranslateX * scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            } else {
                sprite!!.render(g,
                        (hitbox.posX - scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
        }
    }

    override fun updateGlowSprite(gc: GameContainer, delta: Int) {
        if (spriteGlow != null) spriteGlow!!.update(delta)
    }

    override fun updateBodySprite(gc: GameContainer, delta: Int) {
        if (sprite != null) sprite!!.update(delta)
    }

    private fun clampW(x: Double): Double =
        if (x < TSIZE + nextHitbox.width / 2) {
            TSIZE + nextHitbox.width / 2
        } else if (x >= (map.width * TSIZE).toDouble() - TSIZE.toDouble() - nextHitbox.width / 2) {
            (map.width * TSIZE).toDouble() - 1.0 - TSIZE.toDouble() - nextHitbox.width / 2
        } else {
            x
        }

    private fun clampH(y: Double): Double =
        if (y < TSIZE + nextHitbox.height) {
            TSIZE + nextHitbox.height
        } else if (y >= (map.height * TSIZE).toDouble() - TSIZE.toDouble() - nextHitbox.height) {
            (map.height * TSIZE).toDouble() - 1.0 - TSIZE.toDouble() - nextHitbox.height
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

    fun setDensity(density: Int) {
        if (density < 0)
            throw IllegalArgumentException("[ActorWithBody] $density: density cannot be negative.")

        this.density = density.toDouble()
    }

    private val AUTO_CLIMB_RATE: Int
        get() = Math.min(TSIZE / 8 * Math.sqrt(scale), TSIZE.toDouble()).toInt()

    fun Double.floorInt() = Math.floor(this).toInt()
    fun Double.round() = Math.round(this).toDouble()
    fun Double.floor() = Math.floor(this)
    fun Double.ceil() = this.floor() + 1.0
    fun Double.roundInt(): Int = Math.round(this).toInt()
    fun Double.abs() = Math.abs(this)
    fun Double.sqr() = this * this
    fun Int.abs() = if (this < 0) -this else this
    fun Double.bipolarClamp(limit: Double) =
            if      (this > 0 && this > limit)  limit
            else if (this < 0 && this < -limit) -limit
            else this

    private fun assertInit() {
        // errors
        if (baseHitboxW == 0 || baseHitboxH == 0)
            throw RuntimeException("Hitbox dimension was not set.")
        if (sprite == null && isVisible)
            throw RuntimeException("Actor ${this.javaClass.canonicalName} is visible but the sprite was not set.")
        // warnings
        if (!isVisible && sprite != null)
            println("[ActorWithBody] Caution: actor ${this.javaClass.canonicalName} is invisible but the sprite was given.")

        assertPrinted = true
    }

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

        private fun clampCeil(x: Double, ceil: Double): Double {
            return if (Math.abs(x) > ceil) ceil else x
        }
    }
}
