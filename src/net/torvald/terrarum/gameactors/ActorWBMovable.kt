package net.torvald.terrarum.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.BlockProp
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.*


/**
 * Actor with body movable. Base class for every actor that has animated sprites. This includes furnishings, paintings, gadgets, etc.
 * Also has all the usePhysics
 *
 * @param renderOrder Rendering order (BEHIND, MIDDLE, MIDTOP, FRONT)
 * @param immobileBody use realistic air friction (1/1000 of "unrealistic" canonical setup)
 * @param usePhysics use usePhysics simulation
 *
 * Created by minjaesong on 2016-01-13.
 */
open class ActorWBMovable(renderOrder: RenderOrder, val immobileBody: Boolean = false, var usePhysics: Boolean = true) :
        ActorWithBody(renderOrder) {


    val COLLISION_TEST_MODE = false

    /* !! ActorValue macros are on the very bottom of the source !! */

    /** This is GameWorld? only because the title screen also uses this thing as its camera;
     * titlescreen does not use instance of Ingame.
     */
    private val world: GameWorld?
        get() = Terrarum.ingame?.world

    
    @Transient internal var sprite: SpriteAnimation? = null
    @Transient internal var spriteGlow: SpriteAnimation? = null

    var drawMode = BlendMode.NORMAL

    var hitboxTranslateX: Int = 0// relative to spritePosX
        protected set
    var hitboxTranslateY: Int = 0// relative to spritePosY
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
    override val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0) // Hitbox is implemented using Double;

    /** half integer tilewise hitbox.
     * May hold width/height of zero; the end point should be inclusive!
     *
     * e.g. USE `for (x in hitbox.startX..hitbox.endX)`, NOT `for (x in hitbox.startX until hitbox.endX)`
     */ // got the idea from gl_FragCoord
    val hIntTilewiseHitbox: Hitbox
        get() = Hitbox.fromTwoPoints(
                hitbox.startX.plus(0.00001f).div(TILE_SIZE).floor() + 0.5f,
                hitbox.startY.plus(0.00001f).div(TILE_SIZE).floor() + 0.5f,
                hitbox.endX.plus(0.00001f).div(TILE_SIZE).floor() + 0.5f,
                hitbox.endY.plus(0.00001f).div(TILE_SIZE).floor() + 0.5f,
                true
            )


    /** May hold width/height of zero; the end point should be inclusive!
     *
     * e.g. USE `for (x in hitbox.startX..hitbox.endX)`, NOT `for (x in hitbox.startX until hitbox.endX)`
     */
    val intTilewiseHitbox: Hitbox
        get() = Hitbox.fromTwoPoints(
                hitbox.startX.plus(0.00001f).div(TILE_SIZE).floor(),
                hitbox.startY.plus(0.00001f).div(TILE_SIZE).floor(),
                hitbox.endX.plus(0.00001f).div(TILE_SIZE).floor(),
                hitbox.endY.plus(0.00001f).div(TILE_SIZE).floor(),
                true
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
        inline get() = (actorValue.getAsDouble(AVKey.SCALE) ?: 1.0) *
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
                printdbg(this, "input too small; using $MASS_LOWEST instead.")
                actorValue[AVKey.BASEMASS] = MASS_LOWEST
            }

            actorValue[AVKey.BASEMASS] = value / Math.pow(scale, 3.0)
        }*/
    @Transient val MASS_DEFAULT: Double = 60.0
    /** Valid range: [0, 1]  */
    var elasticity: Double = 0.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("invalid elasticity value $value; valid elasticity value is [0, 1].")
            else if (value >= ELASTICITY_MAX) {
                printdbg(this, "Elasticity were capped to $ELASTICITY_MAX.")
                field = ELASTICITY_MAX
            }
            else
                field = value * ELASTICITY_MAX
        }
    @Transient private val ELASTICITY_MAX = 1.0//0.993 // No perpetual motion!

    /**
     * what pretty much every usePhysics engine has, instead of my 'elasticity'
     *
     * This is just a simple macro for 'elasticity'.
     *
     * Formula: restitution = 1.0 - elasticity
     */
    var restitution: Double
        set(value) {
            elasticity = 1.0 - value
        }
        inline get() = 1.0 - elasticity

    var density = 1000.0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWBMovable] $value: density cannot be negative.")

            field = value
        }

    /**
     * Flags and Properties
     */

    private inline val grounded: Boolean
        get() = if (world == null) true else {
            isNoClip || 
            (world!!.gravitation.y > 0 && isWalled(hitbox, COLLIDING_BOTTOM) || 
             world!!.gravitation.y < 0 && isWalled(hitbox, COLLIDING_TOP))
        }
    /** Default to 'true'  */
    var isVisible = true
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
    var isChronostasis = false

    /**
     * Gravitational Constant G. Load from gameworld.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    private val gravitation: Vector2
            get() = world?.gravitation ?: Vector2(0.0, 9.8)
    @Transient val DRAG_COEFF_DEFAULT = 1.2
    /** Drag coefficient. Parachutes have much higher value than bare body (1.2) */
    var dragCoefficient: Double
        get() = actorValue.getAsDouble(AVKey.DRAGCOEFF) ?: DRAG_COEFF_DEFAULT
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWBMovable] drag coefficient cannot be negative.")
            actorValue[AVKey.DRAGCOEFF] = value
        }


    /**
     * Post-hit invincibility, in milliseconds
     */
    @Transient val INVINCIBILITY_TIME: Second = 0.5f

    @Transient internal val BASE_FRICTION = 0.3

    @Transient internal val BASE_FALLDAMAGE_DAMPEN = 50.0
    val fallDamageDampening: Double
        get() = BASE_FALLDAMAGE_DAMPEN * (actorValue.getAsDouble(AVKey.FALLDAMPENMULT) ?: 1.0)


    var collisionType = COLLISION_DYNAMIC

    //@Transient private val CCD_TICK = 1.0 / 16.0
    //@Transient private val CCD_TRY_MAX = 12800

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
    internal var walledLeft = false
    internal var walledRight = false
    internal var walledTop = false    // UNUSED; only for BasicDebugInfoWindow
    internal var walledBottom = false // UNUSED; only for BasicDebugInfoWindow
    internal var colliding = false
    
    protected inline val updateDelta: Float
        get() = Terrarum.deltaTime


    var isWalkingH = false
    var isWalkingV = false

    init {
        // some initialiser goes here...
    }

    fun makeNewSprite(textureRegionPack: TextureRegionPack) {
        sprite = SpriteAnimation(this)
        sprite!!.setSpriteImage(textureRegionPack)
    }

    fun makeNewSpriteGlow(textureRegionPack: TextureRegionPack) {
        spriteGlow = SpriteAnimation(this)
        spriteGlow!!.setSpriteImage(textureRegionPack)
    }

    /**
     * @param w
     * @param h
     * @param tx positive: translate sprite to LEFT.
     * @param ty positive: translate sprite to DOWN.
     * @see ActorWBMovable.drawBody
     * @see ActorWBMovable.drawGlow
     */
    fun setHitboxDimension(w: Int, h: Int, tx: Int, ty: Int) {
        baseHitboxH = h
        baseHitboxW = w
        hitboxTranslateX = tx
        hitboxTranslateY = ty
        hitbox.setDimension(w.toDouble(), h.toDouble())
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

    inline val centrePosVector: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.centeredY)
    inline val centrePosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.centeredY)
    inline val feetPosVector: Vector2
        get() = Vector2(hitbox.centeredX, hitbox.endY)
    inline val feetPosPoint: Point2d
        get() = Point2d(hitbox.centeredX, hitbox.endY)
    inline val feetPosTile: IntArray
        get() = intArrayOf(hIntTilewiseHitbox.centeredX.floorInt(), hIntTilewiseHitbox.endY.floorInt())

    override fun run() = update(updateDelta)

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

    override fun update(delta: Float) {
        if (isUpdate && !flagDespawn) {

            if (!assertPrinted) assertInit()

            if (sprite != null) sprite!!.update(delta)
            if (spriteGlow != null) spriteGlow!!.update(delta)

            // make NoClip work for player
            if (true) {//this == Terrarum.ingame!!.actorNowPlaying) {
                isNoSubjectToGrav = isNoClip || COLLISION_TEST_MODE
                isNoCollideWorld = isNoClip
                isNoSubjectToFluidResistance = isNoClip
            }

            if (!usePhysics) {
                isNoCollideWorld = true
                isNoSubjectToFluidResistance = true
                isNoSubjectToGrav = true
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
                if (!isNoCollideWorld) {
                    displaceHitbox()
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
                //if (isNoClip) { // TODO also hanging on the rope, etc.
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
            if (isNoCollideWorld) {
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

    fun getDrag(externalForce: Vector2): Vector2 {
        /**
         * weight; gravitational force in action
         * W = mass * G (9.8 [m/s^2])
         */
        val W: Vector2 = gravitation * Terrarum.PHYS_TIME_FRAME.toDouble()
        /**
         * Area
         */
        val A: Double = (scale * baseHitboxW / METER) * (scale * baseHitboxW / METER)
        /**
         * Drag of atmosphere
         * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity sqr) * A (area)
         */
        val D: Vector2 = Vector2(externalForce.x.magnSqr(), externalForce.y.magnSqr()) * dragCoefficient * 0.5 * A// * tileDensityFluid.toDouble()

        val V: Vector2 = (W - D) / Terrarum.PHYS_TIME_FRAME * SI_TO_GAME_ACC

        return V
    }

    /**
     * Event for collision (event gets fired when it collided with the world or other actors)
     *
     * This event may fired two or more times per update.
     */
    open fun collided(other: Array<CollisionMessage>) {
    }

    data class CollisionMessage(val targetID: Int, val AkspfisWorld: Boolean)

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is precessed separately.
     */
    private fun applyGravitation() {

        if (!isNoSubjectToGrav && !(gravitation.y > 0 && walledBottom || gravitation.y < 0 && walledTop)) {
            //if (!isWalled(hitbox, COLLIDING_BOTTOM)) {
            applyForce(getDrag(externalForce))
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

    private fun displaceHitbox() {
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

        if (world != null) {

            fun debug1(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (true) printdbg(this, wut)
            }

            fun debug2(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (false) printdbg(this, wut)
            }

            fun debug3(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (false) printdbg(this, wut)
            }

            fun debug4(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (false) printdbg(this, wut)
            }

            fun BlockAddress.isFeetTile(hitbox: Hitbox): Boolean {
                val (x, y) = LandUtil.resolveBlockAddr(world!!, this)
                val newTilewiseHitbox = Hitbox.fromTwoPoints(
                        hitbox.startX.div(TILE_SIZE).floor(),
                        hitbox.startY.div(TILE_SIZE).floor(),
                        hitbox.endX.minus(0.00001).div(TILE_SIZE).floor(),
                        hitbox.endY.minus(0.00001).div(TILE_SIZE).floor(),
                        true
                )

                       // offset 1 pixel to the down so that friction would work
                return (y == hitbox.endY.plus(1.0).div(TILE_SIZE).floorInt()) && // copied from forEachFeetTileNum
                       (x in newTilewiseHitbox.startX.toInt()..newTilewiseHitbox.endX.toInt()) // copied from forEachOccupyingTilePos
            }

            fun Double.modTile() = this.toInt().div(TILE_SIZE).times(TILE_SIZE)
            fun Double.modTileDelta() = this - this.modTile()


            val vectorSum = externalForce + controllerMoveDelta
            val ccdSteps = minOf(16, (vectorSum.magnitudeSquared / TILE_SIZE.sqr()).floorInt() + 1) // adaptive



            // * NEW idea: wall pushes the actors (ref. SM64 explained by dutch pancake) *
            // direction to push is determined by the velocity
            // proc:
            // 10 I detect being walled and displace myself
            // 11 There's 16 possible case so work all 16 (some can be merged obviously)
            // 12 Amount of displacement can be obtained with modTileDelta()
            // 13 isWalled() is confirmed to be working
            // 20 sixteenStep may be optional, I think, but it'd be good to have

            // ignore MOST of the codes below (it might be possible to recycle the structure??)
            // and the idea above has not yet implemented, and may never will. --Torvald, 2018-12-30

            val sixteenStep = (0..ccdSteps).map { hitbox.clone().translate(vectorSum * (it / ccdSteps.toDouble())) } // zeroth step is for special condition

            var collidingStep: Int? = null

            for (step in 1..ccdSteps) {

                val stepBox = sixteenStep[step]

                forEachOccupyingTilePos(stepBox) {
                    val tileCoord = LandUtil.resolveBlockAddr(world!!, it)
                    val tile = world!!.getTileFromTerrain(tileCoord.first, tileCoord.second) ?: Block.STONE

                    if (shouldICollideWithThis(tile) || (it.isFeetTile(stepBox) && shouldICollideWithThisFeet(tile))) {
                        collidingStep = step
                    }
                }

                if (collidingStep != null) break
            }


            val COLL_LEFTSIDE = 1
            val COLL_BOTTOMSIDE = 2
            val COLL_RIGHTSIDE = 4
            val COLL_TOPSIDE = 8

            var bounceX = false
            var bounceY = false
            var zeroX = false
            var zeroY = false
            // collision NOT detected
            if (collidingStep == null) {
                hitbox.translate(vectorSum)
                // grounded = false
            }
            // collision detected
            else {
                debug1("== Collision step: $collidingStep / $ccdSteps")


                val newHitbox = hitbox.reassign(sixteenStep[collidingStep!!])

                var selfCollisionStatus = 0
                if (isWalled(newHitbox, COLLIDING_LEFT)) selfCollisionStatus += COLL_LEFTSIDE   // 1
                if (isWalled(newHitbox, COLLIDING_RIGHT)) selfCollisionStatus += COLL_RIGHTSIDE  // 4
                if (isWalled(newHitbox, COLLIDING_TOP)) selfCollisionStatus += COLL_TOPSIDE    // 8
                if (isWalled(newHitbox, COLLIDING_BOTTOM)) selfCollisionStatus += COLL_BOTTOMSIDE // 2

                // fixme UP and RIGHT && LEFT and DOWN bug

                when (selfCollisionStatus) {
                    0     -> {
                        debug1("[ActorWBMovable] Contradiction -- collision detected by CCD, but isWalled() says otherwise")
                    }
                    5     -> {
                        zeroX = true
                    }
                    10    -> {
                        zeroY = true
                    }
                    15    -> {
                        newHitbox.reassign(sixteenStep[0]); zeroX = true; zeroY = true
                    }
                // one-side collision
                    1, 11 -> {
                        newHitbox.translatePosX(TILE_SIZE - newHitbox.startX.modTileDelta()); bounceX = true
                    }
                    4, 14 -> {
                        newHitbox.translatePosX(-newHitbox.endX.modTileDelta()); bounceX = true
                    }
                    8, 13 -> {
                        newHitbox.translatePosY(TILE_SIZE - newHitbox.startY.modTileDelta()); bounceY = true
                    }
                    2, 7  -> {
                        newHitbox.translatePosY(-newHitbox.endY.modTileDelta()); bounceY = true
                    }
                }


                // fire Collision Event with one/two/three-side collision
                // for the ease of writing, this jumptable is separated from above.
                when (selfCollisionStatus) {
                // TODO compose CollisionInfo and fire collided()
                }


                // two-side collision
                if (selfCollisionStatus in listOf(3, 6, 9, 12)) {
                    debug1("twoside collision $selfCollisionStatus")

                    // !! this code is based on Dyn4j Vector's coord system; V(1,0) -> 0, V(0,1) -> pi, V(0,-1) -> -pi !! //

                    // we can use selfCollisionStatus to tell which of those four side we care

                    // points to the EDGE of the tile in world dimension (don't use this directly to get tilewise coord!!)
                    val offendingTileWorldX = if (selfCollisionStatus in listOf(6, 12))
                        newHitbox.endX.div(TILE_SIZE).floor() * TILE_SIZE - 0.00001
                    else
                        newHitbox.startX.div(TILE_SIZE).ceil() * TILE_SIZE

                    // points to the EDGE of the tile in world dimension (don't use this directly to get tilewise coord!!)
                    val offendingTileWorldY = if (selfCollisionStatus in listOf(3, 6))
                        newHitbox.endY.div(TILE_SIZE).floor() * TILE_SIZE - 0.00001
                    else
                        newHitbox.startY.div(TILE_SIZE).ceil() * TILE_SIZE

                    val offendingHitboxPointX = if (selfCollisionStatus in listOf(6, 12))
                        newHitbox.endX
                    else
                        newHitbox.startX

                    val offendingHitboxPointY = if (selfCollisionStatus in listOf(3, 6))
                        newHitbox.endY
                    else
                        newHitbox.startY



                    val angleOfIncidence =
                            if (selfCollisionStatus in listOf(3, 9))
                                vectorSum.direction.toPositiveRad()
                            else
                                vectorSum.direction

                    val angleThreshold =
                            if (selfCollisionStatus in listOf(3, 9))
                                (Vector2(offendingHitboxPointX, offendingHitboxPointY) -
                                 Vector2(offendingTileWorldX, offendingTileWorldY)).direction.toPositiveRad()
                            else
                                (Vector2(offendingHitboxPointX, offendingHitboxPointY) -
                                 Vector2(offendingTileWorldX, offendingTileWorldY)).direction


                    debug1("vectorSum: $vectorSum, vectorDirRaw: ${vectorSum.direction / Math.PI}pi")
                    debug1("incidentAngle: ${angleOfIncidence / Math.PI}pi, threshold: ${angleThreshold / Math.PI}pi")


                    val displacementAbs = Vector2(
                            (offendingTileWorldX - offendingHitboxPointX).abs(),
                            (offendingTileWorldY - offendingHitboxPointY).abs()
                    )


                    // FIXME jump-thru-ceil bug on 1px-wide (the edge), case-9 collision (does not occur on case-12 coll.)


                    val displacementUnitVector =
                            if (angleOfIncidence == angleThreshold)
                                -vectorSum
                            else {
                                when (selfCollisionStatus) {
                                    3    -> if (angleOfIncidence > angleThreshold) Vector2(1.0, 0.0) else Vector2(0.0, -1.0)
                                    6    -> if (angleOfIncidence > angleThreshold) Vector2(0.0, -1.0) else Vector2(-1.0, 0.0)
                                    9    -> if (angleOfIncidence > angleThreshold) Vector2(0.0, 1.0) else Vector2(1.0, 0.0)
                                    12   -> if (angleOfIncidence > angleThreshold) Vector2(-1.0, 0.0) else Vector2(0.0, 1.0)
                                    else -> throw InternalError("Blame hardware or universe")
                                }
                            }

                    val finalDisplacement =
                            if (angleOfIncidence == angleThreshold)
                                displacementUnitVector
                            else
                                Vector2(
                                        displacementAbs.x * displacementUnitVector.x,
                                        displacementAbs.y * displacementUnitVector.y
                                )

                    newHitbox.translate(finalDisplacement)


                    debug1("displacement: $finalDisplacement")


                    // TODO: translate other axis proportionally to the incident vector

                    bounceX = angleOfIncidence == angleThreshold || displacementUnitVector.x != 0.0
                    bounceY = angleOfIncidence == angleThreshold || displacementUnitVector.y != 0.0

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
                if (zeroX) {
                    externalForce.x = 0.0
                    controllerMoveDelta?.let { controllerMoveDelta!!.x = 0.0 }
                }
                if (zeroY) {
                    externalForce.y = 0.0
                    controllerMoveDelta?.let { controllerMoveDelta!!.y = 0.0 }
                }


                hitbox.reassign(newHitbox)


                // slam-into-whatever damage (such dirty; much hack; wow)
                //                                                   vvvv hack (supposed to be 1.0)                           vvv 50% hack
                val collisionDamage = mass * (vectorSum.magnitude / (10.0 / Terrarum.PHYS_TIME_FRAME).sqr()) / fallDamageDampening.sqr() * GAME_TO_SI_ACC
                // kg * m / s^2 (mass * acceleration), acceleration -> (vectorMagn / (0.01)^2).gameToSI()
                if (collisionDamage != 0.0) debug1("Collision damage: $collisionDamage N")
                // FIXME instead of 0.5mv^2, we can model after "change of velocity (aka accel)", just as in real-life; big change of accel on given unit time is what kills


                // grounded = true
            }// end of collision not detected


            return



            // if collision not detected, just don't care; it's not your job to apply moveDelta

        }
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

        val txStart = x1.plus(0.5f).div(TILE_SIZE).floorInt()
        val txEnd =   x2.plus(0.5f).div(TILE_SIZE).floorInt()
        val tyStart = y1.plus(0.5f).div(TILE_SIZE).floorInt()
        val tyEnd =   y2.plus(0.5f).div(TILE_SIZE).floorInt()

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

        val txStart = x1.plus(0.5f).div(TILE_SIZE).floorInt()
        val txEnd =   x2.plus(0.5f).div(TILE_SIZE).floorInt()
        val tyStart = y1.plus(0.5f).div(TILE_SIZE).floorInt()
        val tyEnd =   y2.plus(0.5f).div(TILE_SIZE).floorInt()

        return isCollidingInternal(txStart, tyStart, txEnd, tyEnd, option == COLLIDING_BOTTOM)
    }

    private fun isCollidingInternal(txStart: Int, tyStart: Int, txEnd: Int, tyEnd: Int, feet: Boolean = false): Boolean {
        if (world == null) return false
        
        for (y in tyStart..tyEnd) {
            for (x in txStart..txEnd) {
                val tile = world!!.getTileFromTerrain(x, y) ?: Block.STONE

                if (feet) {
                    if (shouldICollideWithThisFeet(tile))
                        return true
                }
                else {
                    if (shouldICollideWithThis(tile))
                        return true
                }

                // this weird statement means that if's the condition is TRUE, return TRUE;
                // if the condition is FALSE, do nothing and let succeeding code handle it.
            }
        }

        return false
    }

    /**
     * If this tile should be treated as "collidable"
     *
     * Very straightforward for the actual solid tiles, not so much for the platforms
     */
    private fun shouldICollideWithThis(tile: Int) =
            // regular solid block
            (BlockCodex[tile].isSolid)

    /**
     * If this tile should be treated as "collidable"
     *
     * Just like "shouldICollideWithThis" but it's intended to work with feet tiles
     */
    private fun shouldICollideWithThisFeet(tile: Int) =
            // regular solid block
            (BlockCodex[tile].isSolid) ||
            // platforms, moving downward AND not "going down"
            (this is ActorHumanoid && BlockCodex[tile].isPlatform &&
             externalForce.y + (controllerMoveDelta?.y ?: 0.0) >= 0.0 &&
             !this.isDownDown && this.axisY <= 0f) ||
            // platforms, moving downward
            (this !is ActorHumanoid && BlockCodex[tile].isPlatform &&
             externalForce.y + (controllerMoveDelta?.y ?: 0.0) >= 0.0)
            // TODO: as for the platform, only apply it when it's a feet tile



    private fun getContactingAreaFluid(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        if (world == null) return 0

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
            if (world!!.getFluid(tileX, tileY).isFluid()) {
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
        val friction = if (isNoClip)
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
        val friction = if (isNoClip)
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

        if (!isNoClip && !grounded) {
            // System.out.println("density: "+density);
            veloY -= ((fluidDensity - this.density).toDouble()
                    * map.gravitation.toDouble() * submergedVolume.toDouble()
                    * Math.pow(mass.toDouble(), -1.0)
                    * SI_TO_GAME_ACC.toDouble()).toDouble()
        }
    }*/

    private inline val submergedRatio: Double
        get() {
            if (hitbox.height == 0.0) throw RuntimeException("Hitbox.height is zero")
            return submergedHeight / hitbox.height
        }
    private inline val submergedVolume: Double
        get() = submergedHeight * hitbox.width * hitbox.width

    private inline val submergedHeight: Double
        get() = Math.max(
                getContactingAreaFluid(COLLIDING_LEFT),
                getContactingAreaFluid(COLLIDING_RIGHT)
        ).toDouble()


    // body friction is always as same as the air. Fluid doesn't matter, they use viscosity
    //    (or viscosity was the wrong way and the friction DO matter? hmm... -- Torvald, 2018-12-31)
    //    (or perhaps... they work in tandem, viscocity slows things down, friction makes go and stop fast -- Torvald, 2019-01-01; not the proudest thing to do in the ney year)
    // going down the platform won't show abnormal slowing (it's because of the friction prop!)
    internal inline val bodyFriction: Double
        get() {
            /*var friction = 0.0
            forEachOccupyingTileNum {
                // get max friction
                if (getTileFriction(it ?: Block.AIR) > friction)
                    friction = getTileFriction(it ?: Block.AIR)
            }

            return friction*/
            return getTileFriction(Block.AIR)
        }
    // after all, feet friction is what it matters
    internal inline val feetFriction: Double
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

    internal inline val bodyViscosity: Int
        get() {
            var viscosity = 0
            forEachOccupyingTile {
                // get max viscosity
                if (it?.viscosity ?: 0 > viscosity)
                    viscosity = it?.viscosity ?: 0
            }

            return viscosity
        }
    internal inline val feetViscosity: Int
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

    internal inline val speedMultByTile: Double
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
    internal inline val accelMultMovement: Double
        get() {
            if (!isNoSubjectToFluidResistance && !isNoSubjectToGrav && !isNoCollideWorld) {
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
    private inline val tileDensityFluid: Int
        get() {
            var density = 0
            forEachOccupyingFluid {
                // get max density for each tile
                if (it?.isFluid() ?: false && it?.getProp()?.density ?: 0 > density) {
                    density = it?.getProp()?.density ?: 0
                }
            }

            return density
        }

    /**
     * Get highest density (specific gravity) value from tiles that the body occupies.
     * @return
     */
    private inline val tileDensity: Int
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
        if (world == null) return


        val worldsizePxl = world!!.width.times(TILE_SIZE)

        // DEAR FUTURE ME,
        //
        // this code potentially caused a collision bug which only happens near the "edge" of the world.
        // (x_tile: 2400..2433 with world_width = 2400)
        //
        // -- Signed, 2017-09-17

        // DEAR PAST ME AT 2017-09-23,
        //
        // I'm starting to thinking that actually fixing the negative-coord-bug in collision part (you know
        // it's caused by wrapping the values to the negative part internally, eh?) ought to be actually faster
        // to resolve this year-old issue
        //
        // Or maybe just allow cameraX (left point) to be negative number and fix the renderer (in which whole
        // tiles shifts to left)?
        //
        // It was interesting; 'fmod' in
        //     shader.setUniformi("cameraTranslation", WorldCamera.x % TILE_SIZE, WorldCamera.y % TILE_SIZE) // surprisingly, using 'fmod' instead of '%' doesn't work
        // broke it, had to use '%';
        // Also, in the lightmap renderer, I had to add this line when updating for_x_start:
        //     if (for_x_start < 0) for_x_start -= 1
        // Apparently this also fixes notorious jumping issue because hitbox position is changed (wrapped to
        // different coord?), which I'm not sure about
        //
        // Following issues are still remain/reintroduced:
        //     FIXME while in this "special" zone, leftmost column tiles are duplicated (prob related to < 0 camera)
        //     FIXME there's large grey box at block coord 0,0
        //
        // -- Unsigned, 2018-11-20


        // wrap around for X-axis
        //val actorMinimumX = Terrarum.HALFW // to make camera's X stay positive
        //val actorMaximumX = worldsizePxl + actorMinimumX // to make camera's X stay positive

        hitbox.setPositionFromPointed(
                if (hitbox.canonicalX >= worldsizePxl) // just wrap normally and allow camera coord to be negative
                    hitbox.canonicalX - worldsizePxl
                else if (hitbox.canonicalX < 0)
                    hitbox.canonicalX + worldsizePxl
                else
                    hitbox.canonicalX, // Fixed ROUNDWORLD impl
                /*if (hitbox.canonicalX < actorMinimumX)
                    hitbox.canonicalX + worldsizePxl
                else if (hitbox.canonicalX >= actorMaximumX)
                    hitbox.canonicalX - worldsizePxl
                else
                    hitbox.canonicalX, // ROUNDWORLD impl */
                clampH(hitbox.canonicalY)
        )
    }

    override fun drawGlow(batch: SpriteBatch) {
        if (isVisible && spriteGlow != null) {
            blendNormal()
            drawSpriteInGoodPosition(spriteGlow!!, batch)
        }
    }

    override fun drawBody(batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            if (!KeyToggler.isOn(Input.Keys.F12)) {
                BlendMode.resolve(drawMode)
                drawSpriteInGoodPosition(sprite!!, batch)
            }
            else {
                batch.color = Color.NAVY
                val hb = intTilewiseHitbox

                batch.fillRect(
                        hb.startX.toFloat() * TILE_SIZE,
                        hb.startY.toFloat() * TILE_SIZE,
                        hb.width.toFloat() * TILE_SIZE,
                        hb.height.toFloat() * TILE_SIZE
                )

                batch.color = Color.VIOLET
                batch.fillRect(hitbox.startX.toFloat(), hitbox.startY.toFloat(), hitbox.width.toFloat(), hitbox.height.toFloat())
            }
        }
    }

    private fun drawSpriteInGoodPosition(sprite: SpriteAnimation, batch: SpriteBatch) {
        if (world == null) return


        val leftsidePadding = world!!.width.times(TILE_SIZE) - WorldCamera.width.ushr(1)
        val rightsidePadding = WorldCamera.width.ushr(1)

        val offsetX = hitboxTranslateX * scale
        val offsetY = sprite.cellHeight * scale - hitbox.height - hitboxTranslateY * scale - 1


        if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
            // camera center neg, actor center pos
            sprite.render(batch,
                    (hitbox.startX - offsetX).toFloat() + world!!.width * TILE_SIZE,
                    (hitbox.startY - offsetY).toFloat(),
                    (scale).toFloat()
            )
        }
        else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
            // camera center pos, actor center neg
            sprite.render(batch,
                    (hitbox.startX - offsetX).toFloat() - world!!.width * TILE_SIZE,
                    (hitbox.startY - offsetY).toFloat(),
                    (scale).toFloat()
            )
        }
        else {
            sprite.render(batch,
                    (hitbox.startX - offsetX).toFloat(),
                    (hitbox.startY - offsetY).toFloat(),
                    (scale).toFloat()
            )
        }
    }

    override fun onActorValueChange(key: String, value: Any?) {
        // do nothing
    }




    private fun clampW(x: Double): Double =
            if (world == null) x
            else if (x < TILE_SIZE + hitbox.width / 2) {
                TILE_SIZE + hitbox.width / 2
            }
            else if (x >= (world!!.width * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - hitbox.width / 2) {
                (world!!.width * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - hitbox.width / 2
            }
            else {
                x
            }

    private fun clampH(y: Double): Double =
            if (world == null) y
            else if (y < TILE_SIZE + hitbox.height) {
                TILE_SIZE + hitbox.height
            }
            else if (y >= (world!!.height * TILE_SIZE).toDouble() - TILE_SIZE.toDouble() - hitbox.height) {
                (world!!.height * TILE_SIZE).toDouble() - 1.0 - TILE_SIZE.toDouble() - hitbox.height
            }
            else {
                y
            }

    private fun clampWtile(x: Int): Int =
            if (x < 0) 0 else if (x >= world?.width ?: 0) (world?.width ?: 0) - 1 else x

    private fun clampHtile(x: Int): Int =
            if (x < 0) 0 else if (x >= world?.height ?: 0) (world?.height ?: 0) - 1 else x


    var isNoClip: Boolean = false
        set(value) {
            field = value

            if (value) {
                externalForce.zero()
                controllerMoveDelta?.zero()
            }
        }

    private val AUTO_CLIMB_RATE: Int
        get() = Math.min(TILE_SIZE / 8 * Math.sqrt(scale), TILE_SIZE.toDouble()).toInt()

    private fun assertInit() {
        // errors
        if (baseHitboxW == 0 || baseHitboxH == 0)
            throw Error("Hitbox dimension was not set. (don't modify hitbox directly -- use 'setHitboxDimension()')")

        // warnings
        if (sprite == null && isVisible)
            printdbg(this, "Caution: actor ${this.javaClass.simpleName} is visible but the sprite was not set.")
        else if (sprite != null && !isVisible)
            printdbg(this, "Caution: actor ${this.javaClass.simpleName} is invisible but the sprite was given.")

        assertPrinted = true
    }

    internal fun flagDespawn() {
        flagDespawn = true
    }


    private fun forEachOccupyingTileNum(consumer: (Int?) -> Unit) {
        if (world == null) return
        
        
        val tiles = ArrayList<Int?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tiles.add(world!!.getTileFromTerrain(x, y))
            }
        }

        return tiles.forEach(consumer)
    }

    private fun forEachOccupyingTile(consumer: (BlockProp?) -> Unit) {
        if (world == null) return

        
        val tileProps = ArrayList<BlockProp?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tileProps.add(BlockCodex[world!!.getTileFromTerrain(x, y)])
            }
        }

        return tileProps.forEach(consumer)
    }

    private fun forEachOccupyingFluid(consumer: (GameWorld.FluidInfo?) -> Unit) {
        if (world == null) return


        val fluids = ArrayList<GameWorld.FluidInfo?>()

        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                fluids.add(world!!.getFluid(x, y))
            }
        }

        return fluids.forEach(consumer)
    }

    private fun forEachOccupyingTilePos(hitbox: Hitbox, consumer: (BlockAddress) -> Unit) {
        if (world == null) return

        
        val newTilewiseHitbox = Hitbox.fromTwoPoints(
                hitbox.startX.div(TILE_SIZE).floor(),
                hitbox.startY.div(TILE_SIZE).floor(),
                hitbox.endX.minus(0.00001).div(TILE_SIZE).floor(),
                hitbox.endY.minus(0.00001).div(TILE_SIZE).floor(),
                true
        ) // NOT the same as intTilewiseHitbox !!

        val tilePosList = ArrayList<BlockAddress>()
        for (y in newTilewiseHitbox.startY.toInt()..newTilewiseHitbox.endY.toInt()) {
            for (x in newTilewiseHitbox.startX.toInt()..newTilewiseHitbox.endX.toInt()) {
                tilePosList.add(LandUtil.getBlockAddr(world!!, x, y))
            }
        }

        return tilePosList.forEach(consumer)
    }

    private fun forEachFeetTileNum(consumer: (Int?) -> Unit) {
        if (world == null) return

        
        val tiles = ArrayList<Int?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
            tiles.add(world!!.getTileFromTerrain(x, y))
        }

        return tiles.forEach(consumer)
    }

    private fun forEachFeetTile(consumer: (BlockProp?) -> Unit) {
        if (world == null) return

        
        val tileProps = ArrayList<BlockProp?>()

        // offset 1 pixel to the down so that friction would work
        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorInt()

        for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
            tileProps.add(BlockCodex[world!!.getTileFromTerrain(x, y)])
        }

        return tileProps.forEach(consumer)
    }



    companion object {

        /**
         * Constants
         */

        @Transient val METER = 24.0
        /**
         * [m / s^2] * SI_TO_GAME_ACC -> [px / InternalFrame^2]
         */
        @Transient val SI_TO_GAME_ACC = METER / (Terrarum.PHYS_TIME_FRAME * Terrarum.PHYS_TIME_FRAME)
        /**
         * [m / s] * SI_TO_GAME_VEL -> [px / InternalFrame]
         */
        @Transient val SI_TO_GAME_VEL = METER / Terrarum.PHYS_TIME_FRAME

        /**
         * [px / InternalFrame^2] * GAME_TO_SI_ACC -> [m / s^2]
         */
        @Transient val GAME_TO_SI_ACC = (Terrarum.PHYS_TIME_FRAME * Terrarum.PHYS_TIME_FRAME) / METER


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

        @Transient val TILE_SIZE = FeaturesDrawer.TILE_SIZE
        @Transient val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

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
    internal val avStrength: Double
        get() = (actorValue.getAsDouble(AVKey.STRENGTH) ?: 1000.0) *
                (actorValue.getAsDouble(AVKey.STRENGTHBUFF) ?: 1.0) * scale
    internal var avBaseStrength: Double?
        get() = actorValue.getAsDouble(AVKey.STRENGTH)
        set(value) {
            actorValue[AVKey.STRENGTH] = value!!
        }
    internal var avBaseMass: Double
        inline get() = actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT
        set(value) {
            if (value <= 0 || value.isNaN() || value.isInfinite())
                throw IllegalArgumentException("Tried to set base mass to invalid value ($value)")
            actorValue[AVKey.BASEMASS] = value
        }
    internal val avAcceleration: Double
        get() { if (accelMultMovement.isNaN()) println("accelMultMovement: $accelMultMovement")
            return actorValue.getAsDouble(AVKey.ACCEL)!! *
                (actorValue.getAsDouble(AVKey.ACCELBUFF) ?: 1.0) *
                accelMultMovement *
                scale.sqrt()
        }
    internal val avSpeedCap: Double
        get() = actorValue.getAsDouble(AVKey.SPEED)!! *
                (actorValue.getAsDouble(AVKey.SPEEDBUFF) ?: 1.0) *
                speedMultByTile *
                scale.sqrt()

    private fun Double.toPositiveRad() = // rad(0..pi, -pi..0) -> rad(0..2pi)
            if (-Math.PI <= this && this < 0.0)
                this + 2 * Math.PI
            else
                this

    override fun dispose() {
        sprite?.dispose()
        spriteGlow?.dispose()
    }
}

