package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.point.Point2d
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.tileproperties.TilePropCodex
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.mapdrawer.MapDrawer.TILE_SIZE
import net.torvald.terrarum.tileproperties.TileNameCode
import net.torvald.terrarum.tileproperties.TileProp
import org.dyn4j.Epsilon
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Base class for every actor that has physical (or echo) body. This includes furnishings, paintings, gadgets, etc.
 *
 * Created by minjaesong on 16-01-13.
 */
open class ActorWithBody : Actor() {

    override var referenceID: Int = generateUniqueReferenceID()
    override var actorValue: ActorValue = ActorValue()

    @Transient internal var sprite: SpriteAnimation? = null
    @Transient internal var spriteGlow: SpriteAnimation? = null

    protected var drawMode: DrawMode = DrawMode.NORMAL

    @Transient private val world: GameWorld = Terrarum.ingame.world

    protected var hitboxTranslateX: Double = 0.0// relative to spritePosX
    protected var hitboxTranslateY: Double = 0.0// relative to spritePosY
    protected var baseHitboxW: Int = 0
    protected var baseHitboxH: Int = 0
    protected var baseSpriteWidth: Int = 0
    protected var baseSpriteHeight: Int = 0
    /**
     * * Position: top-left point
     * * Unit: pixel
     * !! external class should not hitbox.set(); use setHitboxDimension() and setPosition()
     */
    val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)       // Hitbox is implemented using Double;
    @Transient val nextHitbox = Hitbox(0.0, 0.0, 0.0, 0.0) // 52 mantissas ought to be enough for anybody...

    val tilewiseHitbox: Hitbox
        get() = Hitbox.fromTwoPoints(
                nextHitbox.posX.div(TILE_SIZE).floor(),
                nextHitbox.posY.div(TILE_SIZE).floor(),
                nextHitbox.endPointX.div(TILE_SIZE).floor(),
                nextHitbox.endPointY.div(TILE_SIZE).floor()
        )

    /**
     * Velocity vector for newtonian sim.
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     */
    internal val velocity = Vector2(0.0, 0.0)
    var veloX: Double
        get() = velocity.x
        protected set(value) {
            velocity.x = value
        }
    var veloY: Double
        get() = velocity.y
        protected set(value) {
            velocity.y = value
        }

    val moveDelta = Vector2(0.0, 0.0)
    @Transient private val VELO_HARD_LIMIT = 100.0

    /**
     * for "Controllable" actors
     */
    var controllerVel: Vector2? = if (this is Controllable) Vector2() else null
    var walkX: Double
        get() = controllerVel!!.x
        protected set(value) {
            controllerVel!!.x = value
        }
    var walkY: Double
        get() = controllerVel!!.y
        protected set(value) {
            controllerVel!!.y = value
        }

    /**
     * Physical properties.
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
        set(value) {
            elasticity = 1.0 - value
        }
        get() = 1.0 - elasticity

    @Transient private val CEILING_HIT_ELASTICITY = 0.3

    var density = 1000.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithBody] $value: density cannot be negative.")

            field = value
        }

    /**
     * Flags and Properties
     */


    @Volatile var grounded = false
    override @Volatile var flagDespawn = false
    /** Default to 'false'  */
    var isVisible = false
    /** Default to 'true'  */
    var isUpdate = true
    var isNoSubjectToGrav = false
    var isNoCollideWorld = false
    var isNoSubjectToFluidResistance = false
    /** Time-freezing. The actor won't even budge but the velocity will accumulate
     * EXCEPT FOR friction, gravity and buoyancy.
     *
     * (this would give something to do to the player otherwise it would be dull to travel far,
     * think of a grass cutting on the Zelda games. It would also make a great puzzle to solve.
     * --minjaesong)
     */
    @Volatile var isChronostasis = false

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
     * Gravitational Constant G. Load from gameworld.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    @Transient private val gravitation: Vector2 = world.gravitation
    @Transient val DRAG_COEFF_DEFAULT = 1.2
    /** Drag coefficient. Parachutes have much higher value than bare body (1.2) */
    var dragCoefficient: Double
        get() = actorValue.getAsDouble(AVKey.DRAGCOEFF) ?: DRAG_COEFF_DEFAULT
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithBody] drag coefficient cannot be negative.")
            actorValue[AVKey.DRAGCOEFF] = value
        }

    @Transient private val UD_COMPENSATOR_MAX = TILE_SIZE
    @Transient private val LR_COMPENSATOR_MAX = TILE_SIZE

    /**
     * Post-hit invincibility, in milliseconds
     */
    @Transient val INVINCIBILITY_TIME: Int = 500

    @Transient internal val BASE_FRICTION = 0.3

    @Transient val KINEMATIC = 1 // does not displaced by external forces
    @Transient val DYNAMIC = 2   // displaced by external forces
    @Transient val STATIC = 3    // does not displaced by external forces, target of collision
    var collisionType = DYNAMIC

    @Transient private val CCD_TICK = 1.0 / 16.0
    @Transient private val CCD_TRY_MAX = 12800

    // just some trivial magic numbers
    @Transient private val A_PIXEL = 1.0
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
    internal @Volatile var walledLeft = false
    internal @Volatile var walledRight = false

    protected val gameContainer: GameContainer
        get() = Terrarum.appgc
    protected val updateDelta: Int
        get() = Terrarum.ingame.UPDATE_DELTA

    /**
     * true: This actor had just made collision
     */
    var ccdCollided = false
        private set

    var isWalkingH = false
    var isWalkingV = false

    init {
        // some initialiser goes here...
    }

    fun makeNewSprite(w: Int, h: Int) {
        sprite = SpriteAnimation()
        sprite!!.setDimension(w, h)
    }

    fun makeNewSpriteGlow(w: Int, h: Int) {
        spriteGlow = SpriteAnimation()
        spriteGlow!!.setDimension(w, h)
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
        hitbox.setFromWidthHeight(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale,
                y - (baseHitboxH - hitboxTranslateY) * scale,
                baseHitboxW * scale,
                baseHitboxH * scale)

        nextHitbox.setFromWidthHeight(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale,
                y - (baseHitboxH - hitboxTranslateY) * scale,
                baseHitboxW * scale,
                baseHitboxH * scale)
    }

    val centrePosition: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.centeredY)
    val centrePosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.centeredY)
    val feetPosition: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.posY + hitbox.height)

    override fun run() = update(gameContainer, updateDelta)

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
        if (isUpdate && !flagDespawn) {

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

            // Combine velo and walk
            applyMovementVelocity()

            // applyBuoyancy()

            if (!isChronostasis) {
                // Actors are subject to the gravity and the buoyancy if they are not levitating
                if (!isNoSubjectToGrav) {
                    applyGravitation()
                }

                // hard limit velocity
                veloX = veloX.bipolarClamp(VELO_HARD_LIMIT)
                veloY = veloY.bipolarClamp(VELO_HARD_LIMIT)

                // Set 'next' position (hitbox) from canonical and walking velocity
                setNewNextHitbox()

                /**
                 * solveCollision()?
                 * If and only if:
                 *     This body is NON-STATIC and the other body is STATIC
                 */
                if (!isPlayerNoClip) {
                    displaceByCCD()
                    applyNormalForce()
                }

                setHorizontalFriction()
                if (isPlayerNoClip) // or hanging on the rope, etc.
                    setVerticalFriction()

                // apply our compensation to actual hitbox
                updateHitbox()

                // make sure if the actor tries to go out of the map, loop back instead
                clampHitbox()
            }

            // cheap solution for sticking into the wall while Left or Right is held
            walledLeft = isTouchingSide(nextHitbox, COLLIDING_LEFT)
            walledRight = isTouchingSide(nextHitbox, COLLIDING_RIGHT)
            if (isPlayerNoClip) {
                walledLeft = false
                walledRight = false
            }
        }
    }

    private fun applyMovementVelocity() {
        if (this is Controllable) {
            // decide whether to ignore walkX
            if (!(isCollidingSide(hitbox, COLLIDING_LEFT) && walkX < 0)
                || !(isCollidingSide(hitbox, COLLIDING_RIGHT) && walkX > 0)
            ) {
                moveDelta.x = veloX + walkX
            }

            // decide whether to ignore walkY
            if (!(isCollidingSide(hitbox, COLLIDING_TOP) && walkY < 0)
                || !(isCollidingSide(hitbox, COLLIDING_BOTTOM) && walkY > 0)
            ) {
                moveDelta.y = veloY + walkY
            }
        }
        else {
            if (!isCollidingSide(hitbox, COLLIDING_LEFT)
                || !isCollidingSide(hitbox, COLLIDING_RIGHT)
            ) {
                moveDelta.x = veloX
            }

            // decide whether to ignore walkY
            if (!isCollidingSide(hitbox, COLLIDING_TOP)
                || !isCollidingSide(hitbox, COLLIDING_BOTTOM)
            ) {
                moveDelta.y = veloY
            }
        }
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is precessed separately.
     */
    private fun applyGravitation() {
        if (!isTouchingSide(hitbox, COLLIDING_BOTTOM)) {
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
            val D: Vector2 = Vector2(veloX.magnSqr(), veloY.magnSqr()) * dragCoefficient * 0.5 * A// * tileDensityFluid.toDouble()

            val V: Vector2 = (W - D) / mass * SI_TO_GAME_ACC

            applyForce(V)
        }
    }

    private fun applyNormalForce() {
        if (!isNoCollideWorld) {
            // axis Y. Using operand >= and hitting the ceiling will lock the player to the position
            if (moveDelta.y > 0.0) { // was moving downward?
                if (isColliding(nextHitbox, COLLIDING_TOP)) { // hit the ceiling
                    hitAndForciblyReflectY()
                    grounded = false
                }
                else if (isColliding(nextHitbox)) { // FIXME if standing: standard box, if walking: top-squished box
                    hitAndReflectY()
                    grounded = true
                }
                else if (isTouchingSide(nextHitbox, COLLIDING_BOTTOM) && !isColliding(nextHitbox)) { // actor hit something on its bottom
                    hitAndReflectY()
                    grounded = true
                }
                else { // the actor is not grounded at all
                    grounded = false
                }
            }
            else if (moveDelta.y < 0.0) { // or was moving upward?
                grounded = false
                if (isTouchingSide(nextHitbox, COLLIDING_TOP)) { // actor hit something on its top
                    hitAndForciblyReflectY() // prevents sticking to the ceiling
                }
                else { // the actor is not grounded at all
                }
            }
            // axis X
            if (isTouchingSide(nextHitbox, COLLIDING_LEFT) || isTouchingSide(nextHitbox, COLLIDING_RIGHT)
                && moveDelta.x != 0.0) { // check right and left
                // the actor is hitting the wall
                hitAndReflectX()
            }
        }
    }

    /**
     * nextHitbox must NOT be altered before this method is called!
     */
    private fun displaceByCCD() {
        ccdCollided = false

        if (!isNoCollideWorld) {
            if (!isColliding(nextHitbox, COLLIDING_ALLSIDE))
                return

            // do some CCD between hitbox and nextHitbox
            val ccdDelta = (nextHitbox.toVector() - hitbox.toVector())
            if (ccdDelta.x != 0.0 || ccdDelta.y != 0.0)
                ccdDelta.set(ccdDelta.setMagnitude(CCD_TICK))

            //println("deltaMax: $deltaMax")
            //println("ccdDelta: $ccdDelta")

            while (!ccdDelta.isZero && isColliding(nextHitbox, COLLIDING_ALLSIDE)) {
                nextHitbox.translate(-ccdDelta)
                ccdCollided = true
            }

            //println("ccdCollided: $ccdCollided")
        }
    }

    private fun hitAndReflectX() {
        if ((veloX * elasticity).abs() > Epsilon.E) {
            veloX *= -elasticity
            if (this is Controllable) walkX *= -elasticity
        }
        else {
            veloX = 0.0
            if (this is Controllable) walkX = 0.0
        }
    }

    private fun hitAndReflectY() {
        if ((veloY * elasticity).abs() > Epsilon.E) {
            veloY *= -elasticity
            if (this is Controllable) walkY *= -elasticity
        }
        else {
            veloY = 0.0
            if (this is Controllable) walkY *= 0.0
        }
    }

    /**
     * prevents sticking to the ceiling
     */
    private fun hitAndForciblyReflectY() {
        if (veloY.abs() * CEILING_HIT_ELASTICITY > A_PIXEL)
            veloY = -veloY * CEILING_HIT_ELASTICITY
        else
            veloY = veloY.sign() * -A_PIXEL
    }

    private fun isColliding(hitbox: Hitbox) = isColliding(hitbox, 0)

    private fun isColliding(hitbox: Hitbox, option: Int): Boolean {
        if (isNoCollideWorld) return false

        // offsets will stretch and shrink detection box according to the argument
        val x1: Double;
        val x2: Double;
        val y1: Double;
        val y2: Double
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

        val txStart = x1.div(TILE_SIZE).floorInt()
        val txEnd = x2.div(TILE_SIZE).floorInt()
        val tyStart = y1.div(TILE_SIZE).floorInt()
        val tyEnd = y2.div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }

    private fun isTouchingSide(hitbox: Hitbox, option: Int): Boolean {
        val x1: Double;
        val x2: Double;
        val y1: Double;
        val y2: Double
        if (option == COLLIDING_TOP) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX
            y1 = hitbox.posY - A_PIXEL
            y2 = y1
        }
        else if (option == COLLIDING_BOTTOM) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX
            y1 = hitbox.endPointY + A_PIXEL
            y2 = y1
        }
        else if (option == COLLIDING_LEFT) {
            x1 = hitbox.posX - A_PIXEL
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY
        }
        else if (option == COLLIDING_RIGHT) {
            x1 = hitbox.endPointX + A_PIXEL
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY
        }
        else throw IllegalArgumentException()

        val txStart = x1.div(TILE_SIZE).floorInt()
        val txEnd = x2.div(TILE_SIZE).floorInt()
        val tyStart = y1.div(TILE_SIZE).floorInt()
        val tyEnd = y2.div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }


    private fun isCollidingSide(hitbox: Hitbox, option: Int): Boolean {
        val x1: Double;
        val x2: Double;
        val y1: Double;
        val y2: Double
        if (option == COLLIDING_TOP) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX
            y1 = hitbox.posY
            y2 = y1
        }
        else if (option == COLLIDING_BOTTOM) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX
            y1 = hitbox.endPointY
            y2 = y1
        }
        else if (option == COLLIDING_LEFT) {
            x1 = hitbox.posX
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY
        }
        else if (option == COLLIDING_RIGHT) {
            x1 = hitbox.endPointX
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY
        }
        else throw IllegalArgumentException()

        val txStart = x1.div(TILE_SIZE).roundInt()
        val txEnd = x2.div(TILE_SIZE).roundInt()
        val tyStart = y1.div(TILE_SIZE).roundInt()
        val tyEnd = y2.div(TILE_SIZE).roundInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }

    private fun isCollidingInternal(txStart: Int, tyStart: Int, txEnd: Int, tyEnd: Int): Boolean {
        for (y in tyStart..tyEnd) {
            for (x in txStart..txEnd) {
                val tile = world.getTileFromTerrain(x, y)
                if (TilePropCodex[tile].isSolid)
                    return true
            }
        }

        return false

        // test code
        //return if (tyEnd < 348) false else true
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
            if (TilePropCodex[world.getTileFromTerrain(tileX, tileY)].isFluid) {
                contactAreaCounter += 1
            }
        }

        return contactAreaCounter
    }

    private fun setHorizontalFriction() {
        val friction = if (isPlayerNoClip)
            BASE_FRICTION * TilePropCodex[TileNameCode.STONE].friction.tileFrictionToMult()
        else
            BASE_FRICTION * bodyFriction.tileFrictionToMult()

        if (veloX < 0) {
            veloX += friction
            if (veloX > 0) veloX = 0.0 // compensate overshoot
        }
        else if (veloX > 0) {
            veloX -= friction
            if (veloX < 0) veloX = 0.0 // compensate overshoot
        }

        if (this is Controllable) {
            if (walkX < 0) {
                walkX += friction
                if (walkX > 0) walkX = 0.0
            }
            else if (walkX > 0) {
                walkX -= friction
                if (walkX < 0) walkX = 0.0
            }
        }
    }

    private fun setVerticalFriction() {
        val friction = if (isPlayerNoClip)
            BASE_FRICTION * TilePropCodex[TileNameCode.STONE].friction.tileFrictionToMult()
        else
            BASE_FRICTION * bodyFriction.tileFrictionToMult()

        if (veloY < 0) {
            veloY += friction
            if (veloY > 0) veloX = 0.0 // compensate overshoot
        }
        else if (veloY > 0) {
            veloY -= friction
            if (veloY < 0) veloY = 0.0 // compensate overshoot
        }

        if (this is Controllable) {
            if (walkY < 0) {
                walkY += friction
                if (walkY > 0) walkY = 0.0
            }
            else if (walkY > 0) {
                walkY -= friction
                if (walkY < 0) walkY = 0.0
            }
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
     * Get highest friction value from surrounding tiles
     * @return
     */
    internal val bodyFriction: Int
        get() {
            var friction = 0
            forEachFeetTile {
                if (it?.friction ?: 4 > friction) // 4: friction of the air
                    friction = it?.friction ?: 4
            }

            return friction
        }

    fun Int.tileFrictionToMult(): Double = this / 16.0

    /**
     * Get highest tile density from occupying tiles, fluid only
     */
    private val tileDensityFluid: Int
        get() {
            var density = 0
            forEachOccupyingTile {
                // get max density for each tile
                if (it?.isFluid ?: false && it?.density ?: 0 > density) {
                    density = it?.density ?: 0
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
            forEachOccupyingTile {
                // get max density for each tile
                if (it?.density ?: 0 > density) {
                    density = it?.density ?: 0
                }
            }

            return density
        }

    private fun clampHitbox() {
        val worldsizePxl = world.width.times(TILE_SIZE)

        hitbox.setPositionFromPoint(
                //clampW(hitbox.pointedX),
                if (hitbox.pointedX < 0)
                    hitbox.pointedX + worldsizePxl
                else if (hitbox.pointedX >= worldsizePxl)
                    hitbox.pointedX - worldsizePxl
                else
                    hitbox.pointedX, // ROUNDWORLD impl
                clampH(hitbox.pointedY)
        )
    }

    private fun setNewNextHitbox() {
        nextHitbox.setFromWidthHeight(
                hitbox.posX + moveDelta.x,
                hitbox.posY + moveDelta.y,
                baseHitboxW * scale,
                baseHitboxH * scale
        )
    }

    private fun updateHitbox() = hitbox.reassign(nextHitbox)

    open fun drawGlow(gc: GameContainer, g: Graphics) {
        if (isVisible && spriteGlow != null) {
            if (!sprite!!.flippedHorizontal()) {
                spriteGlow!!.render(g,
                        (hitbox.posX - hitboxTranslateX * scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                spriteGlow!!.render(g,
                        (hitbox.posX - scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
        }
    }

    open fun drawBody(gc: GameContainer, g: Graphics) {
        if (isVisible && sprite != null) {
            when (drawMode) {
                DrawMode.NORMAL   -> blendNormal()
                DrawMode.MULTIPLY -> blendMul()
                DrawMode.SCREEN   -> blendScreen()
            }

            if (!sprite!!.flippedHorizontal()) {
                sprite!!.render(g,
                        (hitbox.posX - hitboxTranslateX * scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                sprite!!.render(g,
                        (hitbox.posX - scale).toFloat(),
                        (hitbox.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2).toFloat(),
                        (scale).toFloat()
                )
            }
        }
    }

    open fun updateGlowSprite(gc: GameContainer, delta: Int) {
        if (spriteGlow != null) spriteGlow!!.update(delta)
    }

    open fun updateBodySprite(gc: GameContainer, delta: Int) {
        if (sprite != null) sprite!!.update(delta)
    }

    private fun clampW(x: Double): Double =
            if (x < TILE_SIZE + nextHitbox.width / 2) {
                TILE_SIZE + nextHitbox.width / 2
            }
            else if (x >= (world.width * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - nextHitbox.width / 2) {
                (world.width * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - nextHitbox.width / 2
            }
            else {
                x
            }

    private fun clampH(y: Double): Double =
            if (y < TILE_SIZE + nextHitbox.height) {
                TILE_SIZE + nextHitbox.height
            }
            else if (y >= (world.height * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - nextHitbox.height) {
                (world.height * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - nextHitbox.height
            }
            else {
                y
            }

    private fun clampWtile(x: Int): Int =
            if (x < 0) 0 else if (x >= world.width) world.width - 1 else x

    private fun clampHtile(x: Int): Int =
            if (x < 0) 0 else if (x >= world.height) world.height - 1 else x


    private val isPlayerNoClip: Boolean
        get() = this is Player && this.isNoClip()

    private val AUTO_CLIMB_RATE: Int
        get() = Math.min(TILE_SIZE / 8 * Math.sqrt(scale), TILE_SIZE.toDouble()).toInt()

    private fun assertInit() {
        // errors
        if (baseHitboxW == 0 || baseHitboxH == 0)
            throw Error("Hitbox dimension was not set.")

        // warnings
        if (sprite == null && isVisible)
            println("[ActorWithBody] Caution: actor ${this.javaClass.simpleName} is echo but the sprite was not set.")
        else if (sprite != null && !isVisible)
            println("[ActorWithBody] Caution: actor ${this.javaClass.simpleName} is invisible but the sprite was given.")

        assertPrinted = true
    }

    internal fun flagDespawn() {
        flagDespawn = true
    }

    private fun forEachOccupyingTileNum(consumer: (Int?) -> Unit) {
        val tiles = ArrayList<Int?>()
        for (y in tilewiseHitbox.posY.toInt()..tilewiseHitbox.endPointY.toInt()) {
            for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
                tiles.add(world.getTileFromTerrain(x, y))
            }
        }

        return tiles.forEach(consumer)
    }

    private fun forEachOccupyingTile(consumer: (TileProp?) -> Unit) {
        val tileProps = ArrayList<TileProp?>()
        for (y in tilewiseHitbox.posY.toInt()..tilewiseHitbox.endPointY.toInt()) {
            for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
                tileProps.add(TilePropCodex[world.getTileFromTerrain(x, y)])
            }
        }

        return tileProps.forEach(consumer)
    }

    private fun forEachFeetTileNum(consumer: (Int?) -> Unit) {
        val tiles = ArrayList<Int?>()

        // offset 1 pixel to the down so that friction would work
        val y = nextHitbox.endPointY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
            tiles.add(world.getTileFromTerrain(x, y))
        }

        return tiles.forEach(consumer)
    }

    private fun forEachFeetTile(consumer: (TileProp?) -> Unit) {
        val tileProps = ArrayList<TileProp?>()

        // offset 1 pixel to the down so that friction would work
        val y = nextHitbox.endPointY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
            tileProps.add(TilePropCodex[world.getTileFromTerrain(x, y)])
        }

        return tileProps.forEach(consumer)
    }

    companion object {

        @Transient private val TILE_SIZE = MapDrawer.TILE_SIZE

        private fun div16TruncateToMapWidth(x: Int): Int {
            if (x < 0)
                return 0
            else if (x >= Terrarum.ingame.world.width shl 4)
                return Terrarum.ingame.world.width - 1
            else
                return x and 0x7FFFFFFF shr 4
        }

        private fun div16TruncateToMapHeight(y: Int): Int {
            if (y < 0)
                return 0
            else if (y >= Terrarum.ingame.world.height shl 4)
                return Terrarum.ingame.world.height - 1
            else
                return y and 0x7FFFFFFF shr 4
        }

        private fun clampCeil(x: Double, ceil: Double): Double {
            return if (Math.abs(x) > ceil) ceil else x
        }
    }
}

fun Double.floorInt() = Math.floor(this).toInt()
fun Float.floorInt() = FastMath.floor(this).toInt()
fun Double.round() = Math.round(this).toDouble()
fun Double.floor() = Math.floor(this)
fun Double.ceil() = this.floor() + 1.0
fun Double.roundInt(): Int = Math.round(this).toInt()
fun Float.roundInt(): Int = Math.round(this).toInt()
fun Double.abs() = Math.abs(this)
fun Double.sqr() = this * this
fun Double.sqrt() = Math.sqrt(this)
fun Int.abs() = if (this < 0) -this else this
fun Double.bipolarClamp(limit: Double) =
        if (this > 0 && this > limit) limit
        else if (this < 0 && this < -limit) -limit
        else this

fun absMax(left: Double, right: Double): Double {
    if (left > 0 && right > 0)
        if (left > right) return left
        else return right
    else if (left < 0 && right < 0)
        if (left < right) return left
        else return right
    else {
        val absL = left.abs()
        val absR = right.abs()
        if (absL > absR) return left
        else return right
    }
}

fun Double.magnSqr() = if (this >= 0.0) this.sqr() else -this.sqr()
fun Double.sign() = if (this > 0.0) 1.0 else if (this < 0.0) -1.0 else 0.0

enum class DrawMode {
    NORMAL, SCREEN, MULTIPLY
}
