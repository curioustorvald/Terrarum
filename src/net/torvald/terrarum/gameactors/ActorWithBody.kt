package net.torvald.terrarum.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockProp
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.PHYS_EPSILON_DIST
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameparticles.createRandomBlockParticle
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.*
import kotlin.math.*


/**
 * Actor with body movable; or more like, 'Actor that has defined XY-Position'. Base class for every actor that has animated sprites. This includes furnishings, paintings, gadgets, etc.
 * Also has all the usePhysics
 *
 * @param renderOrder Rendering order (BEHIND, MIDDLE, MIDTOP, FRONT)
 * @param physProp physics properties
 * @param id use custom ActorID
 *
 * Created by minjaesong on 2016-01-13.
 */
open class ActorWithBody : Actor {

    var physProp = PhysProperties.HUMANOID_DEFAULT()

    // copied from old interface Luminous
    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     *
     * NOTE: MUST NOT SERIALISE (use `@Transient`)
     */
    open var lightBoxList: ArrayList<Lightbox> = arrayListOf() // must use ArrayList: has no-arg constructor

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     *
     * NOTE: MUST NOT SERIALISE (use `@Transient`)
     */
    open var shadeBoxList: ArrayList<Lightbox> = arrayListOf() // must use ArrayList: has no-arg constructor
    // end of Luminous

    protected constructor() : super()

    constructor(renderOrder: RenderOrder, physProp: PhysProperties, id: ActorID? = null) : super(renderOrder, id) {
        this.physProp = physProp
    }

    @Transient val COLLISION_TEST_MODE = false

    /* !! ActorValue macros are on the very bottom of the source !! */

    /** This is GameWorld? only because the title screen also uses this thing as its camera;
     * titlescreen does not use instance of Ingame.
     */
    protected val world: GameWorld?
        get() = Terrarum.ingame?.world


    @Transient var sprite: SpriteAnimation? = null
    @Transient var spriteGlow: SpriteAnimation? = null
    @Transient var spriteEmissive: SpriteAnimation? = null

    var drawMode = BlendMode.NORMAL

