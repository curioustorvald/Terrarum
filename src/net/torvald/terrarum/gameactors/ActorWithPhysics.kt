package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.point.Point2d
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockProp
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import java.util.*

/**
 * Base class for every actor that has animated sprites. This includes furnishings, paintings, gadgets, etc.
 * Also has all the physics
 *
 * @param renderOrder Rendering order (BEHIND, MIDDLE, MIDTOP, FRONT)
 * @param immobileBody use realistic air friction (1/1000 of "unrealistic" canonical setup)
 * @param physics use physics simulation
 *
 * Created by minjaesong on 16-01-13.
 */
open class ActorWithPhysics(renderOrder: RenderOrder, val immobileBody: Boolean = false, physics: Boolean = true) : ActorWithBody(renderOrder) {

    /** !! ActorValue macros are on the very bottom of the source !! **/


    @Transient internal var sprite: SpriteAnimation? = null
    @Transient internal var spriteGlow: SpriteAnimation? = null

    var drawMode = BlendMode.NORMAL

    @Transient private val world: GameWorld = Terrarum.ingame!!.world

    var hitboxTranslateX: Double = 0.0// relative to spritePosX
        protected set
    var hitboxTranslateY: Double = 0.0// relative to spritePosY
        protected set
    var baseHitboxW: Int = 0
        protected set
    var baseHitboxH: Int = 0
        protected set
    /**
     * * Position: top-left point
     * * Unit: pixel
     * !! external class should not hitbox.set(); use setHitboxDimension() and setPosition()
     */
    override val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)       // Hitbox is implemented using Double;

    val tilewiseHitbox: Hitbox
        get() = Hitbox.fromTwoPoints(
                hitbox.posX.div(TILE_SIZE).floor(),
                hitbox.posY.div(TILE_SIZE).floor(),
                hitbox.endPointX.div(TILE_SIZE).floor(),
                hitbox.endPointY.div(TILE_SIZE).floor()
        )

    /**
     * Elevators/Movingwalks/etc.: edit hitbox manually!
     *
     * Velocity vector for newtonian sim.
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     */
    internal val externalForce = Vector2(0.0, 0.0)

    @Transient private val VELO_HARD_LIMIT = 100.0

    /**
     * for "Controllable" actors
     */
    var controllerMoveDelta: Vector2? = if (this is Controllable) Vector2() else null
    var walkX: Double
        get() = controllerMoveDelta!!.x
        protected set(value) {
            controllerMoveDelta?.x = value
        }
    var walkY: Double
        get() = controllerMoveDelta!!.y
        protected set(value) {
            controllerMoveDelta?.y = value
        }
    // not sure we need this...
    //var jumpable = true // this is kind of like "semaphore"

    /**
     * Physical properties.
     */
    /** Apparent scale. Use "avBaseScale" for base scale */
    var scale: Double
        get() = (actorValue.getAsDouble(AVKey.SCALE) ?: 1.0) *
                (actorValue.getAsDouble(AVKey.SCALEBUFF) ?: 1.0)
        set(value) {
            val scaleDelta = value - scale
            actorValue[AVKey.SCALE] = value / (actorValue.getAsDouble(AVKey.SCALEBUFF) ?: 1.0)
            // reposition
            translatePosition(-baseHitboxW * scaleDelta / 2, -baseHitboxH * scaleDelta)
        }
    @Transient val MASS_LOWEST = 0.1 // Kilograms
    /** Apparent mass. Use "avBaseMass" for base mass */
    val mass: Double
        get() = actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT * Math.pow(scale, 3.0)
        /*set(value) { // use "var avBaseMass: Double"
            if (value <= 0)
                throw IllegalArgumentException("mass cannot be less than or equal to zero.")
            else if (value < MASS_LOWEST) {
                println("[ActorWithPhysics] input too small; using $MASS_LOWEST instead.")
                actorValue[AVKey.BASEMASS] = MASS_LOWEST
            }

            actorValue[AVKey.BASEMASS] = value / Math.pow(scale, 3.0)
        }*/
    @Transient private val MASS_DEFAULT: Double = 60.0
    /** Valid range: [0, 1]  */
    var elasticity: Double = 0.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("invalid elasticity value $value; valid elasticity value is [0, 1].")
            else if (value >= ELASTICITY_MAX) {
                println("[ActorWithPhysics] Elasticity were capped to $ELASTICITY_MAX.")
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

    var density = 1000.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithPhysics] $value: density cannot be negative.")

            field = value
        }

    /**
     * Flags and Properties
     */


    @Volatile var grounded = false
    /** Default to 'true'  */
    var isVisible = true
    /** Default to 'true'  */
    var isUpdate = physics
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
                throw IllegalArgumentException("[ActorWithPhysics] drag coefficient cannot be negative.")
            actorValue[AVKey.DRAGCOEFF] = value
        }

    @Transient private val UD_COMPENSATOR_MAX = TILE_SIZE
    @Transient private val LR_COMPENSATOR_MAX = TILE_SIZE

    /**
     * Post-hit invincibility, in milliseconds
     */
    @Transient val INVINCIBILITY_TIME: Millisec = 500

    @Transient internal val BASE_FRICTION = 0.3

    var collisionType = COLLISION_DYNAMIC

    @Transient private val CCD_TICK = 1.0 / 16.0
    @Transient private val CCD_TRY_MAX = 12800

    // just some trivial magic numbers
    @Transient private val A_PIXEL = 1.0
    @Transient private val HALF_PIXEL = 0.5
    @Transient private val COLLIDING_TOP = 0
    @Transient private val COLLIDING_RIGHT = 1
    @Transient private val COLLIDING_BOTTOM = 2
    @Transient private val COLLIDING_LEFT = 3
    @Transient private val COLLIDING_UD = 4
    @Transient private val COLLIDING_LR = 5
    @Transient private val COLLIDING_ALLSIDE = 6
    @Transient private val COLLIDING_EXTRA_SIZE = 7

    /**
     * Temporary variables
     */

    @Transient private var assertPrinted = false

    // to use with Controller (incl. player)
    internal @Volatile var walledLeft = false
    internal @Volatile var walledRight = false
    internal @Volatile var walledTop = false    // UNUSED; only for BasicDebugInfoWindow
    internal @Volatile var walledBottom = false // UNUSED; only for BasicDebugInfoWindow

    protected val gameContainer: GameContainer
        get() = Terrarum.appgc
    protected val updateDelta: Int
        get() = Terrarum.delta


    var isWalkingH = false
    var isWalkingV = false

    init {
        // some initialiser goes here...
    }

    fun makeNewSprite(w: Int, h: Int, image: Image) {
        sprite = SpriteAnimation(this, w, h)
        sprite!!.setSpriteImage(image)
    }

    fun makeNewSprite(w: Int, h: Int, imageref: String) {
        sprite = SpriteAnimation(this, w, h)
        sprite!!.setSpriteImage(imageref)
    }

    fun makeNewSpriteGlow(w: Int, h: Int, image: Image) {
        spriteGlow = SpriteAnimation(this, w, h)
        spriteGlow!!.setSpriteImage(image)
    }

    fun makeNewSpriteGlow(w: Int, h: Int, imageref: String) {
        spriteGlow = SpriteAnimation(this, w, h)
        spriteGlow!!.setSpriteImage(imageref)
    }

    /**
     * @param w
     * @param h
     * @param tx positive: translate sprite to LEFT.
     * @param ty positive: translate sprite to DOWN.
     * @see ActorWithPhysics.drawBody
     * @see ActorWithPhysics.drawGlow
     */
    fun setHitboxDimension(w: Int, h: Int, tx: Int, ty: Int) {
        baseHitboxH = h
        baseHitboxW = w
        hitboxTranslateX = tx.toDouble()
        hitboxTranslateY = ty.toDouble()
    }

    fun setPosition(pos: Point2d) = setPosition(pos.x, pos.y)
    fun setPosition(pos: Vector2) = setPosition(pos.x, pos.y)


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
    }

    private fun translatePosition(dx: Double, dy: Double) {
        hitbox.translate(dx, dy)
    }

    val centrePosVector: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.centeredY)
    val centrePosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.centeredY)
    val feetPosVector: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.endPointY)
    val feetPosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.endPointY)
    val feetPosTile: IntArray
        get() = intArrayOf(tilewiseHitbox.centeredX.floorInt(), tilewiseHitbox.endPointY.floorInt())

    override fun run() = update(gameContainer, updateDelta)

    /**
     * Add vector value to the velocity, in the time unit of single frame.
     *
     * Since we're adding some value every frame, the value is equivalent to the acceleration.
     * Look for Newton's second law for the background knowledge.
     * @param acc : Acceleration in Vector2
     */
    fun applyForce(acc: Vector2) {
        externalForce += acc * speedMultByTile
    }

    private val bounceDampenVelThreshold = 0.5

    override fun update(gc: GameContainer, delta: Int) {
        if (isUpdate && !flagDespawn) {

            if (!assertPrinted) assertInit()

            if (sprite != null) sprite!!.update(delta)
            if (spriteGlow != null) spriteGlow!!.update(delta)

            // make NoClip work for player
            if (this is Player) {
                isNoSubjectToGrav = isPlayerNoClip
                isNoCollideWorld = isPlayerNoClip
                isNoSubjectToFluidResistance = isPlayerNoClip
            }

            ////////////////////////////////////////////////////////////////
            // Codes that modifies velocity (moveDelta and externalForce) //
            ////////////////////////////////////////////////////////////////

            // --> Apply more forces <-- //
            if (!isChronostasis) {
                // Actors are subject to the gravity and the buoyancy if they are not levitating

                if (!isNoSubjectToGrav) {
                    applyGravitation()
                }

                //applyBuoyancy()
            }

            // hard limit velocity
            externalForce.x = externalForce.x.bipolarClamp(VELO_HARD_LIMIT) // displaceHitbox SHOULD use moveDelta
            externalForce.y = externalForce.y.bipolarClamp(VELO_HARD_LIMIT)

            if (!isChronostasis) {
                ///////////////////////////////////////////////////
                // Codes that (SHOULD) displaces hitbox directly //
                ///////////////////////////////////////////////////

                /**
                 * solveCollision()?
                 * If and only if:
                 *     This body is NON-STATIC and the other body is STATIC
                 */
                if (!isPlayerNoClip) {
                    // // HOW IT SHOULD WORK // //
                    // ////////////////////////
                    // combineVeloToMoveDelta now
                    // displace hitbox (!! force--moveDelta--still exist, do not touch the force !!)
                    //      make sure "touching" is perfectly useable
                    //      16-step ccd applies here
                    // ((nextHitbox <- hitbox))
                    // resolve forces (use up the force && deform the vector):
                    //      // "touching" should work at this point if displaceHitbox is successful
                    //      [Collision]:
                    //          if touching (test for both axes):
                    //              re-direct force vector by mul w/ elasticity
                    //          if not touching:
                    //              do nothing
                    //      [Friction]:
                    //          deform vector "externalForce"
                    //          if isControllable:
                    //              also alter walkX/Y
                    // translate ((nextHitbox)) hitbox by moveDelta (forces), this consumes force
                    //      DO NOT set whatever delta to zero
                    // ((hitbox <- nextHitbox))
                    //
                    // ((comments))  [Label]


                    displaceHitbox()
                    applyNormalForce()
                    //applyControllerMoveVelo() // TODO
                }
                else {
                    hitbox.translate(externalForce)
                    hitbox.translate(controllerMoveDelta)
                }

                //////////////////////////////////////////////////////////////
                // Codes that modifies velocity (after hitbox displacement) //
                //////////////////////////////////////////////////////////////

                // modified vectors below SHOULD NOT IMMEDIATELY APPLIED!! //
                // these are FOR THE NEXT ROUND of update                  //
                // ((DO NOT DELETE THIS; made same mistake twice already)) //

                // TODO less friction for non-animating objects (make items glide far more on ice)

                setHorizontalFriction() // friction SHOULD use and alter externalForce
                if (isPlayerNoClip) { // TODO also hanging on the rope, etc.
                    setVerticalFriction()
                }


                // make sure if the actor tries to go out of the map, loop back instead
                clampHitbox()
            }

            // cheap solution for sticking into the wall while Left or Right is held
            walledLeft = isTouchingSide(hitbox, COLLIDING_LEFT)
            walledRight = isTouchingSide(hitbox, COLLIDING_RIGHT)
            walledTop = isTouchingSide(hitbox, COLLIDING_TOP)
            walledBottom = isTouchingSide(hitbox, COLLIDING_BOTTOM)
            if (isPlayerNoClip) {
                walledLeft = false
                walledRight = false
                walledTop = false
                walledBottom = false
            }
        }
    }

    /**
     * Similar to applyForce but deals with walking. Read below:
     *
     * how speedcap is achieved with moveDelta:
     * moveDelta is (velo + walk), but this added value is reset every update.
     * if it wasn't, it will go like this:
     *  F 0     velo + walk
     *  F 1     velo + walk + velo + walk
     * as a result, the speed will keep increase without it
     */
    /*private fun combineVeloToMoveDelta() {
        if (this is Controllable) {
            // decide whether to ignore walkX
            if (!(isTouchingSide(hitbox, COLLIDING_LEFT) && walkX < 0)
                || !(isTouchingSide(hitbox, COLLIDING_RIGHT) && walkX > 0)
                    ) {
                moveDelta.x = externalForce.x + walkX
            }

            // decide whether to ignore walkY
            if (!(isTouchingSide(hitbox, COLLIDING_TOP) && walkY < 0)
                || !(isTouchingSide(hitbox, COLLIDING_BOTTOM) && walkY > 0)
                    ) {
                moveDelta.y = externalForce.y + walkY
            }
        }
        else {
            if (!isTouchingSide(hitbox, COLLIDING_LEFT)
                || !isTouchingSide(hitbox, COLLIDING_RIGHT)
                    ) {
                moveDelta.x = externalForce.x
            }

            // decide whether to ignore walkY
            if (!isTouchingSide(hitbox, COLLIDING_TOP)
                || !isTouchingSide(hitbox, COLLIDING_BOTTOM)
                    ) {
                moveDelta.y = externalForce.y
            }
        }
    }*/

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is precessed separately.
     */
    private fun applyGravitation() {
        if (!isNoSubjectToGrav && !isTouchingSide(hitbox, COLLIDING_BOTTOM)) {
            //if (!isTouchingSide(hitbox, COLLIDING_BOTTOM)) {
            /**
             * weight; gravitational force in action
             * W = mass * G (9.8 [m/s^2])
             */
            val W: Vector2 = gravitation * Terrarum.TARGET_FPS.toDouble()
            /**
             * Area
             */
            val A: Double = (scale * baseHitboxW / METER) * (scale * baseHitboxW / METER)
            /**
             * Drag of atmosphere
             * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity sqr) * A (area)
             */
            val D: Vector2 = Vector2(externalForce.x.magnSqr(), externalForce.y.magnSqr()) * dragCoefficient * 0.5 * A// * tileDensityFluid.toDouble()

            val V: Vector2 = (W - D) / Terrarum.TARGET_FPS.toDouble() * SI_TO_GAME_ACC

            applyForce(V)
            //}
        }
    }

    private fun applyNormalForce() {
        if (!isNoCollideWorld) {
            val moveDelta = externalForce + controllerMoveDelta

            // axis Y. Using operand >= and hitting the ceiling will lock the player to the position

            // was moving downward?
            if (moveDelta.y > 0.0) {
                if (isTouchingSide(hitbox, COLLIDING_TOP)) { // hit the ceiling
                    hitAndReflectY() //hitAndForciblyReflectY()
                    grounded = false
                }
                else if (isTouchingSide(hitbox, COLLIDING_BOTTOM)) { // actor hit something on its bottom
                    hitAndReflectY()
                    grounded = true
                }
                else { // the actor is not grounded at all
                    grounded = false
                }
            }
            // or was moving upward?
            else if (moveDelta.y < 0.0) {
                grounded = false
                if (isTouchingSide(hitbox, COLLIDING_TOP)) { // actor hit something on its top
                    hitAndForciblyReflectY() // prevents sticking to the ceiling
                }
            }
            // axis X
            if (isTouchingSide(hitbox, COLLIDING_LEFT) || isTouchingSide(hitbox, COLLIDING_RIGHT)) { // check right and left
                // the actor is hitting the wall

                // FIXME balls are stuck in this
                //if (referenceID != 321321321)
                //    println("$this trying to reflectX")
                hitAndReflectX()
            }
        }
    }

    /**
     * nextHitbox must NOT be altered before this method is called!
     */
    /*@Deprecated("It's stupid anyway.") private fun displaceByCCD() {
        if (!isNoCollideWorld) {
            if (!isColliding(hitbox))
                return

            // do some CCD between hitbox and nextHitbox
            val ccdDelta = (hitbox.toVector() - hitbox.toVector())
            if (ccdDelta.x != 0.0 || ccdDelta.y != 0.0) {
                //ccdDelta.set(ccdDelta.setMagnitude(CCD_TICK)) // fixed tick
                val displacement = Math.min(1.0.div(moveDelta.magnitude * 2), 0.5) // adaptive tick
                ccdDelta.set(ccdDelta.setMagnitude(displacement))
            }

            //println("deltaMax: $deltaMax")
            //println("ccdDelta: $ccdDelta")

            while (!ccdDelta.isZero && isColliding(hitbox)) {
                hitbox.translate(-ccdDelta)
            }

            //println("ccdCollided: $ccdCollided")
        }
    }*/

    /**
     * nextHitbox must NOT be altered before this method is called!
     */
    private val ccdSteps = 32 // max allowed velocity = backtrackSteps * TILE_SIZE
    private val binaryBranchingMax = 54 // higher = more precise; theoretical max = 54 (# of mantissa + 2)

    private fun displaceHitbox() {
        fun debug1(wut: Any?) {
            //  vvvvv  set it true to make debug print work
            if (true) println(wut)
        }
        fun debug2(wut: Any?) {
            //  vvvvv  set it true to make debug print work
            if (true) println(wut)
        }
        fun debug3(wut: Any?) {
            //  vvvvv  set it true to make debug print work
            if (true) println(wut)
        }
        fun debug4(wut: Any?) {
            //  vvvvv  set it true to make debug print work
            if (true) println(wut)
        }

        //val moveDelta = externalForce + controllerMoveDelta

        // I kinda need these notes when I'm not caffeinated
        //
        // First of all: Rules
        //  1. If two sides are touching each other (they share exactly same coord along one axis),
        //     they are not colliding (just touching)
        //  2. If two sides are embedded into each other, they are colliding.
        // [Objective]
        //  - Displace `hitbox` directly
        //  - Make sure "isTouching" is perfectly useable, it depends on it
        // [Procedure]
        //  find "edge" point using binary search
        // [END OF SUBROUTINE]

        // TODO IDEA: if hitbox of controllerMoveDelta is shrunken then you won't need "PUSH THE HITBOX INTO THE AIR" part


        fun getBacktrackDelta(percentage: Double): Vector2 {
            if (percentage < 0.0 || percentage > 1.0)
                throw IllegalArgumentException("$percentage")

            return externalForce * percentage//externalForce * percentage
        }
        fun getControllerXDelta(percentage: Double): Vector2 {
            if (percentage < 0.0 || percentage > 1.0)
                throw IllegalArgumentException("$percentage")

            return Vector2(controllerMoveDelta!!.x * percentage, 0.0)
        }
        fun getControllerYDelta(percentage: Double): Vector2 {
            if (percentage < 0.0 || percentage > 1.0)
                throw IllegalArgumentException("$percentage")

            return Vector2(0.0, controllerMoveDelta!!.y * percentage)
        }



        debug1("########################################")
        debug1("hitbox: $hitbox")

        val simulationHitbox = hitbox.clone()

        if (externalForce.isZero) {
            debug1("externalForce is zero")
        }
        else {
            debug1("externalForce = $externalForce, controller = $controllerMoveDelta")

            var ccdTick: Int = ccdSteps // 0..15: collision detected, 16: not

            // do CCD first
            for (i in 1..ccdSteps) { // start from 1: if you are grounded, CCD of 0 will report as COLLIDING and will not you jump
                simulationHitbox.reassign(hitbox)
                simulationHitbox.translate(getBacktrackDelta(i.toDouble() / ccdSteps))

                debug2("ccd $i, endY = ${simulationHitbox.endPointY}")

                // TODO use isTouching (larger box) instead of isColliding?
                if (isColliding(simulationHitbox)) {
                    ccdTick = i
                    break
                }
            }


            debug2("ccdTick = $ccdTick, endY = ${simulationHitbox.endPointY}")


            // collision not found
            var collisionNotFound = false
            if (ccdTick == ccdSteps) {
                hitbox.translate(externalForce)
                debug2("no collision; endY = ${hitbox.endPointY}")
                collisionNotFound = true
            }

            if (!collisionNotFound) {
                debug2("embedding before: ${simulationHitbox.endPointY}")

                // find no-collision point using binary search
                // trust me, X- and Y-axis must move simultaneously.
                //// binary search ////
                if (ccdTick >= 1) {
                    var low = (ccdTick - 1).toDouble() / ccdSteps
                    var high = (ccdTick).toDouble() / ccdSteps
                    var bmid: Double

                    (1..binaryBranchingMax).forEach { _ ->

                        bmid = (low + high) / 2.0

                        simulationHitbox.reassign(hitbox)
                        simulationHitbox.translate(getBacktrackDelta(bmid))

                        // set new mid
                        // TODO use isTouching (larger box) instead of isColliding?
                        if (isColliding(simulationHitbox)) { //COLLIDING_EXTRA_SIZE: doing trick so that final pos would be x.99800000 instead of y.0000000
                            debug2("bmid = $bmid, new endY: ${simulationHitbox.endPointY}, going back")
                            high = bmid
                        }
                        else {
                            debug2("bmid = $bmid, new endY: ${simulationHitbox.endPointY}, going forth")
                            low = bmid
                        }
                    }

                    debug2("binarySearch embedding: ${simulationHitbox.endPointY}")


                    // apply Normal Force
                    // next step (resolve controllerMoveDelta) requires this to be pre-handled
                    if (isTouchingSide(simulationHitbox, COLLIDING_BOTTOM)) {
                        if (gravitation.y > 0.0) grounded = true
                        // reset walkY
                        walkY *= elasticity
                        debug1("!! grounded ${Random().nextInt(1000)}!!")
                    }
                    else if (isTouchingSide(simulationHitbox, COLLIDING_TOP)) {
                        if (gravitation.y < 0.0) grounded = true
                        // reset walkY
                        walkY *= elasticity
                        debug1("!! headbutt ${Random().nextInt(1000)}!!")
                    }

                    if (isTouchingSide(simulationHitbox, COLLIDING_LR)) {
                        // reset walkX
                        walkX *= elasticity
                        debug1("!! tackle ${Random().nextInt(1000)}!!")
                    }
                }

            }
        } // must end with semi-final hitbox


        // PUSH THE HITBOX INTO THE AIR for a pixel so IT WON'T BE COLLIDING
        //
        // naturally, binarySearch gives you a point like 7584.99999999 (barely not colliding) or
        // 7585.000000000 (colliding as fuck), BUT what we want is 7584.00000000 .
        // [Procedure]
        //  1. get touching area of four sides incl. edge points
        //  2. a side with most touching area is the "colliding side"
        //  3. round the hitbox so that coord of "colliding" side be integer
        //  3.1. there's two main cases: "main axis" being X; "main axis" being Y
        //  3.2. edge cases: (TBA)

        val vectorSum = externalForce + controllerMoveDelta
        // --> Y-Axis
        if (vectorSum.y > 0.0 && isTouchingSide(simulationHitbox, COLLIDING_BOTTOM)) {
            val displacementMainAxis = -1.0
            val displacementSecondAxis = displacementMainAxis * vectorSum.x / vectorSum.y // use controllerMoveDelta.x / controllerMoveDelta.y ?
            simulationHitbox.translate(displacementSecondAxis, displacementMainAxis)
            debug2("1 dx: $displacementSecondAxis, dy: $displacementMainAxis")
        }
        else if (vectorSum.y < 0.0 && isTouchingSide(simulationHitbox, COLLIDING_TOP)) {
            val displacementMainAxis = 1.0
            val displacementSecondAxis = displacementMainAxis * vectorSum.x / vectorSum.y
            simulationHitbox.translate(displacementSecondAxis, displacementMainAxis)
            debug2("2 dx: $displacementSecondAxis, dy: $displacementMainAxis")
        }
        // --> X-Axis
        if (vectorSum.x > 0.0 && isTouchingSide(simulationHitbox, COLLIDING_RIGHT)) {
            val displacementMainAxis = -1.0
            val displacementSecondAxis = displacementMainAxis * vectorSum.y / vectorSum.x
            simulationHitbox.translate(displacementMainAxis, displacementSecondAxis)
            debug2("3 dx: $displacementMainAxis, dy: $displacementSecondAxis")
        }
        else if (vectorSum.x < 0.0 && isTouchingSide(simulationHitbox, COLLIDING_LEFT)) {
            val displacementMainAxis = 1.0
            val displacementSecondAxis = displacementMainAxis * vectorSum.y / vectorSum.x
            simulationHitbox.translate(displacementMainAxis, displacementSecondAxis)
            debug2("4 dx: $displacementMainAxis, dy: $displacementSecondAxis")
        }


        /////////////////////////////////
        // resolve controllerMoveDelta //
        /////////////////////////////////

        // TODO IDEA  just as CCD starts from 1, not 0, if moveDelta is engaged,
        // TODO IDEA  FIRST JUST APPLY the force THEN resolve collision or whatever

        if (controllerMoveDelta != null) {
            debug3("== ControllerMoveDelta ==")

            println("simulationHitbox = $simulationHitbox")

            // X-Axis
            val simulationHitboxX = simulationHitbox.clone()
            if (controllerMoveDelta!!.x != 0.0) {
                // skipping CCD and directly into BinarySearch: CCD would be unnecessary
                var low = 0.0
                var high = 1.0
                var bmid: Double

                (1..binaryBranchingMax).forEach { _ ->

                    bmid = (low + high) / 2.0

                    simulationHitboxX.reassign(simulationHitbox)
                    simulationHitboxX.translate(getControllerXDelta(bmid))

                    // set new mid
                    // TODO LR-touching or colliding?
                    if (isTouchingSide(simulationHitboxX, COLLIDING_LEFT) || isTouchingSide(simulationHitboxX, COLLIDING_RIGHT)) {
                        debug3("x bmid = $bmid, new endX: ${simulationHitboxX.endPointX}, going back")
                        high = bmid
                    }
                    else {
                        debug3("x bmid = $bmid, new endX: ${simulationHitboxX.endPointX}, going forth")
                        low = bmid
                    }
                }


                if (isTouchingSide(simulationHitboxX, COLLIDING_LEFT) || isTouchingSide(simulationHitboxX, COLLIDING_RIGHT)) {
                    //controllerMoveDelta!!.x *= elasticity
                }
            }

            // FIXME FIXME edge-to-edge collision

            // FIXME ceiling hit by jumping: mul controllerY by elasticity
            // FIXME jitter on hitting body against a wall
            // FIXME balls jitter af and stuck on a wall

            // Y-Axis
            val simulationHitboxY = simulationHitbox.clone()
            if (controllerMoveDelta!!.y != 0.0) {
                // skipping CCD and directly into BinarySearch: CCD would be unnecessary
                var low = 0.0
                var high = 1.0
                var bmid: Double

                (1..binaryBranchingMax).forEach { _ ->

                    bmid = (low + high) / 2.0

                    simulationHitboxY.reassign(simulationHitbox)
                    simulationHitboxY.translate(getControllerYDelta(bmid))

                    // set new mid
                    // TODO UD-touching or colliding?
                    if (isTouchingSide(simulationHitboxY, COLLIDING_TOP) || isTouchingSide(simulationHitboxY, COLLIDING_BOTTOM)) {
                        debug3("y bmid = $bmid, new endY: ${simulationHitboxY.endPointY}, going back")
                        high = bmid
                    }
                    else {
                        debug3("y bmid = $bmid, new endY: ${simulationHitboxY.endPointY}, going forth")
                        low = bmid
                    }
                }
            }


            simulationHitbox.setPosition(simulationHitboxX.posX, simulationHitboxY.posY)

            debug3("== END ControllerMoveDelta ==")
        }







        debug2("final controller: $controllerMoveDelta, displacement: ${simulationHitbox - hitbox}")


        hitbox.reassign(simulationHitbox)



        //println("# final hitbox: $hitbox, wx: $walkX, wy: $walkY")



        // if collision not detected, just don't care; it's not your job to apply moveDelta
    }



    private fun hitAndReflectX() {
        // when it sticks, externalForce.x goes back and forth
        /*
1123921356 trying to reflectX
1123921356	-1.3677473305837262
1123921356 trying to reflectX
1123921356	0.8150659571934893
1123921356 trying to reflectX
1123921356	-0.48545419966417575
1123921356 trying to reflectX
1123921356	0.28939570979162116
1123921356 trying to reflectX
1123921356	-0.17225986626214265
1123921356 trying to reflectX
1123921356	0.1027945259506898
1123921356 trying to reflectX
1123921356	-0.06108288092971576
         */

        externalForce.x *= -elasticity
        if (this is Controllable) walkX *= -elasticity // commented; should be managed by displaceHitbox()

        //println("$this\t${externalForce.x}")
    }

    private fun hitAndReflectY() {
        println("** reflY **")
        externalForce.y *= -elasticity
        if (this is Controllable) walkY *= -elasticity // commented; should be managed by displaceHitbox()
    }

    @Transient private val CEILING_HIT_ELASTICITY = 0.3
    @Transient private val MINIMUM_BOUNCE_THRESHOLD = 1.0

    /**
     * prevents sticking to the ceiling
     */
    private fun hitAndForciblyReflectY() {
        println("hitAndForciblyReflectY")
        val moveDelta = externalForce + controllerMoveDelta
        // TODO HARK! I have changed veloX/Y to moveDelta.x/y
        if (moveDelta.y < 0) {
            // kills movement if it is Controllable
            controllerMoveDelta?.let { it.y = 0.0 }


            if (moveDelta.y * CEILING_HIT_ELASTICITY < -A_PIXEL) {
                moveDelta.y = -moveDelta.y * CEILING_HIT_ELASTICITY
            }
            else {
                moveDelta.y = A_PIXEL
            }

            // for more of a "bounce", you can assign zero if you don't like it
            externalForce.y = moveDelta.y * CEILING_HIT_ELASTICITY
            //externalForce.y = 0.0


            //hitbox.translatePosY(0.5) // TODO why de we need it?
        }
        else {
            throw Error("Check this out bitch (moveDelta.y = ${moveDelta.y})")
        }
    }

    //private fun isColliding(hitbox: Hitbox) = isColliding(hitbox, 0)

    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    private fun isColliding(hitbox: Hitbox): Boolean {
        if (isNoCollideWorld) return false

        // detectors are inside of the bounding box
        val x1 = hitbox.posX
        val x2 = hitbox.endPointX - A_PIXEL
        val y1 = hitbox.posY
        val y2 = hitbox.endPointY - A_PIXEL


        val txStart = x1.div(TILE_SIZE).floorInt() // plus(1.0) : adjusting for yet another anomaly
        val txEnd =   x2.div(TILE_SIZE).floorInt()
        val tyStart = y1.div(TILE_SIZE).floorInt()
        val tyEnd =   y2.div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }

    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    private fun isTouchingSide(hitbox: Hitbox, option: Int): Boolean {
        val x1: Double
        val x2: Double
        val y1: Double
        val y2: Double

        /*
        The structure:

             #######  // TOP
            =+-----+=
            =|     |=
            =+-----+=
             #######  // BOTTOM
         */

        // detectors are inside of the bounding box
        if (option == COLLIDING_TOP) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX - A_PIXEL
            y1 = hitbox.posY - A_PIXEL - A_PIXEL
            y2 = y1
        }
        else if (option == COLLIDING_BOTTOM) {
            x1 = hitbox.posX
            x2 = hitbox.endPointX - A_PIXEL
            y1 = hitbox.endPointY
            y2 = y1
        }
        else if (option == COLLIDING_LEFT) {
            x1 = hitbox.posX - A_PIXEL
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY - A_PIXEL
        }
        else if (option == COLLIDING_RIGHT) {
            x1 = hitbox.endPointX
            x2 = x1
            y1 = hitbox.posY
            y2 = hitbox.endPointY - A_PIXEL
        }
        else if (option == COLLIDING_ALLSIDE) {
            x1 = hitbox.posX - A_PIXEL
            x2 = hitbox.endPointX
            y1 = hitbox.posY - A_PIXEL
            y2 = hitbox.endPointY
        }
        else if (option == COLLIDING_LR) {
            x1 = hitbox.posX - A_PIXEL
            x2 = hitbox.endPointX
            y1 = hitbox.posY
            y2 = hitbox.endPointY - A_PIXEL
        }
        else throw IllegalArgumentException()

        val txStart = x1.div(TILE_SIZE).floorInt()
        val txEnd =   x2.div(TILE_SIZE).floorInt()
        val tyStart = y1.div(TILE_SIZE).floorInt()
        val tyEnd =   y2.div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }

    private fun isCollidingInternal(txStart: Int, tyStart: Int, txEnd: Int, tyEnd: Int): Boolean {
        for (y in tyStart..tyEnd) {
            for (x in txStart..txEnd) {
                val tile = world.getTileFromTerrain(x, y)
                if (BlockCodex[tile].isSolid)
                    return true
            }
        }

        return false
    }

    private fun getContactingAreaFluid(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..(if (side % 2 == 0) hitbox.width else hitbox.height).roundInt() - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == COLLIDING_LEFT) {
                tileX = div16TruncateToMapWidth(hitbox.hitboxStart.x.roundInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(hitbox.hitboxEnd.y.roundInt() + translateY)
            }
            else if (side == COLLIDING_TOP) {
                tileX = div16TruncateToMapWidth(hitbox.hitboxStart.x.roundInt()
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(hitbox.hitboxStart.y.roundInt() + translateY)
            }
            else if (side == COLLIDING_RIGHT) {
                tileX = div16TruncateToMapWidth(hitbox.hitboxEnd.x.roundInt() + translateX)
                tileY = div16TruncateToMapHeight(hitbox.hitboxStart.y.roundInt()
                                                 + i + translateY)
            }
            else if (side == COLLIDING_LEFT) {
                tileX = div16TruncateToMapWidth(hitbox.hitboxStart.x.roundInt() + translateX)
                tileY = div16TruncateToMapHeight(hitbox.hitboxStart.y.roundInt()
                                                 + i + translateY)
            }
            else {
                throw IllegalArgumentException(side.toString() + ": Wrong side input")
            }

            // evaluate
            if (BlockCodex[world.getTileFromTerrain(tileX, tileY)].isFluid) {
                contactAreaCounter += 1
            }
        }

        return contactAreaCounter
    }

    private fun getTileFriction(tile: Int) =
            if (immobileBody && tile == Block.AIR)
                BlockCodex[Block.AIR].friction.frictionToMult().div(500)
                        .times(if (!grounded) elasticity else 1.0)
            else
                BlockCodex[tile].friction.frictionToMult()

    /** about stopping
     * for about get moving, see updateMovementControl */
    private fun setHorizontalFriction() {
        val friction = if (isPlayerNoClip)
            BASE_FRICTION * BlockCodex[Block.STONE].friction.frictionToMult()
        else {
            // TODO status quo if !submerged else linearBlend(feetFriction, bodyFriction, submergedRatio)
            BASE_FRICTION * if (grounded) feetFriction else bodyFriction
        }

        if (externalForce.x < 0) {
            externalForce.x += friction
            if (externalForce.x > 0) externalForce.x = 0.0 // compensate overshoot
        }
        else if (externalForce.x > 0) {
            externalForce.x -= friction
            if (externalForce.x < 0) externalForce.x = 0.0 // compensate overshoot
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
            BASE_FRICTION * BlockCodex[Block.STONE].friction.frictionToMult()
        else
            BASE_FRICTION * bodyFriction

        if (externalForce.y < 0) {
            externalForce.y += friction
            if (externalForce.y > 0) externalForce.y = 0.0 // compensate overshoot
        }
        else if (externalForce.y > 0) {
            externalForce.y -= friction
            if (externalForce.y < 0) externalForce.y = 0.0 // compensate overshoot
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

    private val submergedRatio: Double
        get() = submergedHeight / hitbox.height

    private val submergedVolume: Double
        get() = submergedHeight * hitbox.width * hitbox.width

    private val submergedHeight: Double
        get() = Math.max(
                getContactingAreaFluid(COLLIDING_LEFT),
                getContactingAreaFluid(COLLIDING_RIGHT)
        ).toDouble()


    internal val bodyFriction: Double
        get() {
            var friction = 0.0
            forEachOccupyingTileNum {
                // get max friction
                if (getTileFriction(it ?: Block.AIR) > friction)
                    friction = getTileFriction(it ?: Block.AIR)
            }

            return friction
        }
    internal val feetFriction: Double
        get() {
            var friction = 0.0
            forEachFeetTileNum {
                // get max friction
                if (getTileFriction(it ?: Block.AIR) > friction)
                    friction = getTileFriction(it ?: Block.AIR)
            }

            return friction
        }

    fun Int.frictionToMult(): Double = this / 16.0

    internal val bodyViscosity: Int
        get() {
            var viscosity = 0
            forEachOccupyingTile {
                // get max viscosity
                if (it?.viscosity ?: 0 > viscosity)
                    viscosity = it?.viscosity ?: 0
            }

            return viscosity
        }
    internal val feetViscosity: Int
        get() {
            var viscosity = 0
            forEachFeetTile {
                // get max viscosity
                if (it?.viscosity ?: 0 > viscosity)
                    viscosity = it?.viscosity ?: 0
            }

            return viscosity
        }

    fun Int.viscosityToMult(): Double = 16.0 / (16.0 + this)

    internal val speedMultByTile: Double
        get() {
            val notSubmergedCap = if (grounded)
                feetViscosity.viscosityToMult()
            else
                bodyViscosity.viscosityToMult()
            val normalisedViscocity = bodyViscosity.viscosityToMult()

            return interpolateLinear(submergedRatio, notSubmergedCap, normalisedViscocity)
        }
    /** about get going
     * for about stopping, see setHorizontalFriction */
    internal val accelMultMovement: Double
        get() {
            if (!isPlayerNoClip) {
                val notSubmergedAccel = if (grounded)
                    feetFriction
                else
                    bodyFriction
                val normalisedViscocity = bodyViscosity.viscosityToMult()

                return interpolateLinear(submergedRatio, notSubmergedAccel, normalisedViscocity)
            }
            else {
                return 1.0
            }
        }

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

        hitbox.setPositionFromPointed(
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

    override fun drawGlow(g: Graphics) {
        if (isVisible && spriteGlow != null) {
            blendLightenOnly()

            val offsetX = if (!spriteGlow!!.flippedHorizontal())
                hitboxTranslateX * scale
            else
                spriteGlow!!.cellWidth * scale - (hitbox.width + hitboxTranslateX * scale)

            val offsetY = spriteGlow!!.cellHeight * scale - hitbox.height - hitboxTranslateY * scale - 2

            if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
                // camera center neg, actor center pos
                spriteGlow!!.render(g,
                        (hitbox.posX - offsetX).toFloat() + world.width * TILE_SIZE,
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
                // camera center pos, actor center neg
                spriteGlow!!.render(g,
                        (hitbox.posX - offsetX).toFloat() - world.width * TILE_SIZE,
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                spriteGlow!!.render(g,
                        (hitbox.posX - offsetX).toFloat(),
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
        }
    }

    val leftsidePadding = world.width.times(TILE_SIZE) - WorldCamera.width.ushr(1)
    val rightsidePadding = WorldCamera.width.ushr(1)

    override fun drawBody(g: Graphics) {
        if (isVisible && sprite != null) {

            BlendMode.resolve(drawMode)

            val offsetX = if (!sprite!!.flippedHorizontal())
                hitboxTranslateX * scale
            else
                sprite!!.cellWidth * scale - (hitbox.width + hitboxTranslateX * scale)

            val offsetY = sprite!!.cellHeight * scale - hitbox.height - hitboxTranslateY * scale - 2

            if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
                // camera center neg, actor center pos
                sprite!!.render(g,
                        (hitbox.posX - offsetX).toFloat() + world.width * TILE_SIZE,
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
                // camera center pos, actor center neg
                sprite!!.render(g,
                        (hitbox.posX - offsetX).toFloat() - world.width * TILE_SIZE,
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                sprite!!.render(g,
                        (hitbox.posX - offsetX).toFloat(),
                        (hitbox.posY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }

        }
    }

    override fun onActorValueChange(key: String, value: Any?) {
        // do nothing
    }




    private fun clampW(x: Double): Double =
            if (x < TILE_SIZE + hitbox.width / 2) {
                TILE_SIZE + hitbox.width / 2
            }
            else if (x >= (world.width * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - hitbox.width / 2) {
                (world.width * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - hitbox.width / 2
            }
            else {
                x
            }

    private fun clampH(y: Double): Double =
            if (y < TILE_SIZE + hitbox.height) {
                TILE_SIZE + hitbox.height
            }
            else if (y >= (world.height * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - hitbox.height) {
                (world.height * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - hitbox.height
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
            println("[ActorWithPhysics] Caution: actor ${this.javaClass.simpleName} is echo but the sprite was not set.")
        else if (sprite != null && !isVisible)
            println("[ActorWithPhysics] Caution: actor ${this.javaClass.simpleName} is invisible but the sprite was given.")

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

    private fun forEachOccupyingTile(consumer: (BlockProp?) -> Unit) {
        val tileProps = ArrayList<BlockProp?>()
        for (y in tilewiseHitbox.posY.toInt()..tilewiseHitbox.endPointY.toInt()) {
            for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
                tileProps.add(BlockCodex[world.getTileFromTerrain(x, y)])
            }
        }

        return tileProps.forEach(consumer)
    }

    private fun forEachFeetTileNum(consumer: (Int?) -> Unit) {
        val tiles = ArrayList<Int?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endPointY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
            tiles.add(world.getTileFromTerrain(x, y))
        }

        return tiles.forEach(consumer)
    }

    private fun forEachFeetTile(consumer: (BlockProp?) -> Unit) {
        val tileProps = ArrayList<BlockProp?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endPointY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.posX.toInt()..tilewiseHitbox.endPointX.toInt()) {
            tileProps.add(BlockCodex[world.getTileFromTerrain(x, y)])
        }

        return tileProps.forEach(consumer)
    }

    companion object {

        /**
         * Constants
         */

        @Transient private val METER = 24.0
        /**
         * [m / s^2] * SI_TO_GAME_ACC -> [px / InternalFrame^2]
         */
        @Transient val SI_TO_GAME_ACC = METER / (Terrarum.TARGET_FPS * Terrarum.TARGET_FPS).toDouble()
        /**
         * [m / s] * SI_TO_GAME_VEL -> [px / InternalFrame]
         */
        @Transient val SI_TO_GAME_VEL = METER / Terrarum.TARGET_FPS

        /**
         *  Enumerations that exported to JSON
         */
        @Transient const val COLLISION_NOCOLLIDE = 0
        /** does not displaced by external forces when collided, but it still can move (e.g. player, elevator) */
        @Transient const val COLLISION_KINEMATIC = 1
        /** displaced by external forces */
        @Transient const val COLLISION_DYNAMIC = 2
        /** does not displaced by external forces, target of collision (e.g. nonmoving static obj) */
        @Transient const val COLLISION_STATIC = 3
        @Transient const val COLLISION_KNOCKBACK_GIVER = 4 // mobs
        @Transient const val COLLISION_KNOCKBACK_TAKER = 5 // benevolent NPCs

        @Transient private val TILE_SIZE = FeaturesDrawer.TILE_SIZE

        private fun div16TruncateToMapWidth(x: Int): Int {
            if (x < 0)
                return 0
            else if (x >= Terrarum.ingame!!.world.width shl 4)
                return Terrarum.ingame!!.world.width - 1
            else
                return x and 0x7FFFFFFF shr 4
        }

        private fun div16TruncateToMapHeight(y: Int): Int {
            if (y < 0)
                return 0
            else if (y >= Terrarum.ingame!!.world.height shl 4)
                return Terrarum.ingame!!.world.height - 1
            else
                return y and 0x7FFFFFFF shr 4
        }

        private fun clampCeil(x: Double, ceil: Double): Double {
            return if (Math.abs(x) > ceil) ceil else x
        }
    }

    // gameplay-related actorvalue macros

    var avBaseScale: Double // use canonical "scale" for apparent scale (base * buff)
        get() = actorValue.getAsDouble(AVKey.SCALE) ?: 1.0
        set(value) {
            actorValue[AVKey.SCALE] = value
        }
    /**
     * Apparent strength. 1 000 is default value
     */
    val avStrength: Double
        get() = (actorValue.getAsDouble(AVKey.STRENGTH) ?: 1000.0) *
                (actorValue.getAsDouble(AVKey.STRENGTHBUFF) ?: 1.0) * scale
    var avBaseStrength: Double?
        get() = actorValue.getAsDouble(AVKey.STRENGTH)
        set(value) {
            actorValue[AVKey.STRENGTH] = value!!
        }
    var avBaseMass: Double
        get() = actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT
        set(value) {
            actorValue[AVKey.BASEMASS] = value
        }
    val avAcceleration: Double
        get() = actorValue.getAsDouble(AVKey.ACCEL)!! *
                actorValue.getAsDouble(AVKey.ACCELBUFF)!! *
                accelMultMovement *
                scale.sqrt()
    val avSpeedCap: Double
        get() = actorValue.getAsDouble(AVKey.SPEED)!! *
                actorValue.getAsDouble(AVKey.SPEEDBUFF)!! *
                speedMultByTile *
                scale.sqrt()
}

fun Double.floorInt() = Math.floor(this).toInt()
fun Float.floorInt() = FastMath.floor(this)
fun Float.floor() = FastMath.floor(this).toFloat()
fun Float.ceilInt() = FastMath.ceil(this)
fun Double.round() = Math.round(this).toDouble()
fun Double.floor() = Math.floor(this)
fun Double.ceil() = this.floor() + 1.0
fun Double.roundInt(): Int = Math.round(this).toInt()
fun Float.roundInt(): Int = Math.round(this)
fun Double.abs() = Math.abs(this)
fun Double.sqr() = this * this
fun Double.sqrt() = Math.sqrt(this)
fun Float.sqrt() = FastMath.sqrt(this)
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

fun interpolateLinear(scale: Double, startValue: Double, endValue: Double): Double {
    if (startValue == endValue) {
        return startValue
    }
    if (scale <= 0f) {
        return startValue
    }
    if (scale >= 1f) {
        return endValue
    }
    return (1f - scale) * startValue + scale * endValue
}