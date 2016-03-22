package com.Torvald.Terrarum.Actors

import com.Torvald.Rand.HQRNG
import com.Torvald.Terrarum.*
import com.Torvald.Terrarum.GameMap.GameMap
import com.Torvald.Terrarum.MapDrawer.MapDrawer
import com.Torvald.Terrarum.TileProperties.TilePropCodex
import com.Torvald.spriteAnimation.SpriteAnimation
import com.jme3.math.FastMath
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-14.
 */
open class ActorWithBody constructor() : Actor, Visible, Glowing {

    internal var actorValue: ActorValue

    var hitboxTranslateX: Float = 0.toFloat()// relative to spritePosX
    var hitboxTranslateY: Float = 0.toFloat()// relative to spritePosY
    var baseHitboxW: Int = 0
    var baseHitboxH: Int = 0

    /**
     * Velocity for newtonian sim.
     * Fluctuation in, otherwise still, velocity is equal to acceleration.

     * Acceleration: used in code like:
     * veloY += 3.0
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

    override var referenceID: Long = HQRNG().nextLong()
    /**
     * Positions: top-left point
     */
    @Volatile var hitbox: Hitbox? = null
    @Volatile @Transient var nextHitbox: Hitbox? = null

    /**
     * Physical properties
     */
    @Volatile @Transient var scale = 1f
    @Volatile @Transient var mass = 2f
    @Transient private val MASS_LOWEST = 2f
    /** Valid range: [0, 1]  */
    var elasticity = 0f
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("[ActorWithBody] $value: valid elasticity value is [0, 1].")
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
     * Gravitational Constant G. Load from GameMap.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    @Transient private val METER = 24f
    /**
     * [m / s^2] * SI_TO_GAME_ACC -> [px / IFrame^2]
     */
    @Transient private val SI_TO_GAME_ACC = METER / FastMath.sqr(Terrarum.TARGET_FPS.toFloat())
    /**
     * [m / s] * SI_TO_GAME_VEL -> [px / IFrame]
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
    @Transient private val TILE_AUTOCLIMB_RATE = 4

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
     * in milliseconds
     */
    @Transient val INVINCIBILITY_TIME = 500

    /**
     * Will ignore fluid resistance if (submerged height / actor height) <= this var
     */
    @Transient private val FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO = 0.2f
    @Transient private val FLUID_RESISTANCE_APPLY_FULL_RATIO = 0.5f

    @Transient private val map: GameMap

    init {
        // referenceID = HQRNG().nextLong() // renew ID just in case
        actorValue = ActorValue()
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
        hitbox = Hitbox(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale, y - (baseHitboxH - hitboxTranslateY) * scale, baseHitboxW * scale, baseHitboxH * scale)

        nextHitbox = Hitbox(
                x - (baseHitboxW / 2 - hitboxTranslateX) * scale, y - (baseHitboxH - hitboxTranslateY) * scale, baseHitboxW * scale, baseHitboxH * scale)
    }