    open var isStationary: Boolean = true
        protected set
    open var tooltipText: String? = null // null: display nothing
    val mouseUp: Boolean
        get() = hitbox.containsPoint((world?.width ?: 0) * TILE_SIZED, Terrarum.mouseX, Terrarum.mouseY)

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
    open val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0) // Hitbox is implemented using Double;

    /** half integer tilewise hitbox.
     * Used by physics-related shits.
     *
     * e.g. USE `for (x in hitbox.startX..hitbox.endX)`, NOT `for (x in hitbox.startX until hitbox.endX)`
     */ // got the idea from gl_FragCoord
    val hIntTilewiseHitbox: Hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)
        /*get() = Hitbox.fromTwoPoints(
                hitbox.startX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor() + 0.5,
                hitbox.startY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor() + 0.5,
                hitbox.endX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor() + 0.5,
                hitbox.endY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor() + 0.5,
                true
        )*/


    /** Used by non-physics shits. (e.g. BlockBase determining "occupied" blocks)
     *
     * e.g. USE `for (x in hitbox.startX..hitbox.endX)`, NOT `for (x in hitbox.startX until hitbox.endX)`
     */
    val intTilewiseHitbox: Hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)
        /*get() = Hitbox.fromTwoPoints(
                hitbox.startX.div(TILE_SIZE).floor(),
                hitbox.startY.div(TILE_SIZE).floor(),
                hitbox.endX.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor(),
                hitbox.endY.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floor(),
                true
        )*/

    /**
     * Unit: Pixels per 1/60 (or AppLoader.UPDATE_RATE) seconds.
     *
     * When the engine resolves this value, the framerate must be accounted for. E.g.:
     *  3.0 is resolved as 3.0 if FPS is 60, but the same value should be resolved as 6.0 if FPS is 30.
     *  v_resolved = v * (60/FPS) or, v * (60 * delta_t)
     * (Use this code verbatim: '(Terrarum.PHYS_REF_FPS * delta)')
     *
     *
     * Elevators/Movingwalks/etc.: edit hitbox manually!
     *
     * Velocity vector for newtonian sim.
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     *
     * V for Velocity!
     */
    internal val externalV = Vector2(0.0, 0.0)

    @Transient private val VELO_HARD_LIMIT = 100.0

    /**
     * Unit: Pixels per 1/60 (or AppLoader.UPDATE_RATE) seconds.
     *
     * for "Controllable" actors
     *
     * V for Velocity!
     */
    var controllerV: Vector2? = if (this is Controllable) Vector2() else null

    // not sure we need this...
    //var jumpable = true // this is kind of like "semaphore"

    /**
     * Physical properties.
     */
    /** Apparent scale. Use "avBaseScale" for base scale */
    val scale: Double
        inline get() = (actorValue.getAsDouble(AVKey.SCALE) ?: 1.0) *
                       (actorValue.getAsDouble(AVKey.SCALEBUFF) ?: 1.0)
        /*set(value) {
            val scaleDelta = value - scale
            actorValue[AVKey.SCALE] = value / (actorValue.getAsDouble(AVKey.SCALEBUFF) ?: 1.0)
            // reposition
            translatePosition(-baseHitboxW * scaleDelta / 2, -baseHitboxH * scaleDelta)
        }*/
    @Transient val MASS_LOWEST = 0.1 // Kilograms
    /** Apparent mass. Use "avBaseMass" for base mass */
    val mass: Double
        get() = (actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT) * Math.pow(scale, 3.0)
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
    /**
     * Apparent strength scaled so that 1.0 is default value
     */
    val avStrengthNormalised: Double
        get() = avStrength / 1000.0
    var avBaseStrength: Double?
        get() = actorValue.getAsDouble(AVKey.STRENGTH)
        set(value) {
            actorValue[AVKey.STRENGTH] = value!!
        }
    var avBaseMass: Double
        inline get() = actorValue.getAsDouble(AVKey.BASEMASS) ?: MASS_DEFAULT
        set(value) {
            if (value <= 0 || value.isNaN() || value.isInfinite())
                throw IllegalArgumentException("Tried to set base mass to invalid value ($value)")
            actorValue[AVKey.BASEMASS] = value
        }
    val avAcceleration: Double
        get() { if (accelMultMovement.isNaN()) println("accelMultMovement: $accelMultMovement")
            return actorValue.getAsDouble(AVKey.ACCEL)!! *
                   (actorValue.getAsDouble(AVKey.ACCELBUFF) ?: 1.0) *
                   accelMultMovement *
                   scale.sqrt()
        }
    val ingamePlayersCanBeEncumbered: Boolean
        get() = if (this is IngamePlayer)
            !actorValue.getAsString(AVKey.GAMEMODE).isNullOrBlank()
        else true

    val encumberment: Double
        get() = if (this is Pocketed && ingamePlayersCanBeEncumbered) this.inventory.encumberment else 0.0

    val avSpeedCap: Double
        get() = actorValue.getAsDouble(AVKey.SPEED)!! * // base stat
                (actorValue.getAsDouble(AVKey.SPEEDBUFF) ?: 1.0) * // buffed stat
                speedMultByTile * // tile-specific
                scale.sqrt() * // taller actors have longer legs but also receives relatively weaker gravity -> pow(0.5)
                ((encumberment / avStrengthNormalised).pow(-2.0/3.0)).coerceIn(0.1, 1.0) // encumbered actors move slower

    /**
     * Flags and Properties
     */

    private inline val grounded: Boolean
        get() = if (world == null) true else {
            isNoClip ||
            (world!!.gravitation.y > 0 && isWalled(hitbox, COLLIDING_BOTTOM) ||
             world!!.gravitation.y < 0 && isWalled(hitbox, COLLIDING_TOP))
        }
    /**
     * Toggles rendering
     * Default to 'true'  */
    var isVisible = true
    /**
     * Toggles the actual update
     * Default to 'true'  */
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
     * if set to TRUE, the ingame will not move the actor into the active list.
     *
     * This flag will override `chunkAnchoring` flag (in this case, the chunk will be anchored but the actor will be dormant)
     */
    var forceDormant = false

    var isPickedUp = false

    /**
     * Redundant entry for ActorMovingPlatform.actorsRiding. This field must be modified by the platforms!
     *
     * Also see [net.torvald.terrarum.modulebasegame.gameactors.ActorMovingPlatform.actorsRiding]
     */
    @Transient protected val platformsRiding = ArrayList<ActorID>()

    /**
     * Gravitational Constant G. Load from gameworld.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     *
     * NOTE: this property is "var" so that future coder can implement the "reverse gravity potion"
     */
    var gravitation: Vector2 = world?.gravitation ?: GameWorld.DEFAULT_GRAVITATION
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

    var collisionType = COLLISION_DYNAMIC

    //@Transient private val CCD_TICK = 1.0 / 16.0
    //@Transient private val CCD_TRY_MAX = 12800

    // just some trivial magic numbers
    @Transient val A_PIXEL = 1.0
    @Transient val HALF_PIXEL = 0.5

    @Transient val COLLIDING_LEFT = 1
    @Transient val COLLIDING_BOTTOM = 2
    @Transient val COLLIDING_RIGHT = 4
    @Transient val COLLIDING_TOP = 8

    @Transient val COLLIDING_UD = 10
    @Transient val COLLIDING_LR = 5
    @Transient val COLLIDING_ALLSIDE = 15
    //@Transient private val COLLIDING_LEFT_EXTRA = 7
    //@Transient private val COLLIDING_RIGHT_EXTRA = 7

    /**
     * Temporary variables
     */

    @Transient private var assertPrinted = false

    internal var walledLeft = false
    internal var walledRight = false
    internal var walledTop = false    // UNUSED; only for BasicDebugInfoWindow
    internal var walledBottom = false // UNUSED; only for BasicDebugInfoWindow
    internal var colliding = false

    var isWalkingH = false
    var isWalkingV = false

    private var stairPenaltyMax = 1
    private var stairPenaltyCounter = 0 // unit: update count. 1 second is roughly 64 updates.
    private var stairPenaltyVector = 1.0


    /**
     * 0: None
     * 1: 1x1
     * 2: 3x3
     * 3: 5x5
     * ...
     * n: (2n-1)x(2n-1)
     */
    @Transient open val chunkAnchorRange: Int = 0

    /**
     * Should nearby chunks be kept in the chunk pool even if the player is far away.
     *
     * `ActorWithBody.forceDormant` will IGNORE this flag.
     */
    @Transient open var chunkAnchoring = false

    init {
        // some initialiser goes here...
    }

    fun makeNewSprite(textureRegionPack: TextureRegionPack): SheetSpriteAnimation {
        sprite = SheetSpriteAnimation(this).also {
            it.setSpriteImage(textureRegionPack)
        }
        return sprite as SheetSpriteAnimation
    }

    fun makeNewSpriteGlow(textureRegionPack: TextureRegionPack): SheetSpriteAnimation {
        spriteGlow = SheetSpriteAnimation(this).also {
            it.setSpriteImage(textureRegionPack)
        }
        return spriteGlow as SheetSpriteAnimation
    }

    fun makeNewSpriteEmissive(textureRegionPack: TextureRegionPack): SheetSpriteAnimation {
        spriteEmissive = SheetSpriteAnimation(this).also {
            it.setSpriteImage(textureRegionPack)
        }
        return spriteEmissive as SheetSpriteAnimation
    }

    /**
     * ONLY FOR INITIAL SETUP
     *
     * For the fixtures, `ty` of `0` will plant the fixture to the ground (they will spawn 1 px lower). Use value `1` to spawn them flush to the block grids.
     *
     * @param w
     * @param h
     * @param tx positive: translate sprite to LEFT.
     * @param ty positive: translate sprite to DOWN.
     */
    fun setHitboxDimension(w: Int, h: Int, tx: Int, ty: Int) {
        baseHitboxH = h
        baseHitboxW = w
        hitboxTranslateX = tx
        hitboxTranslateY = ty - 1 // plant the fixture to the ground
        hitbox.setDimension(w.toDouble(), h.toDouble())
    }

    fun setPosition(pos: Point2d) = setPosition(pos.x, pos.y)
    fun setPosition(pos: Vector2) = setPosition(pos.x, pos.y)
    fun setPositionFromCentrePoint(pos: Vector2) = setPosition(pos.x, pos.y + (hitbox.height) / 2)

    /**
     * ONLY FOR INITIAL SETUP
     *
     * Set hitbox position from bottom-center point
     * @param x
     * @param y
     */
    fun setPosition(x: Double, y: Double) {
        hitbox.setPositionFromPointed(x, y)
    }

    // get() methods are moved to update(), too much stray object being created is definitely not good
    @Transient val centrePosVector: Vector2 = Vector2(0.0,0.0)
        //get() = Vector2(hitbox.centeredX, hitbox.centeredY)
    @Transient val centrePosPoint: Point2d = Point2d(0.0, 0.0)
        //get() = Point2d(hitbox.centeredX, hitbox.centeredY)
    @Transient val feetPosVector: Vector2 = Vector2(0.0,0.0)
        //get() = Vector2(hitbox.centeredX, hitbox.endY)
    @Transient val feetPosPoint: Point2d = Point2d(0.0,0.0)
        //get() = Point2d(hitbox.centeredX, hitbox.endY)
    @Transient val feetPosTile: Point2i = Point2i(0,0)
        //get() = Point2i(hIntTilewiseHitbox.centeredX.floorToInt(), hIntTilewiseHitbox.endY.floorToInt())

    override fun run() = update(App.UPDATE_RATE)

    /**
     * Add vector value to the velocity, in the time unit of single frame.
     *
     * Since we're adding some value every frame, the value is equivalent to the acceleration.
     * Look for Newton's second law for the background knowledge.
     * @param acc acceleration in Vector2, the unit is [px / InternalFrame^2]
     */
    fun applyAcceleration(acc: Vector2) {
        externalV += acc
    }

    private fun Vector2.applyViscoseDrag() {
        val viscosity = bodyViscosity
        val divisor = 1.0 + (viscosity / 16.0)

        this.x /= divisor
        this.y /= divisor
    }

    private val bounceDampenVelThreshold = 0.5

    /**
     * Used on final loading phase, move the player to the opposite direction of the gravitation, until the player's
     * not colliding
     */
    open fun tryUnstuck() {
        val newHitbox = hitbox.clone()
        val translation = gravitation.setMagnitude(-1.0)

        while (isColliding(newHitbox)) {
            newHitbox.translate(translation)
        }

        hitbox.reassign(newHitbox)

        hIntTilewiseHitbox.setFromTwoPoints(
            hitbox.startX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.startY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.endX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.endY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5
        )
        intTilewiseHitbox.setFromTwoPoints(
            hitbox.startX.div(TILE_SIZE).floorToDouble(),
            hitbox.startY.div(TILE_SIZE).floorToDouble(),
            hitbox.endX.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
            hitbox.endY.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble()
        )

        centrePosVector.set(hitbox.centeredX, hitbox.centeredY)
        centrePosPoint.set(hitbox.centeredX, hitbox.centeredY)
        feetPosVector.set(hitbox.centeredX, hitbox.endY)
        feetPosPoint.set(hitbox.centeredX, hitbox.endY)
        feetPosTile.set(hIntTilewiseHitbox.centeredX.floorToInt(), hIntTilewiseHitbox.endY.floorToInt())
    }

    override fun updateImpl(delta: Float) {
        // re-scale hitbox
        // it's just much better to poll them than use perfectly-timed setter because latter simply cannot exist
        hitbox.canonicalResize(baseHitboxW * scale, baseHitboxH * scale)

        val oldHitbox = hitbox.clone()

        if (density < 100.0) density = 100.0

        if (isUpdate && !flagDespawn) {
            if (!assertPrinted) assertInit()

            if (sprite != null) sprite!!.update(delta)
            if (spriteGlow != null) spriteGlow!!.update(delta)
            if (spriteEmissive != null) spriteEmissive!!.update(delta)

            // make NoClip work for player
            if (true) {//this == INGAME.actorNowPlaying) {
                isNoSubjectToGrav = isNoClip || COLLISION_TEST_MODE
                isNoCollideWorld = isNoClip
                isNoSubjectToFluidResistance = isNoClip
            }

            if (!physProp.usePhysics) {
                isNoCollideWorld = true
                isNoSubjectToFluidResistance = true
                isNoSubjectToGrav = true
            }


            ////////////////////////////////////////////////////////////////
            // Codes that modifies velocity (moveDelta and externalV) //
            ////////////////////////////////////////////////////////////////

            // --> Apply more forces <-- //
            // Actors are subject to the gravity and the buoyancy if they are not levitating
            if (!isNoSubjectToGrav) {
                applyGravitation()
                applyBuoyancy()
            }


            // hard limit velocity
            externalV.x = externalV.x.bipolarClamp(VELO_HARD_LIMIT) // displaceHitbox SHOULD use moveDelta
            externalV.y = externalV.y.bipolarClamp(VELO_HARD_LIMIT)

            if (!isChronostasis) {
                ///////////////////////////////////////////////////
                // Codes that (SHOULD) displaces hitbox directly //
                ///////////////////////////////////////////////////

                val vecSum = externalV + controllerV
                /**
                 * solveCollision()?
                 * If and only if:
                 *     This body is NON-STATIC and the other body is STATIC
                 */
                if (!isNoCollideWorld) {
                    val (collisionStatus, collisionDamage) = displaceHitbox(true)


                    if (collisionStatus != 0 && collisionDamage >= 1.0) {
                        val terrainDamage = collisionDamage / 1000.0
                        getWalledTiles(hitbox, collisionStatus).let {
                            if (it.isNotEmpty()) {
//                                printdbg(this, "Dmg to terrain: $terrainDamage, affected: ${it.size}")
//                                printdbg(this, it)
                                val dmgPerTile = terrainDamage / it.size
                                it.forEach { (x, y) ->
                                    world?.inflictTerrainDamage(x, y, dmgPerTile, false)
                                }
                            }
                        }
                    }

                    // make some effects
                    if (collisionStatus != 0)
                        makeDust(collisionDamage, vecSum)
                    if (collisionStatus and COLLIDING_BOTTOM != 0)
                        makeNoise(collisionDamage)
                }
                else {
                    stairPenaltyCounter = 999
                    hitbox.translate(vecSum)
                }

                //////////////////////////////////////////////////////////////
                // Codes that modifies velocity (after hitbox displacement) //
                //////////////////////////////////////////////////////////////

                // modified vectors below SHOULD NOT IMMEDIATELY APPLIED!! //
                // these are FOR THE NEXT ROUND of update                  //
                // ((DO NOT DELETE THIS; made same mistake twice already)) //

                // TODO less friction for non-animating objects (make items glide far more on ice)

                // FIXME asymmetry on friction
                setHorizontalFriction() // friction SHOULD use and alter externalV
                //if (isNoClip) { // TODO also hanging on the rope, etc.
                setVerticalFriction()
                //}


                // make sure if the actor tries to go out of the map, loop back instead
                clampHitbox()

                if (stairPenaltyCounter < 999) stairPenaltyCounter += 1



            }


            // update all the other variables //

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

            hIntTilewiseHitbox.setFromTwoPoints(
                    hitbox.startX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
                    hitbox.startY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
                    hitbox.endX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
                    hitbox.endY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5
            )
            intTilewiseHitbox.setFromTwoPoints(
                    hitbox.startX.div(TILE_SIZE).floorToDouble(),
                    hitbox.startY.div(TILE_SIZE).floorToDouble(),
                    hitbox.endX.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
                    hitbox.endY.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble()
            )

            centrePosVector.set(hitbox.centeredX, hitbox.centeredY)
            centrePosPoint.set(hitbox.centeredX, hitbox.centeredY)
            feetPosVector.set(hitbox.centeredX, hitbox.endY)
            feetPosPoint.set(hitbox.centeredX, hitbox.endY)
            feetPosTile.set(hIntTilewiseHitbox.centeredX.floorToInt(), hIntTilewiseHitbox.endY.floorToInt())

            submergedHeight = getSubmergedHeight() // pixels
            submergedRatio = if (hitbox.height == 0.0) throw RuntimeException("Hitbox.height is zero")
                    else submergedHeight / hitbox.height


            if (mouseUp && tooltipText != null && !tooltipAcquired()) {
                acquireTooltip(tooltipText)
            }
        }

        if (tooltipText == null || !mouseUp || flagDespawn) {
            releaseTooltip()
        }

//        isStationary = (hitbox - oldHitbox).magnitudeSquared < PHYS_EPSILON_VELO
        isStationary = isCloseEnough(hitbox.startX, oldHitbox.startX) && // this is supposed to be more accurate, idk
                       isCloseEnough(hitbox.startY, oldHitbox.startY)


        if (this is IngamePlayer) {
//            printdbg(this, "Submerged=$submergedVolume   rho=$tileDensityFluid")
        }
    }

    fun getDrag(externalForce: Vector2): Vector2 {
        /**
         * weight; gravitational force in action
         * W = mass * G (9.8 [m/s^2])
         */
        val W: Vector2 = gravitation * Terrarum.PHYS_TIME_FRAME
        /**
         * Area
         */
        val A: Double = (scale.sqrt() * baseHitboxW / METER).sqr() // this is not physically accurate but it's needed to make large playable characters more controllable
        /**
         * Drag of atmosphere
         * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity sqr) * A (area)
         */
        val D: Vector2 = Vector2(externalForce.x.magnSqr(), externalForce.y.magnSqr()) * dragCoefficient * 0.5 * A// * tileDensityFluid.toDouble()

        val V: Vector2 = (W - D) / Terrarum.PHYS_TIME_FRAME * SI_TO_GAME_ACC

        return V

        // FIXME v * const, where const = 1.0 for FPS=60, sqrt(2.0) for FPS=30, etc.
        //       this is "close enough" solution and not perfect.
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is precessed separately.
     */
    private fun applyGravitation() {

        if (!isNoSubjectToGrav && !(gravitation.y > 0 && walledBottom || gravitation.y < 0 && walledTop)) {
            //if (!isWalled(hitbox, COLLIDING_BOTTOM)) {
            applyAcceleration(getDrag(externalV))
            //}
        }
    }

    /**
     * @return Collision Status, Collision damage in Newtons
     */
    private fun displaceHitbox(useControllerV: Boolean): Pair<Int, Double> {
        var collisionDamage = 0.0
        val printdbg1 = false && App.IS_DEVELOPMENT_BUILD
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
        //          deform vector "externalV"
        //          if isControllable:
        //              also alter walkX/Y
        // translate ((nextHitbox)) hitbox by moveDelta (forces), this consumes force
        //      DO NOT set whatever delta to zero
        // ((hitbox <- nextHitbox))
        //
        // ((comments))  [Label]

        if (world != null) {


            fun Double.modTile() = this.div(TILE_SIZE).floorToInt().times(TILE_SIZE)
            fun Double.modTileDelta() = this - this.modTile()



            if (useControllerV && controllerV != null && stairPenaltyCounter < stairPenaltyMax) {
                controllerV!!.x  *= stairPenaltyVector
            }



            // the job of the ccd is that the "next hitbox" would not dig into the terrain greater than the tile size,
            // in which the modTileDelta returns a wrong value
            val vectorSum = (externalV + (if (useControllerV) controllerV else Vector2()))
            val ccdSteps = (vectorSum.magnitude / TILE_SIZE).ceilToInt().plus(1).times(2).coerceIn(0, 64) // adaptive



            fun debug1(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (printdbg1 && vectorSum.magnitudeSquared != 0.0) printdbg(this, wut)
            }

            fun debug2(wut: Any?) {
                //  vvvvv  set it true to make debug print work
                if (true && printdbg1 && vectorSum.magnitudeSquared != 0.0) printdbg(this, wut)
            }

            if (printdbg1 && vectorSum.magnitudeSquared != 0.0) println("")
            debug1("Update Frame: ${INGAME.WORLD_UPDATE_TIMER}")
            debug1("Hitbox: ${hitbox}")

            debug1("vec dir: ${if (vectorSum.isZero) "." else Math.toDegrees(vectorSum.direction)} deg, value: $vectorSum, magnitude: ${vectorSum.magnitude}")


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

            val downDown = if (this is ActorHumanoid) this.downButtonHeld > 0 else false

            val sixteenStep = (0..ccdSteps).map { hitbox.clone().translate(vectorSum * (it / ccdSteps.toDouble())) }
            var collidingStep: Int? = null

            for (step in 0..ccdSteps) {

                val stepBox = sixteenStep[step]

                val goingDownwardDirection = Math.toDegrees(vectorSum.direction).let { it > 0.0 && it < 180.0 }
                val goingDown = (vectorSum.y >= PHYS_EPSILON_VELO || (vectorSum.y.absoluteValue < PHYS_EPSILON_VELO && !isWalled(stepBox, COLLIDING_BOTTOM))) // TODO reverse gravity adaptation?

                debug2("stepbox[$step]=$stepBox; goingDown=$goingDown, downDown=$downDown, goingDownwardDirection=$goingDownwardDirection")

                if (collidingStep == null && isColliding(stepBox, goingDown && !downDown && goingDownwardDirection)) {
                    collidingStep = step
                }

//                if (collidingStep != null) break
            }

            var bounceX = false
            var bounceY = false
            var zeroX = false
            var zeroY = false

            val newHitbox = if (collidingStep == null) null else hitbox.clone().reassign(sixteenStep[collidingStep])
            var staircaseStatus = 0
            var stairHeightLeft = 0.0
            var stairHeightRight = 0.0
            val selfCollisionStatus = if (newHitbox == null) 0 else {
                intArrayOf(1, 2, 4, 8).fold(0) { acc, state ->
                    val (isWalled, stairHeight) = isWalledStairs(newHitbox, state)
                    // also update staircaseStatus while we're iterating
                    if (state and COLLIDING_LR != 0) {
                        staircaseStatus = staircaseStatus or (state * (isWalled == 1).toInt())
                        if (state == COLLIDING_LEFT)
                            stairHeightLeft = stairHeight.toDouble()
                        else
                            stairHeightRight = stairHeight.toDouble()
                    }
                    acc or (state * isWalled.coerceAtMost(1))  // TODO reverse gravity adaptation?
                }
            }

            // collision NOT detected
            if (collidingStep == null) {
                debug1("== Collision step: no collision")
                hitbox.translate(vectorSum)
                // grounded = false
            }
            // collision detected
            else {
                debug1("== Collision step: $collidingStep / $ccdSteps")
                debug1("CCD hitbox: ${newHitbox}")

                val newHitbox = newHitbox!!

                // superseded by isWalledStairs-related codes
                //if (isWalled(newHitbox, COLLIDING_LEFT)) selfCollisionStatus += COLL_LEFTSIDE   // 1
                //if (isWalled(newHitbox, COLLIDING_BOTTOM)) selfCollisionStatus += COLL_BOTTOMSIDE // 2
                //if (isWalled(newHitbox, COLLIDING_RIGHT)) selfCollisionStatus += COLL_RIGHTSIDE  // 4
                //if (isWalled(newHitbox, COLLIDING_TOP)) selfCollisionStatus += COLL_TOPSIDE    // 8

                // fixme UP and RIGHT && LEFT and DOWN bug

                debug1("collision: $selfCollisionStatus\tstaircasing: $staircaseStatus")

                when (selfCollisionStatus) {
                    /*0     -> {
                        debug1("Contradiction -- collision detected by CCD, but isWalled() says otherwise")
                    }*/
                    /* 5 */ COLLIDING_LR -> {
                        zeroX = true
                    }
                    /* 10 */ COLLIDING_UD -> {
                        zeroY = true
                    }
                    /* 15  */ COLLIDING_ALLSIDE -> {
                        newHitbox.reassign(sixteenStep[0]); zeroX = true; zeroY = true
                        debug1("reassining hitbox to ${sixteenStep[0]}")
                    }
                    // one-side collision
                    /* 1, 11 */ COLLIDING_LEFT, COLLIDING_LEFT or COLLIDING_UD -> {
                        val t = TILE_SIZE - newHitbox.startX.modTileDelta()
                        newHitbox.translatePosX(t); bounceX = true
                        debug1("translate x by $t")
                    }
                    /* 4, 14 */ COLLIDING_RIGHT, COLLIDING_RIGHT or COLLIDING_UD -> {
                        val t = -newHitbox.endX.modTileDelta() - PHYS_EPSILON_DIST // THE cheapest way to resolve right-sided phys bug
                        newHitbox.translatePosX(t); bounceX = true
                        debug1("translate x by $t")
                    }
                    /* 8, 13 */ COLLIDING_TOP, COLLIDING_TOP or COLLIDING_LR -> {
                        val t = TILE_SIZE - newHitbox.startY.modTileDelta()
                        newHitbox.translatePosY(t); bounceY = true
                        debug1("translate y by $t")
                    }
                    /* 2, 7 */ COLLIDING_BOTTOM, COLLIDING_BOTTOM or COLLIDING_LR -> {
                        val t = -newHitbox.endY.modTileDelta()
                        newHitbox.translatePosY(t); bounceY = true
                        debug1("translate y by $t")
                    }
                }


                if (selfCollisionStatus == 0) {
                    debug1("== selfCollisionStatus was zero, behaving as if (collidingStep = null)")
                    hitbox.translate(vectorSum)
                }
                else {
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
                            newHitbox.endX.div(TILE_SIZE).floorToDouble() * TILE_SIZE - PHYS_EPSILON_DIST // adding/subbing fixes a bug where player stops midair when L/R is held and moving down from the platform
                        else
                            newHitbox.startX.div(TILE_SIZE).ceilToDouble() * TILE_SIZE + PHYS_EPSILON_DIST // adding/subbing fixes a bug where player stops midair when L/R is held and moving down from the platform

                        // points to the EDGE of the tile in world dimension (don't use this directly to get tilewise coord!!)
                        val offendingTileWorldY = if (selfCollisionStatus in listOf(3, 6))
                            newHitbox.endY.div(TILE_SIZE).floorToDouble() * TILE_SIZE - PHYS_EPSILON_DIST // adding/subbing fixes a bug where player stops midair when L/R is held and moving down from the platform
                        else
                            newHitbox.startY.div(TILE_SIZE).ceilToDouble() * TILE_SIZE + PHYS_EPSILON_DIST // adding/subbing fixes a bug where player stops midair when L/R is held and moving down from the platform

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

                        debug1(
                            "incidentAngle: ${Math.toDegrees(angleOfIncidence)}°, threshold: ${
                                Math.toDegrees(
                                    angleThreshold
                                )
                            }°"
                        )

                        debug1("offendingTileWorldY=$offendingTileWorldY, offendingHitboxPointY=$offendingHitboxPointY")

                        val displacementAbs = Vector2(
                            (offendingTileWorldX - offendingHitboxPointX).abs() +
                                    if (selfCollisionStatus and COLLIDING_RIGHT != 0) PHYS_EPSILON_DIST else 0.0, // THE cheapest way to resolve right-sided phys bug
                            (offendingTileWorldY - offendingHitboxPointY).abs()
                        )

                        // FIXME jump-thru-ceil bug on 1px-wide (the edge), case-9 collision (does not occur on case-12 coll.)


                        val displacementUnitVector =
                            if (angleOfIncidence == angleThreshold)
                                -vectorSum.signum
                            else {
                                when (selfCollisionStatus) {
                                    3 -> if (angleOfIncidence > angleThreshold) Vector2(1.0, 0.0)
                                    else Vector2(
                                        0.0,
                                        -1.0
                                    )

                                    6 -> if (angleOfIncidence > angleThreshold) Vector2(0.0, -1.0)
                                    else Vector2(
                                        -1.0,
                                        0.0
                                    )

                                    9 -> if (angleOfIncidence > angleThreshold) Vector2(0.0, 1.0) else Vector2(1.0, 0.0)
                                    12 -> if (angleOfIncidence > angleThreshold) Vector2(-1.0, 0.0)
                                    else Vector2(
                                        0.0,
                                        1.0
                                    )

                                    else -> throw InternalError("Blame hardware or universe")
                                }
                            }

                        val finalDisplacement =
//                            if (angleOfIncidence == angleThreshold)
//                                displacementUnitVector
//                            else
                            Vector2(
                                displacementAbs.x * displacementUnitVector.x,
                                displacementAbs.y * displacementUnitVector.y
                            )

                        debug1("displacementAbs=$displacementAbs")
                        debug1("displacementUnitVector=$displacementUnitVector")
                        debug1("finalDisplacement=$finalDisplacement")

                        // adjust finalDisplacement for honest-to-god staircasing
                        if (physProp.useStairs && vectorSum.y <= 0.0 && staircaseStatus in listOf(1, 4) &&
                            selfCollisionStatus in (if (gravitation.y >= 0.0) listOf(3, 6) else listOf(9, 12))
                        ) {
                            // remove Y displacement
                            // let original X velocity to pass-thru instead of snapping to tiles coded above
                            // pass-thru values are held by the vectorSum

                            debug1("staircasing: $staircaseStatus for $selfCollisionStatus")

                            val stairHeight =
                                if (staircaseStatus == COLLIDING_LEFT) stairHeightLeft else stairHeightRight
                            finalDisplacement.y = -stairHeight
                            finalDisplacement.x = vectorSum.x

                            bounceX = false
                            bounceY = false

                            // this will slow down the player, but its main purpose is to hide a bug
                            // that when player happens to be "walled" (which zeroes the x velo) they can keep
                            // move left/right as long as "buried depth" <= stairheight
                            // so we also zero the same exact value here for perfect hiding
                            if (useControllerV && controllerV != null) {
                                val stairRatio = stairHeight / hitbox.height
                                stairPenaltyVector =
                                    Math.pow(1.0 - (stairRatio), 90 * stairRatio).times(10).coerceIn(0.00005, 1.0)
                                controllerV!!.x = 0.0
                                stairPenaltyCounter = 0
                                stairPenaltyMax = Math.pow(stairRatio, 2.4).times(166).roundToInt().coerceAtMost(64)
                            }
                        }
                        else {
                            bounceX = angleOfIncidence == angleThreshold || displacementUnitVector.x != 0.0
                            bounceY = angleOfIncidence == angleThreshold || displacementUnitVector.y != 0.0
                        }


                        newHitbox.translate(finalDisplacement)


                        debug1("displacement: $finalDisplacement")


                        // TODO: translate other axis proportionally to the incident vector


                    }

                    // bounce X/Y
                    if (bounceX) {
                        externalV.x *= elasticity
                        if (useControllerV) controllerV?.let { controllerV!!.x *= elasticity }
                    }
                    if (bounceY) {
                        externalV.y *= elasticity
                        if (useControllerV) controllerV?.let { controllerV!!.y *= elasticity }
                    }
                    if (zeroX) {
                        externalV.x = 0.0
                        if (useControllerV) controllerV?.let { controllerV!!.x = 0.0 }
                    }
                    if (zeroY) {
                        externalV.y = 0.0
                        if (useControllerV) controllerV?.let { controllerV!!.y = 0.0 }
                    }


                    hitbox.reassign(newHitbox)
                    debug1("resulting hitbox: $newHitbox")


//                    var feetTileCount = 0
//                    var feetTileStregthSum = 0
//                    forEachFeetTile { it?.let {
//                        feetTileCount += 1
//                        feetTileStregthSum += it.strength
//                    }}
//                    val avrFeetTileStrength = feetTileStregthSum.toDouble() / feetTileCount
//                    val adjustedTileStr = avrFeetTileStrength / 1176
//                    val fallDamageDampenMult = (adjustedTileStr / (adjustedTileStr + 1)).sqr()

                    collisionDamage = Calculate.collisionDamage(this, vectorSum)

                    // kg * m / s^2 (mass * acceleration), acceleration -> (vectorMagn / (0.01)^2).gameToSI()
                    // take material softness(?) into account
                    if (collisionDamage != 0.0) debug1("Collision damage: $collisionDamage N")
                    // FIXME instead of 0.5mv^2, we can model after "change of velocity (aka accel)", just as in real-life; big change of accel on given unit time is what kills


                    // grounded = true

                    // another platform-related hacks
//                    if (this is ActorHumanoid) downButtonHeld = false

                }
            }// end of collision not detected




            return selfCollisionStatus to collisionDamage



            // if collision not detected, just don't care; it's not your job to apply moveDelta

        } // end of (world != null)
        return 0 to collisionDamage
    }

    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    private fun isColliding(hitbox: Hitbox, usePlatformDetection: Boolean = false): Boolean {
        if (isNoCollideWorld) return false

        // detectors are inside of the bounding box
        val x1 = hitbox.startX
        val y1 = hitbox.startY - if (gravitation.y < 0) HALF_PIXEL else 0.0
        val x2 = hitbox.endX - PHYS_EPSILON_DIST
        val y2 = hitbox.endY + if (gravitation.y >= 0) HALF_PIXEL else 0.0 // PLUS HALF PIXEL AND NOT MINUS EPSILON to fix issue #48 and #49

        // this commands and the commands on isWalled WILL NOT match (1 px gap on endX/Y). THIS IS INTENTIONAL!

        val txStart = x1/*.plus(HALF_PIXEL)*/.floorToInt()
        val txEnd =   x2/*.plus(HALF_PIXEL)*/.floorToInt()
        val tyStart = y1/*.plus(HALF_PIXEL)*/.floorToInt()
        val tyEnd =   y2/*.plus(HALF_PIXEL)*/.floorToInt()

//        return isCollidingInternalStairs(txStart, if (feet) tyEnd else tyStart, txEnd, tyEnd, feet).first > 0
        return isCollidingInternalStairs(txStart, tyStart, txEnd, tyEnd, usePlatformDetection).first > 0
    }

    private fun Hitbox.getWallDetection(option: Int): List<Double> {
        val SOME_PIXEL = 1.0 // it really does NOT work if the value is not 1.0
        val x1: Double
        val x2: Double
        val y1: Double
        val y2: Double
        when (option) {
            COLLIDING_TOP -> {
                x1 = this.startX
                x2 = this.endX - PHYS_EPSILON_DIST
                y1 = this.startY - SOME_PIXEL
                y2 = y1
            }
            COLLIDING_BOTTOM -> {
                x1 = this.startX
                x2 = this.endX - PHYS_EPSILON_DIST
                y1 = this.endY - PHYS_EPSILON_DIST + SOME_PIXEL
                y2 = y1
            }
            COLLIDING_LEFT -> {
                x1 = this.startX - SOME_PIXEL
                x2 = x1
                y1 = this.startY
                y2 = this.endY - PHYS_EPSILON_DIST
            }
            COLLIDING_RIGHT -> {
                x1 = this.endX - PHYS_EPSILON_DIST + SOME_PIXEL
                x2 = x1
                y1 = this.startY
                y2 = this.endY - PHYS_EPSILON_DIST
            }
            else -> throw IllegalArgumentException("Unknown option $option")
        }
        return listOf(x1, x2, y1, y2)
    }

    private fun Int.popcnt() = Integer.bitCount(this)

    /**
     * @see /work_files/hitbox_collision_detection_compensation.jpg
     */
    fun isWalled(hitbox: Hitbox, option: Int): Boolean {
        /*
        The structure:

             #######  // TOP
            =+-----+=
            =|     |=
            =+-----+=
             #######  // BOTTOM

        IMPORTANT AF NOTE: things are ASYMMETRIC!
         */

        val canUseStairs = option and COLLIDING_LR != 0 && (externalV + controllerV).y.absoluteValue < 1.0

        if (option.popcnt() == 1) {
            val (x1, x2, y1, y2) = hitbox.getWallDetection(option)

            val txStart = x1/*.plus(HALF_PIXEL)*/.floorToInt()
            val txEnd =   x2/*.plus(HALF_PIXEL)*/.floorToInt()
            val tyStart = y1/*.plus(HALF_PIXEL)*/.floorToInt()
            val tyEnd =   y2/*.plus(HALF_PIXEL)*/.floorToInt()

            return isCollidingInternalStairs(txStart, tyStart, txEnd, tyEnd, option == COLLIDING_BOTTOM).first.let { status ->
                if (canUseStairs)
                    status == 2
                else
                    status > 0
            }
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
        else throw IllegalArgumentException("$option")

    }

    private fun getWalledTiles(hitbox: Hitbox, option: Int): List<Point2i> {
        /*
        The structure:

             #######  // TOP
            =+-----+=
            =|     |=
            =+-----+=
             #######  // BOTTOM

        IMPORTANT AF NOTE: things are ASYMMETRIC!
         */

        if (option.popcnt() == 1) {
            val (x1, x2, y1, y2) = hitbox.getWallDetection(option)

            val txStart = x1.floorToInt().div(TILE_SIZED).floorToInt() // round down toward negative infinity
            val txEnd =   x2.floorToInt().div(TILE_SIZED).floorToInt() // round down toward negative infinity
            val tyStart = y1.floorToInt().div(TILE_SIZED).floorToInt() // round down toward negative infinity
            val tyEnd =   y2.floorToInt().div(TILE_SIZED).floorToInt() // round down toward negative infinity

            return getWalledTiles0(txStart, tyStart, txEnd, tyEnd, option == COLLIDING_BOTTOM)
        }
        else if (option == 0) {
            return emptyList()
        }
        else {
            return intArrayOf(1, 2, 4, 8).flatMap {
                getWalledTiles(hitbox, option and it)
            }
        }
    }

    private fun getWalledTiles0(pxStart: Int, pyStart: Int, pxEnd: Int, pyEnd: Int, feet: Boolean): List<Point2i> {
        val ret = ArrayList<Point2i>()
        if (world == null) return emptyList()
        val ys = pyStart..pyEnd
        val xs = pxStart..pxEnd
        val feetY = pyEnd // round down toward negative infinity // TODO reverse gravity adaptation?

        for (ty in ys) {
            val isFeetTileHeight = (ty == feetY)

            for (tx in xs) {
                val tile = world!!.getTileFromTerrain(tx, ty)

                if (feet && isFeetTileHeight) {
                    if (shouldICollideWithThisFeet(tile)) {
                        ret.add(Point2i(tx, ty))
                    }
                }
                else {
                    if (shouldICollideWithThis(tile)) {
                        ret.add(Point2i(tx, ty))
                    }
                }
            }
        }
//        printdbg(this, "ys=$ys, xs=$xs, ret=$ret")
        return ret
    }

    /**
     * @return First int: 0 - no collision, 1 - staircasing, 2 - "bonk" to the wall; Second int: stair height
     */
    private fun isWalledStairs(hitbox: Hitbox, option: Int): Pair<Int, Int> {
        /*
        The structure:

             #######  // TOP
            =+-----+=
            =|     |=
            =+-----+=
             #######  // BOTTOM

        IMPORTANT AF NOTE: things are ASYMMETRIC!
         */

        if (option.popcnt() == 1) {
            val (x1, x2, y1, y2) = hitbox.getWallDetection(option)

            val pxStart = x1/*.plus(HALF_PIXEL)*/.floorToInt()
            val pxEnd =   x2/*.plus(HALF_PIXEL)*/.floorToInt()
            val pyStart = y1/*.plus(HALF_PIXEL)*/.floorToInt()
            val pyEnd =   y2/*.plus(HALF_PIXEL)*/.floorToInt()

            return isCollidingInternalStairs(pxStart, pyStart, pxEnd, pyEnd, option == COLLIDING_BOTTOM)
        }
        else if (option == COLLIDING_ALLSIDE) {
            return max(max(isWalledStairs(hitbox, COLLIDING_LEFT).first,
                           isWalledStairs(hitbox, COLLIDING_RIGHT).first),
                    max(isWalledStairs(hitbox, COLLIDING_BOTTOM).first,
                        isWalledStairs(hitbox, COLLIDING_TOP).first)) to 0

        }
        else if (option == COLLIDING_LR) {
            val v1 = isWalledStairs(hitbox, COLLIDING_LEFT)
            val v2 = isWalledStairs(hitbox, COLLIDING_RIGHT)
            return max(v1.first, v2.first) to max(v2.first, v2.second)
        }
        else if (option == COLLIDING_UD) {
            return max(isWalledStairs(hitbox, COLLIDING_BOTTOM).first,
                       isWalledStairs(hitbox, COLLIDING_TOP).first) to 0
        }
        else throw IllegalArgumentException("$option")

    }

    private val AUTO_CLIMB_STRIDE: Int
        get() = ((actorValue.getAsInt(AVKey.VERTSTRIDE) ?: 8) * scale).toInt()
    //private val AUTO_CLIMB_RATE: Int // we'll just climb stairs instantly to make things work wo worrying about the details
    //    get() = Math.min(TILE_SIZE / 8 * Math.sqrt(scale), TILE_SIZED).toInt()

    /**
     * @return First int: 0 - no collision, 1 - staircasing, 2 - "bonk" to the wall; Second int: stair height
     */
    private fun isCollidingInternalStairs(pxStart: Int, pyStart: Int, pxEnd: Int, pyEnd: Int, feet: Boolean = false): Pair<Int, Int> {
        if (world == null) return 0 to 0

        val cornerSize = minOf((pyEnd - pyStart) / 3, (pxEnd - pyStart) / 3)

        val ys = if (gravitation.y >= 0) pyEnd downTo pyStart else pyStart..pyEnd
        val yheight = (ys.last - ys.first).absoluteValue
        var stairHeight = 0
        var hitFloor = false

//        if (ys.last != ys.first && feet) throw InternalError("Feet mode collision but pyStart != pyEnd ($pyStart .. $pyEnd)")

        val feetY = (pyEnd / TILE_SIZED).floorToInt() // round down toward negative infinity // TODO reverse gravity adaptation?

//        if (feet && this is IngamePlayer) printdbg(this, "dim=($pxStart,$pyStart)-($pxEnd,$pyEnd), feetY=$feetY")

        for (y in ys) {

            val ty = (y / TILE_SIZED).floorToInt() // round down toward negative infinity
//            if (this is IngamePlayer) printdbg(this, "ty=${ty}")
            val isFeetTileHeight = (ty == feetY)
            var hasFloor = false

            // octagonal shape
            val xs = /*if (y < cornerSize) {
                val sub = cornerSize - y
                (pxStart + sub)..(pxEnd - sub)
            }
            else if (y > pyEnd - cornerSize) {
                val sub = y - (pyEnd - cornerSize)
                (pxStart + sub)..(pxEnd - sub)
            }
            else*/
                (pxStart / TILE_SIZED).floorToInt()..(pxEnd / TILE_SIZED).floorToInt()

            for (tx in xs) {
                val tile = world!!.getTileFromTerrain(tx, ty)

                if (feet && isFeetTileHeight) {
                    if (shouldICollideWithThisFeet(tile)) {
                        hasFloor = true
                        hitFloor = true
                        //return 2
                    }
                }
                else {
                    if (shouldICollideWithThis(tile)) {
                        hasFloor = true
                        hitFloor = true
                        //return 2
                    }
                }
            }

            val distFromOriginY = if (gravitation.y >= 0) ys.first - y else y - ys.first
            //print("$distFromOriginY ")

            if (hasFloor)
                stairHeight = distFromOriginY

            if (stairHeight > AUTO_CLIMB_STRIDE) {
                //println(" -> $stairHeight ending prematurely")
                return 2 to stairHeight
            }

        }

        //println("-> $stairHeight")

               // edge-detect mode
        return if (yheight == 0) hitFloor.toInt(1) to stairHeight
               // not an edge-detect && no collision
               else if (stairHeight == 0) 0 to 0
               // there was collision and stairHeight <= AUTO_CLIMB_STRIDE
               else 1 to stairHeight // 1; your main code is not ready to handle return code 1 (try "setscale 2")
    }

    /**
     * If this tile should be treated as "collidable"
     *
     * Very straightforward for the actual solid tiles, not so much for the platforms
     */
    protected fun shouldICollideWithThis(tile: ItemID) =
            // regular solid block
            (BlockCodex[tile].isSolid)

    // the location and size of the platform in world coord
    /*protected var platformToIgnore: Hitbox? = null

    private fun standingOnIgnoredPlatform(): Boolean = platformToIgnore.let {
        return if (it != null)
            hitbox.startX >= it.startX && hitbox.endX < it.endX - PHYS_EPSILON_DIST &&
            it.startY <= hitbox.endY && hitbox.endY < it.endY - PHYS_EPSILON_DIST
        else false
    }*/

    /**
     * If this tile should be treated as "collidable"
     *
     * Just like "shouldICollideWithThis" but it's intended to work with feet tiles
     */
    protected fun shouldICollideWithThisFeet(tile: ItemID) =
            // regular solid block
            (BlockCodex[tile].isSolid) ||
            // or platforms that are not on the "ignored list" (the list is updated on the update())
            // THIS IS NOT A CAUSE OF THE "OSCILLATING PLATFORM" BUG
            //(this is ActorHumanoid && BlockCodex[tile].isPlatform && !standingOnIgnoredPlatform())
            // platforms, moving downward AND not "going down"
            (this is ActorHumanoid && BlockCodex[tile].isPlatform &&
             externalV.y + (controllerV?.y ?: 0.0) >= 0.0 &&
             this.downButtonHeld == 0 && this.axisY <= 0f) ||
            // platforms, moving downward, for the case of NOT ActorHumanoid
            (this !is ActorHumanoid && !physProp.ignorePlatform && BlockCodex[tile].isPlatform &&
             externalV.y + (controllerV?.y ?: 0.0) >= 0.0)
    // TODO: as for the platform, only apply it when it's a feet tile


    private fun getSubmergedHeight(): Double {
        if (world == null) return 0.0
        val straightGravity = (world!!.gravitation.y > 0)
        // TODO reverse gravity
        if (!straightGravity) TODO()

        val itsY = ((hitbox.startY - PHYS_EPSILON_DIST) / TILE_SIZED).toInt()
        val iteY = ((hitbox.endY - PHYS_EPSILON_DIST) / TILE_SIZED).toInt()
        val txL = (hitbox.startX / TILE_SIZED).floorToInt()
        val txR = (hitbox.endX / TILE_SIZED).floorToInt()

        var hL = 0.0
        var hR = 0.0

        val rec = ArrayList<Double>()

        for (ty in itsY..iteY) {
            val fL = world!!.getFluid(txL, ty).amount.coerceAtMost(1f) * TILE_SIZED // 0-16
            val fR = world!!.getFluid(txR, ty).amount.coerceAtMost(1f) * TILE_SIZED // 0-16

            // if head
            if (ty == itsY) {
                val actorHs = hitbox.startY % TILE_SIZED // 0-16
                val yp = TILE_SIZED - actorHs // 0-16

                hL += min(yp, fL)
                hR += min(yp, fR)

                rec.add(min(yp, fL))
            }
            // if tail
            else if (ty == iteY) {
                val actorHe = hitbox.endY % TILE_SIZED // 0-16

                hL += (actorHe - TILE_SIZED + fL).coerceAtLeast(0.0)
                hR += (actorHe - TILE_SIZED + fR).coerceAtLeast(0.0)

                rec.add((actorHe - TILE_SIZED + fL).coerceAtLeast(0.0))
            }
            else {
                hL += fL
                hR += fR

                rec.add(fL)
            }
        }

        // returns average of two sides
        return (hL + hR) / 2.0
    }

    private fun getTileFriction(tile: ItemID) =
            if (physProp.immobileBody && tile == Block.AIR)
                BlockCodex[Block.AIR].friction.frictionToMult().div(500)
                        .times(if (!grounded) elasticity else 1.0)
            else
                BlockCodex[tile].friction.frictionToMult()

    /** about stopping
     * for about get moving, see updateMovementControl */
    private fun setHorizontalFriction() {
        val friction = if (isNoClip)
            BASE_FRICTION * (actorValue.getAsDouble(AVKey.FRICTIONMULT) ?: 1.0) * BlockCodex[Block.STONE].friction.frictionToMult()
        else {
            // TODO status quo if !submerged else linearBlend(feetFriction, bodyFriction, submergedRatio)
            BASE_FRICTION * if (grounded) feetFriction else bodyFriction
        }

        if (externalV.x < 0) {
            externalV.x += friction
            if (externalV.x > 0) externalV.x = 0.0 // compensate overshoot
        }
        else if (externalV.x > 0) {
            externalV.x -= friction
            if (externalV.x < 0) externalV.x = 0.0 // compensate overshoot
        }

        if (this is Controllable) {
            if (controllerV!!.x < 0) {
                controllerV!!.x += friction
                if (controllerV!!.x > 0) controllerV!!.x = 0.0
            }
            else if (controllerV!!.x > 0) {
                controllerV!!.x -= friction
                if (controllerV!!.x < 0) controllerV!!.x = 0.0
            }
        }
    }

    private fun setVerticalFriction() {
        val friction = if (isNoClip)
            BASE_FRICTION * (actorValue.getAsDouble(AVKey.FRICTIONMULT) ?: 1.0) * BlockCodex[Block.STONE].friction.frictionToMult()
        else
            BASE_FRICTION * bodyFriction
        // TODO wall friction (wall skid) similar to setHorizintalFriction ?


        if (externalV.y < 0) {
            externalV.y += friction
            if (externalV.y > 0) externalV.y = 0.0 // compensate overshoot
        }
        else if (externalV.y > 0) {
            externalV.y -= friction
            if (externalV.y < 0) externalV.y = 0.0 // compensate overshoot
        }

        if (this is Controllable) {
            if (controllerV!!.y < 0) {
                controllerV!!.y += friction
                if (controllerV!!.y > 0) controllerV!!.y = 0.0
            }
            else if (controllerV!!.y > 0) {
                controllerV!!.y -= friction
                if (controllerV!!.y < 0) controllerV!!.y = 0.0
            }
        }
    }

    /**
     * [N] = [kg * m / s^2]
     * F(bo) = density * submerged_volume * gravitational_acceleration [N]
     *
     * @return the resultant buoyant force, F(k) + F(bo)
     */
    private fun applyBuoyancy() {
        if (world == null) return

        val submergedRatioForOutOfWaterManoeuvre = submergedRatio.coerceAtLeast(PHYS_EPSILON_SUBMERSION_RATIO).pow(0.75)

        // this term allows swimming. Model: you're opening a thruster pointing downwards
        val jumpAcc = if (this is ActorHumanoid)
            Vector2(0.0, if (submergedRatio >= PHYS_EPSILON_SUBMERSION_RATIO) (swimAcc / submergedRatioForOutOfWaterManoeuvre) else 0.0)
        else
            Vector2()

        val rho = tileDensityFluid // kg / m^3
        val V_full = mass / density * 2.0 // density = mass / volume, simply rearrange this. Multiplier of 2.0 is a hack!
        val V = V_full * submergedRatio
        val grav = world!!.gravitation // m / s^2
        val F_k = (grav + jumpAcc) * mass // F = ma where a is g; TODO add jump-accel into 'a' to allow better jumping under water
        val F_bo = grav * (rho * V) // Newtons

        // mh'' = mg - rho*gv
        // h'' = (mg - rho*gv) / m

        // if tileDensity = actorDensity, F_k = F_bo (this will be the case if there was no hack)
//        printdbg(this, "F_k=$F_k [N] \t F_bo=${F_bo} [N] \t density=$density")

        val F = F_k - F_bo

        val acc = F / mass // (kg * m / s^2) / kg = m / s^2
        val acc_game = acc.let { Vector2(it.x, it.y.coerceAtMost(0.0)) } * SI_TO_GAME_ACC

        applyAcceleration(acc_game)
    }


    @Transient private var submergedRatio: Double = 0.0
    /** unit : pixels */
    @Transient private var submergedHeight: Double = 0.0


    // body friction is always as same as the air. Fluid doesn't matter, they use viscosity
    //    (or viscosity was the wrong way and the friction DO matter? hmm... -- Torvald, 2018-12-31)
    //    (or perhaps... they work in tandem, viscocity slows things down, friction makes go and stop fast -- Torvald, 2019-01-01; not the proudest thing to do in the ney year)
    // going down the platform won't show abnormal slowing (it's because of the friction prop!)
    internal inline val bodyFriction: Double
        get() {
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
            forEachOccupyingFluid {
                // get max viscosity
                if (it != null) {
                    viscosity = max(viscosity, FluidCodex[it.type].viscosity)
                }
            }

            return viscosity
        }
    internal inline val feetViscosity: Int
        get() {
            var viscosity = 0
            forEachFeetFluid {
                // get max viscosity
                if (it != null) {
                    viscosity = max(viscosity, FluidCodex[it.type].viscosity)
                }
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
    internal inline val jumpMultByTile: Double
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
                if (it?.isFluid() == true && it.getProp().density > density) {
                    density = it.getProp().density
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
        //val actorMinimumX = AppLoader.halfScreenW // to make camera's X stay positive
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

    open fun drawGlow(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible) {
            blendNormalStraightAlpha(batch)
            if (spriteGlow != null)
                drawSpriteInGoodPosition(frameDelta, spriteGlow!!, batch, 1)
            else if (sprite != null)
                drawSpriteInGoodPosition(frameDelta, sprite!!, batch, 1, Color.BLACK) // use black version of normal sprite as a substitution
        }
    }

    open fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible) {
            blendNormalStraightAlpha(batch)
            if (spriteEmissive != null)
                drawSpriteInGoodPosition(frameDelta, spriteEmissive!!, batch, 2)
            else if (sprite != null)
                drawSpriteInGoodPosition(frameDelta, sprite!!, batch, 2, Color.BLACK) // use black version of normal sprite as a substitution
        }
    }

    open fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            BlendMode.resolve(drawMode, batch)
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch)
        }
    }

    
    internal fun drawBody1(frameDelta: Float, batch: SpriteBatch) {
        drawBody(frameDelta, batch)

        // debug display of hIntTilewiseHitbox
        if (KeyToggler.isOn(Input.Keys.F9)) {
            val blockMark = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common").get(0, 0)

            for (y in 0..intTilewiseHitbox.height.toInt() + 1) {
                batch.color = if (y == intTilewiseHitbox.height.toInt() + 1) HITBOX_COLOURS1 else HITBOX_COLOURS0
                for (x in 0..intTilewiseHitbox.width.toInt()) {
                    batch.draw(blockMark,
                        (intTilewiseHitbox.startX.toFloat() + x) * TILE_SIZEF,
                        (intTilewiseHitbox.startY.toFloat() + y) * TILE_SIZEF
                    )
                }
            }

            batch.color = Color.WHITE
        }
    }

    protected fun drawSpriteInGoodPosition(frameDelta: Float, sprite: SpriteAnimation, batch: SpriteBatch, mode: Int = 0, forcedColourFilter: Color? = null) {
        if (world == null) return

        val offsetX = 0f
        val offsetY = 0f // for some reason this value must be zero to draw the actor planted to the ground

        val posX = hitbox.startX.plus(PHYS_EPSILON_DIST).toFloat()
        val posY = hitbox.startY.plus(PHYS_EPSILON_DIST).toFloat()

        drawBodyInGoodPosition(posX, posY) { x, y ->
            sprite.render(frameDelta, batch, x + offsetX, y + offsetY, scale.toFloat(), mode, forcedColourFilter)
        }
    }

    fun drawTextureInGoodPosition(frameDelta: Float, texture: TextureRegion, batch: SpriteBatch, forcedColourFilter: Color? = null) {
        if (world == null) return

        val offsetX = 0f
        val offsetY = 0f

        val posX = hitbox.startX.plus(PHYS_EPSILON_DIST).toFloat()
        val posY = hitbox.startY.plus(PHYS_EPSILON_DIST).toFloat()

        drawBodyInGoodPosition(posX, posY) { x, y ->
            val oldCol = batch.color.cpy()
            batch.color = if (forcedColourFilter != null) forcedColourFilter else Color.WHITE
            batch.draw(texture, x + offsetX, y + offsetY)
            batch.color = oldCol
        }
    }

    fun drawUsingDrawFunInGoodPosition(frameDelta: Float, drawFun: (x: Float, y: Float) -> Unit) {
        if (world == null) return

        val offsetX = 0f
        val offsetY = 0f // for some reason this value must be zero to draw the actor planted to the ground

        val posX = hitbox.startX.plus(PHYS_EPSILON_DIST).toFloat()
        val posY = hitbox.startY.plus(PHYS_EPSILON_DIST).toFloat()

        drawBodyInGoodPosition(posX, posY) { x, y ->
            drawFun(posX + offsetX + x, posY + offsetY + y)
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
            else if (x >= (world!!.width * TILE_SIZE).toDouble() - TILE_SIZED - hitbox.width / 2) {
                (world!!.width * TILE_SIZE).toDouble() - 1.0 - TILE_SIZED - hitbox.width / 2
            }
            else {
                x
            }

    private fun clampH(y: Double): Double =
            if (world == null) y
            else if (y < TILE_SIZE + hitbox.height) {
                TILE_SIZE + hitbox.height
            }
            else if (y >= (world!!.height * TILE_SIZE).toDouble() - TILE_SIZED - hitbox.height) {
                (world!!.height * TILE_SIZE).toDouble() - 1.0 - TILE_SIZED - hitbox.height
            }
            else {
                y
            }

    private fun clampWtile(x: Int): Int =
            if (x < 0) 0 else if (x >= (world?.width ?: 0)) (world?.width ?: 0) - 1 else x

    private fun clampHtile(x: Int): Int =
            if (x < 0) 0 else if (x >= (world?.height ?: 0)) (world?.height ?: 0) - 1 else x


    var isNoClip: Boolean = false
        set(value) {
            field = value

            if (value) {
                externalV.zero()
                controllerV?.zero()
            }
        }

    private fun assertInit() {
        // errors
        if (baseHitboxW == 0 || baseHitboxH == 0)
            throw Error("Hitbox dimension was not set. (don't modify hitbox directly -- use 'setHitboxDimension()')")

        // warnings
        if (sprite == null && isVisible && this.javaClass.simpleName != "DroppedItem")
            printdbg(this, "Caution: actor ${this.javaClass.simpleName} is visible but the sprite was not set.\n" +
                           "Actor localhash: ${this.localHashStr}")
        else if (sprite != null && !isVisible)
            printdbg(this, "Caution: actor ${this.javaClass.simpleName} is invisible but the sprite was given.\n" +
                           "Actor localhash: ${this.localHashStr}")

        assertPrinted = true
    }

    internal open fun flagDespawn() {
        flagDespawn = true
        removeFromTooltipRecord()
    }

    open fun getSpriteHead(): TextureRegion? {
        return CommonResourcePool.getAsTextureRegion("placeholder_16")
        //return sprite?.textureRegion?.get(0,0)
    }


    fun forEachOccupyingTileNum(consumer: (ItemID?) -> Unit) {
        if (world == null) return


        val tiles = ArrayList<ItemID?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tiles.add(world!!.getTileFromTerrain(x, y))
            }
        }

        return tiles.forEach(consumer)
    }

    fun forEachOccupyingTile(consumer: (BlockProp?) -> Unit) {
        if (world == null) return


        val tileProps = ArrayList<BlockProp?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tileProps.add(BlockCodex[world!!.getTileFromTerrain(x, y)])
            }
        }

        return tileProps.forEach(consumer)
    }

    fun forEachOccupyingFluid(consumer: (GameWorld.FluidInfo?) -> Unit) {
        if (world == null) return


        val tileProps = ArrayList<GameWorld.FluidInfo?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tileProps.add(world!!.getFluid(x, y))
            }
        }

        return tileProps.forEach(consumer)
    }

    fun forEachOccupyingTilePos(hitbox: Hitbox, consumer: (BlockAddress) -> Unit) {
        if (world == null) return


        val newTilewiseHitbox = Hitbox.fromTwoPoints(
                hitbox.startX.div(TILE_SIZE).floorToDouble(),
                hitbox.startY.div(TILE_SIZE).floorToDouble(),
                hitbox.endX.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
                hitbox.endY.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
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

    fun forEachOccupyingTilePosXY(hitbox: Hitbox, consumer: (Pair<Int, Int>) -> Unit) {
        if (world == null) return


        val newTilewiseHitbox = Hitbox.fromTwoPoints(
            hitbox.startX.div(TILE_SIZE).floorToDouble(),
            hitbox.startY.div(TILE_SIZE).floorToDouble(),
            hitbox.endX.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
            hitbox.endY.minus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble(),
            true
        ) // NOT the same as intTilewiseHitbox !!

        val tilePosList = ArrayList<Pair<Int, Int>>()
        for (y in newTilewiseHitbox.startY.toInt()..newTilewiseHitbox.endY.toInt()) {
            for (x in newTilewiseHitbox.startX.toInt()..newTilewiseHitbox.endX.toInt()) {
                tilePosList.add(x to y)
            }
        }

        return tilePosList.forEach(consumer)
    }

    fun forEachFeetTileNum(consumer: (ItemID?) -> Unit) {
        if (world == null) return


        val tiles = ArrayList<ItemID?>()

        // offset 1 pixel to the down so that friction would work
        val y = intTilewiseHitbox.height.toInt() + 1

        for (x in 0..intTilewiseHitbox.width.toInt()) {
            tiles.add(world!!.getTileFromTerrain(x + intTilewiseHitbox.startX.toInt(), y + intTilewiseHitbox.startY.toInt()))
        }

        return tiles.forEach(consumer)
    }

    fun forEachFeetTile(consumer: (BlockProp?) -> Unit) {
        if (world == null) return


        val tileProps = ArrayList<BlockProp?>()

        // offset 1 pixel to the down so that friction would work
//        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorToInt()
        val y = intTilewiseHitbox.startY.toInt() + intTilewiseHitbox.height.toInt() + 1

        for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
            tileProps.add(BlockCodex[world!!.getTileFromTerrain(x, y)])
        }

        return tileProps.forEach(consumer)
    }

    fun forEachFeetFluid(consumer: (GameWorld.FluidInfo?) -> Unit) {
        if (world == null) return


        val tileProps = ArrayList<GameWorld.FluidInfo?>()

        // offset 1 pixel to the down so that friction would work
//        val y = hitbox.endY.plus(1.0).div(TILE_SIZE).floorToInt()
        val y = intTilewiseHitbox.startY.toInt() + intTilewiseHitbox.height.toInt() + 1

        for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
            tileProps.add(world!!.getFluid(x, y))
        }

        return tileProps.forEach(consumer)
    }

    fun forEachFeetTileWithPos(consumer: (Point2i, ItemID) -> Unit) {
        val y = intTilewiseHitbox.startY.toInt() + intTilewiseHitbox.height.toInt() + 1
        (hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()).map { x ->
            val point = Point2i(x, y)
            val item = world!!.getTileFromTerrain(x, y)

            consumer(point, item)
        }
    }

    fun getFeetTiles(): List<Pair<Point2i, ItemID>> {
        val y = intTilewiseHitbox.startY.toInt() + intTilewiseHitbox.height.toInt() + 1
        return (hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()).map { x ->
            Point2i(x, y) to world!!.getTileFromTerrain(x, y)
        }
    }

    private fun makeDust(collisionDamage: Double, vecSum: Vector2) {
        val particleCount = (collisionDamage / 24.0).pow(0.75)
        val trueParticleCount = particleCount.ditherToInt()

        val feetTiles = getFeetTiles()

        if (collisionDamage > 1.0 / 1024.0) {
//            printdbg(this, "Collision damage: $collisionDamage N, count: $particleCount, velocity: $vecSum, mass: ${this.mass}")
//            printdbg(this, "feetTileCount = ${feetTiles.size}")
            val feetTileIndices = (feetTiles.indices).toList().toIntArray()

            for (i in 0 until trueParticleCount) {
                if (i % feetTiles.size == 0) feetTileIndices.shuffle()

                feetTiles[feetTileIndices[i % feetTiles.size]].second.let { tile ->
                    val px = hitbox.startX + Math.random() * hitbox.width
                    val py = hitbox.endY
                    makeDust0(tile, px, py, particleCount, collisionDamage, vecSum)
                }
            }

        }
    }

    private fun makeNoise(collisionDamage: Double) {
        val DIVIDER = 108.0
        if (collisionDamage / DIVIDER >= 0.05) { // only make noise when the expected volume is at least -26dBfs
            val feetTiles = getFeetTiles()
            val volumeMax = collisionDamage / DIVIDER
            val feetTileMats = feetTiles.slice(feetTiles.indices).map { BlockCodex[it.second].material }
            val feetTileCnt = feetTileMats.size.toDouble()
            val materialStats = feetTileMats.distinct().map { mat -> mat to feetTileMats.count { it == mat } }

            materialStats.forEach { (mat, cnt) ->
                Terrarum.audioCodex.getRandomFootstep(mat)?.let {
                    val vol = volumeMax * (cnt / feetTileCnt)
                    startAudio(it, vol)
//                    printdbg(this, "Playing footstep $mat (vol: $vol, file: ${it.file.name}, cd: $collisionDamage)")
                }
            }
        }
    }

    private val pixelOffs = intArrayOf(2, 7, 12) // hard-coded assuming TILE_SIZE=16

    /**
     * @param wx World-X position
     */
    private fun makeDust0(tile: ItemID, wx: Double, wy: Double, count: Double, fallDamage: Double, vecSum: Vector2) {
        val pw = 3
        val ph = 3

        val renderTag = App.tileMaker.getRenderTag(tile)
        val baseTilenum = renderTag.tileNumber
        val representativeTilenum = when (renderTag.maskType) {
            CreateTileAtlas.RenderTag.MASK_47 -> 17
            CreateTileAtlas.RenderTag.MASK_PLATFORM -> 7
            else -> 0
        }
        val tileNum = baseTilenum + representativeTilenum // the particle won't match the visible tile anyway because of the seasons stuff

        val vi = if (vecSum.y > PHYS_EPSILON_VELO) 0 else if (vecSum.y < -PHYS_EPSILON_VELO) 2 else (Math.random() * 3).toInt()
        val u = (wx.toInt() % TILE_SIZE).coerceIn(0..TILE_SIZE - pw)
        val v = pixelOffs[vi]
        val pos = Vector2(
            wx,
            wy,
        )
        val veloXvar = (Math.random() + Math.random()) * (if (Math.random() < 0.5) -1 else 1) * 0.5 // avr at 0.5
        val veloYvar = brownianRand()
        val veloMult = Vector2(
            vecSum.x * 0.8 + veloXvar,
            (count.pow(0.5) + veloYvar) * vecSum.y.sign
        )
        createRandomBlockParticle(tileNum, pos, veloMult, u, v, pw, ph).let {
            it.despawnUponCollision = true
            it.drawColour.set(Color.WHITE)
            (Terrarum.ingame as TerrarumIngame).addParticle(it)
        }
    }

    /**
     * @return random number between 0-1, of which 0 is the most likely and 1 is the least
     */
    private fun brownianRand(): Double {
        return Math.abs(Math.random() + Math.random() - 1)
    }



    companion object {

        /**
         * Constants
         */

        @Transient const val METER = 24.0
        /**
         * [m / s^2] * SI_TO_GAME_ACC -> [px / InternalFrame^2]
         */
        @Transient const val SI_TO_GAME_ACC = METER / (Terrarum.PHYS_TIME_FRAME * Terrarum.PHYS_TIME_FRAME)
        /**
         * [m / s] * SI_TO_GAME_VEL0 -> [px / InternalFrame]
         */
        @Transient const val SI_TO_GAME_VELO = METER / Terrarum.PHYS_TIME_FRAME

        /**
         * [px / InternalFrame^2] * GAME_TO_SI_ACC -> [m / s^2]
         */
        @Transient const val GAME_TO_SI_ACC = (Terrarum.PHYS_TIME_FRAME * Terrarum.PHYS_TIME_FRAME) / METER

        /**
         * [px / InternalFrame] * GAME_TO_SI_VELO -> [m / s]
         */
        @Transient const val GAME_TO_SI_VELO = Terrarum.PHYS_TIME_FRAME / METER

        @Transient const val PHYS_EPSILON_DIST = 1.0 / 4096.0
        @Transient const val PHYS_EPSILON_VELO = 1.0 / 65536.0
        @Transient const val PHYS_EPSILON_SUBMERSION_RATIO = 1.0 / 8.0


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

        private fun div16TruncateToMapWidth(x: Int): Int {
            if (x < 0)
                return 0
            else if (x >= INGAME.world.width shl 4)
                return INGAME.world.width - 1
            else
                return x and 0x7FFFFFFF shr 4
        }

        private fun div16TruncateToMapHeight(y: Int): Int {
            if (y < 0)
                return 0
            else if (y >= INGAME.world.height shl 4)
                return INGAME.world.height - 1
            else
                return y and 0x7FFFFFFF shr 4
        }

        private fun clampCeil(x: Double, ceil: Double): Double {
            return if (Math.abs(x) > ceil) ceil else x
        }

        @Transient internal val HITBOX_COLOURS0 = Color(0xFF00FF88.toInt())
        @Transient internal val HITBOX_COLOURS1 = Color(0xFFFF0088.toInt())


        fun isCloseEnough(a: Double, b: Double) = ((a / b).ifNaN(0.0) - 1).absoluteValue < PHYS_EPSILON_DIST
    }


    private fun Double.toPositiveRad() = // rad(0..pi, -pi..0) -> rad(0..2pi)
            if (-Math.PI <= this && this < 0.0)
                this + 2 * Math.PI
            else
                this

    override fun dispose() {
        App.disposables.add(sprite)
        App.disposables.add(spriteGlow)
        App.disposables.add(spriteEmissive)
        removeFromTooltipRecord()
    }
}

inline fun drawBodyInGoodPosition(startX: Float, startY: Float, drawFun: (x: Float, y: Float) -> Unit) {
    val offendingPad = INGAME.world.width.times(TerrarumAppConfiguration.TILE_SIZE) - WorldCamera.width - 1
    val offendingPad2 = WorldCamera.width + 1

    if (WorldCamera.x >= offendingPad && startX < WorldCamera.width) {
//        App.batch.color = Color.RED
        drawFun(startX + INGAME.world.width * TILE_SIZEF, startY)
    }
    else if (WorldCamera.x <= offendingPad2 && startX > offendingPad) {
//        App.batch.color = Color.BLUE
        drawFun(startX - INGAME.world.width * TILE_SIZEF, startY)
    }
    else {
//        App.batch.color = Color.WHITE
        drawFun(startX , startY)
    }
}
