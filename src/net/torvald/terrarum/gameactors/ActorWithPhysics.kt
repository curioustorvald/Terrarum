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
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.realestate.LandUtil
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


    val COLLISION_TEST_MODE = true

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
                hitbox.startX.div(TILE_SIZE).floor(),
                hitbox.startY.div(TILE_SIZE).floor(),
                hitbox.endX.div(TILE_SIZE).floor(),
                hitbox.endY.div(TILE_SIZE).floor()
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
    @Transient private val ELASTICITY_MAX = 1.0//0.993 // No perpetual motion!

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

    val grounded: Boolean
        get() = isPlayerNoClip ||
                (world.gravitation.y > 0 && isWalled(hitbox, COLLIDING_BOTTOM) ||
                 world.gravitation.y < 0 && isWalled(hitbox, COLLIDING_TOP))
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
    //@Transient private val COLLIDING_LEFT_EXTRA = 7
    //@Transient private val COLLIDING_RIGHT_EXTRA = 7

    /**
     * Temporary variables
     */

    @Transient private var assertPrinted = false

    // debug only
    internal @Volatile var walledLeft = false
    internal @Volatile var walledRight = false
    internal @Volatile var walledTop = false    // UNUSED; only for BasicDebugInfoWindow
    internal @Volatile var walledBottom = false // UNUSED; only for BasicDebugInfoWindow
    internal @Volatile var colliding = false

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
        get() = Vector2(hitbox.centeredX, hitbox.endY)
    val feetPosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.endY)
    val feetPosTile: IntArray
        get() = intArrayOf(tilewiseHitbox.centeredX.floorInt(), tilewiseHitbox.endY.floorInt())

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
                isNoSubjectToGrav = isPlayerNoClip || COLLISION_TEST_MODE
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
                    //applyNormalForce()
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

                // FIXME asymmetry on friction
                setHorizontalFriction() // friction SHOULD use and alter externalForce
                //if (isPlayerNoClip) { // TODO also hanging on the rope, etc.
                    setVerticalFriction()
                //}


                // make sure if the actor tries to go out of the map, loop back instead
                clampHitbox()
            }

            // cheap solution for sticking into the wall while Left or Right is held
            walledLeft = isWalled(hitbox, COLLIDING_LEFT)
            walledRight = isWalled(hitbox, COLLIDING_RIGHT)
            walledTop = isWalled(hitbox, COLLIDING_TOP)
            walledBottom = isWalled(hitbox, COLLIDING_BOTTOM)
            colliding = isColliding(hitbox)
            if (isPlayerNoClip) {
                walledLeft = false
                walledRight = false
                walledTop = false
                walledBottom = false
                colliding = false
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
            if (!(isWalled(hitbox, COLLIDING_LEFT) && walkX < 0)
                || !(isWalled(hitbox, COLLIDING_RIGHT) && walkX > 0)
                    ) {
                moveDelta.x = externalForce.x + walkX
            }

            // decide whether to ignore walkY
            if (!(isWalled(hitbox, COLLIDING_TOP) && walkY < 0)
                || !(isWalled(hitbox, COLLIDING_BOTTOM) && walkY > 0)
                    ) {
                moveDelta.y = externalForce.y + walkY
            }
        }
        else {
            if (!isWalled(hitbox, COLLIDING_LEFT)
                || !isWalled(hitbox, COLLIDING_RIGHT)
                    ) {
                moveDelta.x = externalForce.x
            }

            // decide whether to ignore walkY
            if (!isWalled(hitbox, COLLIDING_TOP)
                || !isWalled(hitbox, COLLIDING_BOTTOM)
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
        if (!isNoSubjectToGrav && !isWalled(hitbox, COLLIDING_BOTTOM)) {
            //if (!isWalled(hitbox, COLLIDING_BOTTOM)) {
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
            if (false) println(wut)
        }
        fun debug4(wut: Any?) {
            //  vvvvv  set it true to make debug print work
            if (true) println(wut)
        }
        fun Double.modTile() = this.toInt().div(TILE_SIZE).times(TILE_SIZE)
        fun Double.modTileDelta() = this - this.modTile()


        val vectorSum = externalForce + controllerMoveDelta
        val ccdSteps = 16



        // TODO NEW idea: wall pushes the actors (ref. SM64 explained by dutch pancake)
        // direction to push is determined by the velocity
        // proc:
        // 10 I detect being walled and displace myself
        // 11 There's 16 possible case so work all 16 (some can be merged obviously)
        // 12 Amount of displacement can be obtained with modTileDelta()
        // 13 isWalled() is confirmed to be working
        // 20 sixteenStep may be optional, I think, but it'd be good to have

        // ignore MOST of the codes below (it might be possible to recycle the structure??)

        val sixteenStep = (0..ccdSteps).map { hitbox.clone().translate(vectorSum * (it / ccdSteps.toDouble())) } // zeroth step is for special condition

        val affectingTiles = ArrayList<BlockAddress>()
        var collidingStep: Int? = null
        for (step in 1..ccdSteps) {
            val stepBox = sixteenStep[step]
            forEachOccupyingTilePos(stepBox) {
                val tileCoord = LandUtil.resolveBlockAddr(it)
                val tileProp = BlockCodex.getOrNull(world.getTileFromTerrain(tileCoord.first, tileCoord.second))
                if (tileProp == null || tileProp.isSolid) {
                    affectingTiles.add(it)
                }
            }
            if (affectingTiles.isNotEmpty()) {
                collidingStep = step
                break // collision found on this step, break and proceed to next step
            }
        }


        val COLL_LEFTSIDE = 1
        val COLL_BOTTOMSIDE = 2
        val COLL_RIGHTSIDE = 4
        val COLL_TOPSIDE = 8

        var bounceX = false
        var bounceY = false
        // collision NOT detected
        if (collidingStep == null) {
            hitbox.translate(vectorSum)
            // grounded = false
        }
        // collision detected
        else {
            val newHitbox = hitbox.reassign(sixteenStep[collidingStep])

            var selfCollisionStatus = 0
            if (isWalled(newHitbox, COLLIDING_LEFT))   selfCollisionStatus += COLL_LEFTSIDE
            if (isWalled(newHitbox, COLLIDING_RIGHT))  selfCollisionStatus += COLL_RIGHTSIDE
            if (isWalled(newHitbox, COLLIDING_TOP))    selfCollisionStatus += COLL_TOPSIDE
            if (isWalled(newHitbox, COLLIDING_BOTTOM)) selfCollisionStatus += COLL_BOTTOMSIDE


            when (selfCollisionStatus) {
                0 -> { println("[ActorWithPhysics] Contradiction -- collision detected by CCD, but isWalled() says otherwise") }
                5 -> { bounceX = true }
                10 -> { bounceY = true }
                15 -> { newHitbox.reassign(sixteenStep[0]); bounceX = true; bounceY = true }
                // one-side collision
                1, 11 -> { newHitbox.translatePosX(TILE_SIZE - newHitbox.startX.modTileDelta ()); bounceX = true }
                4, 14 -> { newHitbox.translatePosX(          - newHitbox.endX.modTileDelta ())  ; bounceX = true }
                8, 13 -> { newHitbox.translatePosY(TILE_SIZE - newHitbox.startY.modTileDelta ()); bounceY = true }
                2, 7  -> { newHitbox.translatePosY(          - newHitbox.endY.modTileDelta ())  ; bounceY = true }
                // two-side collision
                3 -> {
                    newHitbox.translatePosX(TILE_SIZE - newHitbox.startX.modTileDelta ())
                    newHitbox.translatePosY(          - newHitbox.endY.modTileDelta ())
                    bounceX = true; bounceY = true
                }
                6 -> {
                    newHitbox.translatePosX(          - newHitbox.endX.modTileDelta ())
                    newHitbox.translatePosY(          - newHitbox.endY.modTileDelta ())
                    bounceX = true; bounceY = true
                }
                9 -> {
                    newHitbox.translatePosX(TILE_SIZE - newHitbox.startX.modTileDelta ())
                    newHitbox.translatePosY(TILE_SIZE - newHitbox.startY.modTileDelta ())
                    bounceX = true; bounceY = true
                }
                12 -> {
                    newHitbox.translatePosX(          - newHitbox.endX.modTileDelta ())
                    newHitbox.translatePosY(TILE_SIZE - newHitbox.startY.modTileDelta ())
                    bounceX = true; bounceY = true
                }
            }


            // bounce X/Y
            if (bounceX) {
                externalForce.x *= elasticity
                controllerMoveDelta?.let { controllerMoveDelta!!.x *= elasticity }
            }
            if (bounceY) {
                externalForce.y *= elasticity
                controllerMoveDelta?.let { controllerMoveDelta!!.y *= elasticity }
            }


            hitbox.reassign(newHitbox)


            // grounded = true
        }// end of collision not detected


        return

        /*val BLOCK_LEFTSIDE = 1
        val BLOCK_BOTTOMSIDE = 2
        val BLOCK_RIGHTSIDE = 4
        val BLOCK_TOPSIDE = 8
        fun getBlockCondition(hitbox: Hitbox, blockAddress: BlockAddress): Int {
            val blockX = (blockAddress % world.width) * TILE_SIZE
            val blockY = (blockAddress / world.width) * TILE_SIZE
            var ret = 0

            // test leftside
            if (hitbox.startX >= blockX && hitbox.startX < blockX + TILE_SIZE) { // TEST ME: <= or <
                ret += BLOCK_LEFTSIDE
            }
            // test bottomside
            if (hitbox.endY >= blockY && hitbox.endY < blockY + TILE_SIZE) {
                ret += BLOCK_BOTTOMSIDE
            }
            // test rightside
            if (hitbox.endX >= blockX && hitbox.endX < blockX + TILE_SIZE) {
                ret += BLOCK_RIGHTSIDE
            }
            // test topside
            if (hitbox.startY >= blockY && hitbox.startY < blockY + TILE_SIZE) {
                ret += BLOCK_TOPSIDE
            }

            // cancel two superpositions (change 0b-numbers if you've changed side indices!)
            //if (ret and 0b1010 == 0b1010) ret = ret and 0b0101
            //if (ret and 0b0101 == 0b0101) ret = ret and 0b1010

            return ret
        }
        infix fun Int.hasSide(side: Int) = this and side != 0



        var bounceX = false
        var bounceY = false
        // collision NOT detected
        if (collidingStep == null) {
            hitbox.translate(vectorSum)
            // grounded = false
        }
        // collision detected
        else {
            //debug1("Collision detected")

            val newHitbox = hitbox.clone() // this line is wrong (must be hitbox.reassign(sixteenStep[collidingStep])) HOWEVER the following method is also wrong; think about the case where I am placed exactly in between two tiles)
            // see if four sides of hitbox CROSSES the tile
            // that information should be able to tell where the hitbox be pushed
            // blocks can have up to 4 status at once
            affectingTiles.forEach { blockAddr ->
                val blockCollStatus = getBlockCondition(newHitbox, blockAddr)

                if (blockCollStatus != 0) debug4("--> blockCollStat: $blockCollStatus")

                // displacements (no ELSE IFs!) superpositions are filtered in getBlockCondition()
                if (blockCollStatus hasSide BLOCK_LEFTSIDE) {
                    val displacement = TILE_SIZE - newHitbox.startX.modTileDelta()
                    newHitbox.translatePosX(displacement)
                    bounceX = true

                    debug4("--> leftside")
                }
                if (blockCollStatus hasSide BLOCK_RIGHTSIDE) {
                    val displacement = newHitbox.endX.modTileDelta()
                    newHitbox.translatePosX(displacement)
                    bounceX = true

                    debug4("--> rightside")
                }
                if (blockCollStatus hasSide BLOCK_TOPSIDE) {
                    val displacement = TILE_SIZE - newHitbox.startY.modTileDelta()
                    newHitbox.translatePosY(displacement)
                    bounceY = true

                    debug4("--> topside")
                }
                if (blockCollStatus hasSide BLOCK_BOTTOMSIDE) {
                    val displacement = newHitbox.endY.modTileDelta()
                    newHitbox.translatePosY(displacement)
                    bounceY = true

                    debug4("--> bottomside")
                }
            }


            hitbox.reassign(newHitbox)


            // bounce X/Y
            if (bounceX) {
                externalForce.x *= elasticity
                controllerMoveDelta?.let { controllerMoveDelta!!.x *= elasticity }
            }
            if (bounceY) {
                externalForce.y *= elasticity
                controllerMoveDelta?.let { controllerMoveDelta!!.y *= elasticity }
            }


            // grounded = true

        }// end of collision not detected*/




        // if collision not detected, just don't care; it's not your job to apply moveDelta
    }


    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    private fun isColliding(hitbox: Hitbox): Boolean {
        if (isNoCollideWorld) return false

        // detectors are inside of the bounding box
        // CONFIRMED
        val x1 = hitbox.startX
        val x2 = hitbox.endX - A_PIXEL
        val y1 = hitbox.startY
        val y2 = hitbox.endY - A_PIXEL
        // this commands and the commands on isWalled WILL NOT match (1 px gap on endX/Y). THIS IS INTENDED!


        val txStart = x1.div(TILE_SIZE).floorInt()
        val txEnd =   x2.div(TILE_SIZE).floorInt()
        val tyStart = y1.div(TILE_SIZE).floorInt()
        val tyEnd =   y2.div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd)
    }

    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    private fun isWalled(hitbox: Hitbox, option: Int): Boolean {
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

        IMPORTANT AF NOTE: things are ASYMMETRIC!
         */

        // AT LEAST THESE ARE CONFIRMED
        if (option == COLLIDING_TOP) {
            x1 = hitbox.startX
            x2 = hitbox.endX - A_PIXEL
            y1 = hitbox.startY - A_PIXEL
            y2 = y1
        }
        else if (option == COLLIDING_BOTTOM) {
            x1 = hitbox.startX
            x2 = hitbox.endX - A_PIXEL
            y1 = hitbox.endY + A_PIXEL
            y2 = y1
        }
        else if (option == COLLIDING_LEFT) {
            x1 = hitbox.startX - A_PIXEL
            x2 = x1
            y1 = hitbox.startY
            y2 = hitbox.endY - A_PIXEL
        }
        else if (option == COLLIDING_RIGHT) {
            x1 = hitbox.endX + A_PIXEL
            x2 = x1
            y1 = hitbox.startY
            y2 = hitbox.endY - A_PIXEL
        }
        else if (option == COLLIDING_ALLSIDE) {
            return isWalled(hitbox, COLLIDING_LEFT) || isWalled(hitbox, COLLIDING_RIGHT) ||
                   isWalled(hitbox, COLLIDING_BOTTOM) || isWalled(hitbox, COLLIDING_TOP)

        }
        else if (option == COLLIDING_LR) {
            return isWalled(hitbox, COLLIDING_LEFT) || isWalled(hitbox, COLLIDING_RIGHT)
        }
        else if (option == COLLIDING_UD) {
            return isWalled(hitbox, COLLIDING_BOTTOM) || isWalled(hitbox, COLLIDING_TOP)
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
            if (controllerMoveDelta!!.x < 0) {
                controllerMoveDelta!!.x += friction
                if (controllerMoveDelta!!.x > 0) controllerMoveDelta!!.x = 0.0
            }
            else if (controllerMoveDelta!!.x > 0) {
                controllerMoveDelta!!.x -= friction
                if (controllerMoveDelta!!.x < 0) controllerMoveDelta!!.x = 0.0
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
            if (controllerMoveDelta!!.y < 0) {
                controllerMoveDelta!!.y += friction
                if (controllerMoveDelta!!.y > 0) controllerMoveDelta!!.y = 0.0
            }
            else if (controllerMoveDelta!!.y > 0) {
                controllerMoveDelta!!.y -= friction
                if (controllerMoveDelta!!.y < 0) controllerMoveDelta!!.y = 0.0
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
                //clampW(hitbox.canonicalX),
                if (hitbox.canonicalX < 0)
                    hitbox.canonicalX + worldsizePxl
                else if (hitbox.canonicalX >= worldsizePxl)
                    hitbox.canonicalX - worldsizePxl
                else
                    hitbox.canonicalX, // ROUNDWORLD impl
                clampH(hitbox.canonicalY)
        )
    }

    override fun drawGlow(g: Graphics) {
        if (isVisible && spriteGlow != null) {
            blendLightenOnly()

            val offsetX = if (!spriteGlow!!.flippedHorizontal())
                hitboxTranslateX * scale + 1
            else
                spriteGlow!!.cellWidth * scale - (hitbox.width + hitboxTranslateX * scale) + 1

            val offsetY = spriteGlow!!.cellHeight * scale - hitbox.height - hitboxTranslateY * scale - 1

            if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
                // camera center neg, actor center pos
                spriteGlow!!.render(g,
                        (hitbox.startX - offsetX).toFloat() + world.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
                // camera center pos, actor center neg
                spriteGlow!!.render(g,
                        (hitbox.startX - offsetX).toFloat() - world.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                spriteGlow!!.render(g,
                        (hitbox.startX - offsetX).toFloat(),
                        (hitbox.startY - offsetY).toFloat(),
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
                hitboxTranslateX * scale + 1
            else
                sprite!!.cellWidth * scale - (hitbox.width + hitboxTranslateX * scale) + 1

            val offsetY = sprite!!.cellHeight * scale - hitbox.height - hitboxTranslateY * scale - 1

            if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
                // camera center neg, actor center pos
                sprite!!.render(g,
                        (hitbox.startX - offsetX).toFloat() + world.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
                // camera center pos, actor center neg
                sprite!!.render(g,
                        (hitbox.startX - offsetX).toFloat() - world.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        (scale).toFloat()
                )
            }
            else {
                sprite!!.render(g,
                        (hitbox.startX - offsetX).toFloat(),
                        (hitbox.startY - offsetY).toFloat(),
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
        for (y in tilewiseHitbox.startY.toInt()..tilewiseHitbox.endY.toInt()) {
            for (x in tilewiseHitbox.startX.toInt()..tilewiseHitbox.endX.toInt()) {
                tiles.add(world.getTileFromTerrain(x, y))
            }
        }

        return tiles.forEach(consumer)
    }

    private fun forEachOccupyingTile(consumer: (BlockProp?) -> Unit) {
        val tileProps = ArrayList<BlockProp?>()
        for (y in tilewiseHitbox.startY.toInt()..tilewiseHitbox.endY.toInt()) {
            for (x in tilewiseHitbox.startX.toInt()..tilewiseHitbox.endX.toInt()) {
                tileProps.add(BlockCodex[world.getTileFromTerrain(x, y)])
            }
        }

        return tileProps.forEach(consumer)
    }

    private fun forEachOccupyingTilePos(hitbox: Hitbox, consumer: (BlockAddress) -> Unit) {
        val newTilewiseHitbox =  Hitbox.fromTwoPoints(
                hitbox.startX.div(TILE_SIZE).floor(),
                hitbox.startY.div(TILE_SIZE).floor(),
                hitbox.endX.div(TILE_SIZE).floor(),
                hitbox.endY.div(TILE_SIZE).floor()
        )

        val tilePosList = ArrayList<BlockAddress>()
        for (y in newTilewiseHitbox.startY.toInt()..newTilewiseHitbox.endY.toInt()) {
            for (x in newTilewiseHitbox.startX.toInt()..newTilewiseHitbox.endX.toInt()) {
                tilePosList.add(LandUtil.getBlockAddr(x, y))
            }
        }

        return tilePosList.forEach(consumer)
    }

    private fun forEachFeetTileNum(consumer: (Int?) -> Unit) {
        val tiles = ArrayList<Int?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.startX.toInt()..tilewiseHitbox.endX.toInt()) {
            tiles.add(world.getTileFromTerrain(x, y))
        }

        return tiles.forEach(consumer)
    }

    private fun forEachFeetTile(consumer: (BlockProp?) -> Unit) {
        val tileProps = ArrayList<BlockProp?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in tilewiseHitbox.startX.toInt()..tilewiseHitbox.endX.toInt()) {
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