    override fun update(gc: GameContainer, delta_t: Int) {
        if (isUpdate) {
            /**
             * Update variables
             */
            if (this is Player) {
                isNoSubjectToGrav = isPlayerNoClip
                isNoCollideWorld = isPlayerNoClip
                isNoSubjectToFluidResistance = isPlayerNoClip
            }

            if (mass < MASS_LOWEST) mass = MASS_LOWEST // clamp to minimum possible mass
            if (sprite != null) {
                baseSpriteHeight = sprite!!.height
                baseSpriteWidth = sprite!!.width
            }
            gravitation = map.gravitation
            AUTO_CLIMB_RATE = Math.min(TSIZE / 8 * FastMath.sqrt(scale), TSIZE.toFloat()).toInt()

            if (!isNoSubjectToGrav) {
                applyGravitation()
                applyBuoyancy()
            }

            // hard limit velocity
            if (veloX > VELO_HARD_LIMIT) veloX = VELO_HARD_LIMIT
            if (veloY > VELO_HARD_LIMIT) veloY = VELO_HARD_LIMIT
            // limit velocity by fluid resistance
            //int tilePropResistance = getTileMvmtRstc();
            //if (!noSubjectToFluidResistance) {
            //    veloX *= mvmtRstcToMultiplier(tilePropResistance);
            //    veloY *= mvmtRstcToMultiplier(tilePropResistance);
            //}


            // Set 'next' positions to fiddle with
            updateNextHitboxFromVelo()


            // if not horizontally moving then ...
            //if (Math.abs(veloX) < 0.5) { // fix for special situations (see fig. 1 at the bottom of the source)
            //    updateVerticalPos();
            //    updateHorizontalPos();
            //}
            //else {
            updateHorizontalPos()
            updateVerticalPos()
            //}


            updateHitboxX()
            updateHitboxY()


            clampNextHitbox()
            clampHitbox()
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

    private fun updateVerticalPos() {
        if (!isNoCollideWorld) {
            // check downward
            if (veloY >= 0) {
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_BOTTOM)) {
                    adjustHitBottom()
                    elasticReflectY()
                    grounded = true
                } else if (isColliding(CONTACT_AREA_BOTTOM, 0, 1)) {
                    elasticReflectY()
                    grounded = true
                } else {
                    grounded = false
                }
            } else if (veloY < 0) {
                grounded = false

                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_TOP)) {
                    adjustHitTop()
                    elasticReflectY()
                } else if (isColliding(CONTACT_AREA_TOP, 0, -1)) {
                    elasticReflectY() // for reversed gravity
                } else {
                }
            }
        }
    }

    private fun adjustHitBottom() {
        val newX = nextHitbox!!.pointedX // look carefully, getPos or getPointed
        // int-ify posY of nextHitbox
        nextHitbox!!.setPositionYFromPoint(FastMath.floor(nextHitbox!!.pointedY).toFloat())

        var newYOff = 0 // always positive

        var colliding: Boolean
        do {
            newYOff += 1
            colliding = isColliding(CONTACT_AREA_BOTTOM, 0, -newYOff)
        } while (colliding)

        val newY = nextHitbox!!.pointedY - newYOff
        nextHitbox!!.setPositionFromPoint(newX, newY)
    }

    private fun adjustHitTop() {
        val newX = nextHitbox!!.posX
        // int-ify posY of nextHitbox
        nextHitbox!!.setPositionY(FastMath.ceil(nextHitbox!!.posY).toFloat())

        var newYOff = 0 // always positive

        var colliding: Boolean
        do {
            newYOff += 1
            colliding = isColliding(CONTACT_AREA_TOP, 0, newYOff)
        } while (colliding)

        val newY = nextHitbox!!.posY + newYOff
        nextHitbox!!.setPosition(newX, newY)
    }

    private fun updateHorizontalPos() {
        if (!isNoCollideWorld) {
            // check right
            if (veloX >= 0.5) {
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_RIGHT) && !isColliding(CONTACT_AREA_LEFT)) {
                    adjustHitRight()
                    elasticReflectX()
                } else if (isColliding(CONTACT_AREA_RIGHT, 1, 0) && !isColliding(CONTACT_AREA_LEFT, -1, 0)) {
                    elasticReflectX()
                } else {
                }
            } else if (veloX <= -0.5) {
                // System.out.println("collidingleft");
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_LEFT) && !isColliding(CONTACT_AREA_RIGHT)) {
                    adjustHitLeft()
                    elasticReflectX()
                } else if (isColliding(CONTACT_AREA_LEFT, -1, 0) && !isColliding(CONTACT_AREA_RIGHT, 1, 0)) {
                    elasticReflectX()
                } else {
                }
            } else {
                // System.out.println("updatehorizontal - |velo| < 0.5");
                if (isColliding(CONTACT_AREA_LEFT) || isColliding(CONTACT_AREA_RIGHT)) {
                    elasticReflectX()
                }
            }
        }
    }

    private fun adjustHitRight() {
        val newY = nextHitbox!!.posY // look carefully, getPos or getPointed
        // int-ify posY of nextHitbox
        nextHitbox!!.setPositionX(FastMath.floor(nextHitbox!!.posX + nextHitbox!!.width) - nextHitbox!!.width)

        var newXOff = 0 // always positive

        var colliding: Boolean
        do {
            newXOff += 1
            colliding = isColliding(CONTACT_AREA_BOTTOM, -newXOff, 0)
        } while (newXOff < TSIZE && colliding)

        val newX = nextHitbox!!.posX - newXOff
        nextHitbox!!.setPosition(newX, newY)
    }

    private fun adjustHitLeft() {
        val newY = nextHitbox!!.posY
        // int-ify posY of nextHitbox
        nextHitbox!!.setPositionX(FastMath.ceil(nextHitbox!!.posX).toFloat())

        var newXOff = 0 // always positive

        var colliding: Boolean
        do {
            newXOff += 1
            colliding = isColliding(CONTACT_AREA_TOP, newXOff, 0)
        } while (newXOff < TSIZE && colliding)

        val newX = nextHitbox!!.posX + newXOff
        nextHitbox!!.setPosition(newX, newY) // + 1; float-point rounding compensation (i think...)
    }

    private fun elasticReflectX() {
        if (veloX != 0f) veloX = -veloX * elasticity
    }

    private fun elasticReflectY() {
        if (veloY != 0f) veloY = -veloY * elasticity
    }

    private fun isColliding(side: Int, tx: Int = 0, ty: Int = 0): Boolean {
        return getContactingArea(side, tx, ty) > 1
    }

    private fun getContactingArea(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..Math.round(if (side % 2 == 0) nextHitbox!!.width else nextHitbox!!.height) - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x)
                        + i + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxEnd.y) + translateY)
            } else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x)
                        + i + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y) + translateY)
            } else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxEnd.x) + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y)
                        + i + translateY)
            } else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x) + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y)
                        + i + translateY)
            } else {
                throw IllegalArgumentException(side.toString() + ": Wrong side input")
            }

            // evaluate
            if (TilePropCodex.getProp(map.getTileFromTerrain(tileX, tileY)).isSolid) {
                contactAreaCounter += 1
            }
        }

        return contactAreaCounter
    }

    private fun getContactingAreaFluid(side: Int, translateX: Int = 0, translateY: Int = 0): Int {
        var contactAreaCounter = 0
        for (i in 0..Math.round(if (side % 2 == 0) nextHitbox!!.width else nextHitbox!!.height) - 1) {
            // set tile positions
            val tileX: Int
            val tileY: Int
            if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x)
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxEnd.y) + translateY)
            } else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x)
                                                + i + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y) + translateY)
            } else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxEnd.x) + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y)
                                                 + i + translateY)
            } else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox!!.hitboxStart.x) + translateX)
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox!!.hitboxStart.y)
                                                 + i + translateY)
            } else {
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
    private fun applyBuoyancy() {
        val fluidDensity = tileDensity
        val submergedVolume = submergedVolume

        if (!isPlayerNoClip && !grounded) {
            // System.out.println("density: "+density);
            veloY -= ((fluidDensity - this.density).toDouble()
                    * map.gravitation.toDouble() * submergedVolume.toDouble()
                    * Math.pow(mass.toDouble(), -1.0)
                    * SI_TO_GAME_ACC.toDouble()).toFloat()
        }
    }

    private val submergedVolume: Float
        get() = submergedHeight * hitbox!!.width * hitbox!!.width

    private val submergedHeight: Float
        get() = Math.max(
                getContactingAreaFluid(CONTACT_AREA_LEFT),
                getContactingAreaFluid(CONTACT_AREA_RIGHT)
        ).toFloat()


    /**
     * Get highest friction value from feet tiles.
     * @return
     */
    private val tileFriction: Int
        get() {
            var friction = 0

            //get highest fluid density
            val tilePosXStart = Math.round(nextHitbox!!.posX / TSIZE)
            val tilePosXEnd = Math.round(nextHitbox!!.hitboxEnd.x / TSIZE)
            val tilePosY = Math.round(nextHitbox!!.pointedY / TSIZE)
            for (x in tilePosXStart..tilePosXEnd) {
                val tile = map.getTileFromTerrain(x, tilePosY)
                if (TilePropCodex.getProp(tile).isFluid) {
                    val thisFluidDensity = TilePropCodex.getProp(tile).friction

                    if (thisFluidDensity > friction) friction = thisFluidDensity
                }
            }

            return friction
        }

    /**
     * Get highest movement resistance value from tiles that the body occupies.
     * @return
     */
    private val tileMvmtRstc: Int
        get() {
            var resistance = 0

            //get highest fluid density
            val tilePosXStart = Math.round(nextHitbox!!.posX / TSIZE)
            val tilePosYStart = Math.round(nextHitbox!!.posY / TSIZE)
            val tilePosXEnd = Math.round(nextHitbox!!.hitboxEnd.x / TSIZE)
            val tilePosYEnd = Math.round(nextHitbox!!.hitboxEnd.y / TSIZE)
            for (y in tilePosYStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)
                    if (TilePropCodex.getProp(tile).isFluid) {
                        val thisFluidDensity = TilePropCodex.getProp(tile).movementResistance

                        if (thisFluidDensity > resistance) resistance = thisFluidDensity
                    }
                }
            }

            return resistance
        }

    /**
     * Get highest density (specific gravity) value from tiles that the body occupies.
     * @return
     */
    private val tileDensity: Int
        get() {
            var density = 0

            //get highest fluid density
            val tilePosXStart = Math.round(nextHitbox!!.posX / TSIZE)
            val tilePosYStart = Math.round(nextHitbox!!.posY / TSIZE)
            val tilePosXEnd = Math.round(nextHitbox!!.hitboxEnd.x / TSIZE)
            val tilePosYEnd = Math.round(nextHitbox!!.hitboxEnd.y / TSIZE)
            for (y in tilePosYStart..tilePosYEnd) {
                for (x in tilePosXStart..tilePosXEnd) {
                    val tile = map.getTileFromTerrain(x, y)
                    if (TilePropCodex.getProp(tile).isFluid) {
                        val thisFluidDensity = TilePropCodex.getProp(tile).density

                        if (thisFluidDensity > density) density = thisFluidDensity
                    }
                }
            }

            return density
        }

    private fun mvmtRstcToMultiplier(movementResistanceValue: Int): Float {
        return 1f / (1 + movementResistanceValue / 16f)
    }

    private fun clampHitbox() {
        hitbox!!.setPositionFromPoint(
                clampW(hitbox!!.pointedX), clampH(hitbox!!.pointedY))
    }

    private fun clampNextHitbox() {
        nextHitbox!!.setPositionFromPoint(
                clampW(nextHitbox!!.pointedX), clampH(nextHitbox!!.pointedY))
    }

    private fun updateNextHitboxFromVelo() {
        val fluidResistance = mvmtRstcToMultiplier(tileMvmtRstc)
        val submergedRatio = FastMath.clamp(
                submergedHeight / nextHitbox!!.height,
                0f, 1f
        )
        val applyResistance: Boolean = !isNoSubjectToFluidResistance
                                       && submergedRatio > FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO
        val resistance: Float = FastMath.interpolateLinear(
                submergedRatio,
                1f, fluidResistance
        )

        nextHitbox!!.set(
                  Math.round(hitbox!!.posX + veloX * (if (!applyResistance) 1f else resistance)).toFloat()
                , Math.round(hitbox!!.posY + veloY * (if (!applyResistance) 1f else resistance)).toFloat()
                , Math.round(baseHitboxW * scale).toFloat()
                , Math.round(baseHitboxH * scale).toFloat())
        /** Full quantisation; wonder what havoc these statements would wreak...
         */
    }

    private fun updateHitboxX() {
        hitbox!!.setDimension(
                nextHitbox!!.width, nextHitbox!!.height)
        hitbox!!.setPositionX(nextHitbox!!.posX)
    }

    private fun updateHitboxY() {
        hitbox!!.setDimension(
                nextHitbox!!.width, nextHitbox!!.height)
        hitbox!!.setPositionY(nextHitbox!!.posY)
    }

    override fun drawGlow(gc: GameContainer, g: Graphics) {
        if (isVisible && spriteGlow != null) {
            if (!sprite!!.flippedHorizontal()) {
                spriteGlow!!.render(g, hitbox!!.posX - hitboxTranslateX * scale, hitbox!!.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            } else {
                spriteGlow!!.render(g, hitbox!!.posX - scale, hitbox!!.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            }
        }
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        if (isVisible && sprite != null) {
            if (!sprite!!.flippedHorizontal()) {
                sprite!!.render(g, hitbox!!.posX - hitboxTranslateX * scale, hitbox!!.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            } else {
                sprite!!.render(g, hitbox!!.posX - scale, hitbox!!.posY + hitboxTranslateY * scale - (baseSpriteHeight - baseHitboxH) * scale + 2, scale)
            }
        }
    }

    override fun updateGlowSprite(gc: GameContainer, delta: Int) {
        if (spriteGlow != null) spriteGlow!!.update(delta)
    }

    override fun updateBodySprite(gc: GameContainer, delta: Int) {
        if (sprite != null) sprite!!.update(delta)
    }

    private fun clampW(x: Float): Float {
        if (x < TSIZE + nextHitbox!!.width / 2) {
            return TSIZE + nextHitbox!!.width / 2
        } else if (x >= (map.width * TSIZE).toFloat() - TSIZE.toFloat() - nextHitbox!!.width / 2) {
            return (map.width * TSIZE).toFloat() - 1f - TSIZE.toFloat() - nextHitbox!!.width / 2
        } else {
            return x
        }
    }

    private fun clampH(y: Float): Float {
        if (y < TSIZE + nextHitbox!!.height) {
            return TSIZE + nextHitbox!!.height
        } else if (y >= (map.height * TSIZE).toFloat() - TSIZE.toFloat() - nextHitbox!!.height) {
            return (map.height * TSIZE).toFloat() - 1f - TSIZE.toFloat() - nextHitbox!!.height
        } else {
            return y
        }
    }

    private fun clampWtile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x >= map.width) {
            return map.width - 1
        } else {
            return x
        }
    }

    private fun clampHtile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x >= map.height) {
            return map.height - 1
        } else {
            return x
        }
    }

    private val isPlayerNoClip: Boolean
        get() = this is Player && this.isNoClip()

    private fun quantiseTSize(v: Float): Int {
        return FastMath.floor(v / TSIZE) * TSIZE
    }

    fun setDensity(density: Int) {
        if (density < 0)
            throw IllegalArgumentException("[ActorWithBody] $density: density cannot be negative.")

        this.density = density.toFloat()
    }

    companion object {

        @Transient private val TSIZE = MapDrawer.TILE_SIZE
        private var AUTO_CLIMB_RATE = TSIZE / 8

        private fun div16(x: Int): Int {
            if (x < 0) {
                throw IllegalArgumentException("div16: Positive integer only: " + x.toString())
            }
            return x and 0x7FFFFFFF shr 4
        }

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

        private fun mod16(x: Int): Int {
            if (x < 0) {
                throw IllegalArgumentException("mod16: Positive integer only: " + x.toString())
            }
            return x and 15
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