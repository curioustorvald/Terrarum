package com.Torvald.Terrarum.Actors;

import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.*;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.TileProperties.TilePropCodex;
import com.Torvald.spriteAnimation.SpriteAnimation;
import com.jme3.math.FastMath;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

import java.io.Serializable;

/**
 * Created by minjaesong on 16-01-13.
 */
public class ActorWithBody implements Actor, Visible, Glowing {

    ActorValue actorValue;

    ActorInventory inventory;

    private @NotNull float hitboxTranslateX; // relative to spritePosX
    private @NotNull float hitboxTranslateY; // relative to spritePosY
    private @NotNull int baseHitboxW;
    private @NotNull int baseHitboxH;

    /**
     * Velocity for newtonian sim.
     * Fluctuation in, otherwise still, velocity is equal to acceleration.
     *
     * Acceleration: used in code like:
     *     veloY += 3.0
     * +3.0 is acceleration. You __accumulate__ acceleration to the velocity.
     */
    private volatile @NotNull float veloX, veloY;
    private final transient float VELO_HARD_LIMIT = 10000;

    boolean grounded = false;

    @Nullable transient SpriteAnimation sprite;
    @Nullable transient SpriteAnimation spriteGlow;
    /** Default to 'false' */
    private boolean visible = false;
    /** Default to 'true' */
    private boolean update = true;

    private boolean noSubjectToGrav = false;
    private boolean noCollideWorld = false;
    private boolean noSubjectToFluidResistance = false;

    int baseSpriteWidth, baseSpriteHeight;

    /**
     * Positions: top-left point
     */
    private volatile @NotNull Hitbox hitbox, nextHitbox;

    /**
     * Physical properties
     */
    @NonZero private volatile transient float scale = 1;
    @NonZero private volatile transient float mass = 2f;
    private final transient float MASS_LOWEST = 2f;
    /** Valid range: [0, 1] */
    private float elasticity = 0;
    private final transient float ELASTICITY_MAX = 0.993f;
    @NoNegative private float density = 1000;

    private static final transient int TSIZE = MapDrawer.TILE_SIZE;
    private static int AUTO_CLIMB_RATE = TSIZE / 8;

    /**
     * Gravitational Constant G. Load from GameMap.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    private final transient float METER = 24f;
    /**
     * [m / s^2] * SI_TO_GAME_ACC -> [px / IFrame^2]
     */
    private final transient float SI_TO_GAME_ACC = METER / FastMath.sqr(Terrarum.TARGET_FPS);
    /**
     * [m / s] * SI_TO_GAME_VEL -> [px / IFrame]
     */
    private final transient float SI_TO_GAME_VEL = METER / Terrarum.TARGET_FPS;

    private float gravitation;
    private final transient float DRAG_COEFF = 1f;

    private final transient int CONTACT_AREA_TOP = 0;
    private final transient int CONTACT_AREA_RIGHT = 1;
    private final transient int CONTACT_AREA_BOTTOM = 2;
    private final transient int CONTACT_AREA_LEFT = 3;

    private final transient int UD_COMPENSATOR_MAX = TSIZE;
    private final transient int LR_COMPENSATOR_MAX = TSIZE;
    private final transient int TILE_AUTOCLIMB_RATE = 4;

    /**
     * A constant to make falling faster so that the game is more playable
     */
    private final transient float G_MUL_PLAYABLE_CONST = 1.4142f;

    long referenceID;

    private final transient int EVENT_MOVE_TOP = 0;
    private final transient int EVENT_MOVE_RIGHT = 1;
    private final transient int EVENT_MOVE_BOTTOM = 2;
    private final transient int EVENT_MOVE_LEFT = 3;
    private final transient int EVENT_MOVE_NONE = -1;

    int eventMoving = EVENT_MOVE_NONE; // cannot collide both X-axis and Y-axis, or else jump control breaks up.

    /**
     * in milliseconds
     */
    public final transient int INVINCIBILITY_TIME = 500;

    /**
     * Will ignore fluid resistance if (submerged height / actor height) <= this var
     */
    private final transient float FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO = 0.2f;
    private final transient float FLUID_RESISTANCE_APPLY_FULL_RATIO = 0.5f;

    private GameMap map;

    /**
     * Give new random ReferenceID and initialise ActorValue
     */
    public ActorWithBody() {
        referenceID = new HQRNG().nextLong();
        actorValue = new ActorValue();
        map = Terrarum.game.map;
    }

    /**
     *
     * @param w
     * @param h
     * @param tx +: translate drawn sprite to LEFT.
     * @param ty +: translate drawn sprite to DOWN.
     * @see ActorWithBody#drawBody(GameContainer, Graphics)
     * @see ActorWithBody#drawGlow(GameContainer, Graphics)
     */
    public void setHitboxDimension(int w, int h, int tx, int ty) {
        baseHitboxH = h;
        baseHitboxW = w;
        hitboxTranslateX = tx;
        hitboxTranslateY = ty;
    }

    /**
     * Set hitbox position from bottom-center point
     * @param x
     * @param y
     */
    public void setPosition(float x, float y) {
        hitbox = new Hitbox(
                x - ((baseHitboxW / 2) - hitboxTranslateX) * scale
                , y - (baseHitboxH - hitboxTranslateY) * scale
                , baseHitboxW * scale
                , baseHitboxH * scale
        );

        nextHitbox = new Hitbox(
                x - ((baseHitboxW / 2) - hitboxTranslateX) * scale
                , y - (baseHitboxH - hitboxTranslateY) * scale
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    public void setSprite(SpriteAnimation sprite) {this.sprite = sprite; }

    public void setSpriteGlow(SpriteAnimation sprite) { this.spriteGlow = sprite; }

    public void update(GameContainer gc, int delta_t) {
        if (update) {
            /**
             * Update variables
             */
            if (this instanceof Player) {
                noSubjectToGrav = isPlayerNoClip();
                noCollideWorld = isPlayerNoClip();
                noSubjectToFluidResistance = isPlayerNoClip();
            }

            if (mass < MASS_LOWEST) mass = MASS_LOWEST; // clamp to minimum possible mass
            if (sprite != null) {
                baseSpriteHeight = sprite.getHeight();
                baseSpriteWidth = sprite.getWidth();
            }
            gravitation = map.getGravitation();
            AUTO_CLIMB_RATE = (int) Math.min(TSIZE / 8 * FastMath.sqrt(scale), TSIZE);

            if (!isNoSubjectToGrav()) {
                applyGravitation();
                applyBuoyancy();
            }

            // hard limit velocity
            if (veloX > VELO_HARD_LIMIT) veloX = VELO_HARD_LIMIT;
            if (veloY > VELO_HARD_LIMIT) veloY = VELO_HARD_LIMIT;
            // limit velocity by fluid resistance
            //int tilePropResistance = getTileMvmtRstc();
            //if (!noSubjectToFluidResistance) {
            //    veloX *= mvmtRstcToMultiplier(tilePropResistance);
            //    veloY *= mvmtRstcToMultiplier(tilePropResistance);
            //}


            // Set 'next' positions to fiddle with
            updateNextHitboxFromVelo();


            // if not horizontally moving then ...
            //if (Math.abs(veloX) < 0.5) { // fix for special situations (see fig. 1 at the bottom of the source)
            //    updateVerticalPos();
            //    updateHorizontalPos();
            //}
            //else {
                updateHorizontalPos();
                updateVerticalPos();
            //}


            updateHitboxX();
            updateHitboxY();


            clampNextHitbox();
            clampHitbox();
        }
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is not implemented (and redundant)
     * so we manually reset G to zero (not applying G. force) if grounded.
     */
    // FIXME abnormal jump behaviour if mass < 2, same thing happens if mass == 0 (but zero mass is invalid anyway).
    private void applyGravitation() {
        if (!isGrounded()) {
            /**
             * weight; gravitational force in action
             * W = mass * G (9.8 [m/s^2])
             */
            float W = gravitation * mass;
            /**
             * Drag of atmosphere
             * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity) * A (area)
             */
            float A = scale * scale;
            float D = DRAG_COEFF * 0.5f * 1.292f * veloY * veloY * A;

            int fluidResistance = getTileMvmtRstc();

            veloY += clampCeil(
                    ((W - D) / mass) * SI_TO_GAME_ACC * G_MUL_PLAYABLE_CONST
                            // * mvmtRstcToMultiplier(fluidResistance) // eliminate shoot-up from fluids
                    , VELO_HARD_LIMIT
            );
        }
    }

    private void updateVerticalPos() {
        if (!isNoCollideWorld()) {
            // check downward
            if (veloY >= 0) {
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_BOTTOM)) {
                    adjustHitBottom();
                    elasticReflectY();
                    grounded = true;
                }
                else if (isColliding(CONTACT_AREA_BOTTOM, 0, 1)) {
                    elasticReflectY();
                    grounded = true;
                }
                else {
                    grounded = false;
                }
            }
            else if (veloY < 0) {
                grounded = false;

                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_TOP)) {
                    adjustHitTop();
                    elasticReflectY();
                }
                else if (isColliding(CONTACT_AREA_TOP, 0, -1)) {
                    elasticReflectY(); // for reversed gravity
                }
                else {
                }
            }
        }
    }

    private void adjustHitBottom() {
        float newX = nextHitbox.getPointedX(); // look carefully, getPos or getPointed
        // int-ify posY of nextHitbox
        nextHitbox.setPositionYFromPoint( FastMath.floor(nextHitbox.getPointedY()) );

        int newYOff = 0; // always positive

        boolean colliding;
        do {
            newYOff += 1;
            colliding = isColliding(CONTACT_AREA_BOTTOM, 0, -newYOff);
        } while (colliding);

        float newY = nextHitbox.getPointedY() - newYOff;
        nextHitbox.setPositionFromPoint(newX, newY);
    }

    private void adjustHitTop() {
        float newX = nextHitbox.getPosX();
        // int-ify posY of nextHitbox
        nextHitbox.setPositionY( FastMath.ceil(nextHitbox.getPosY()) );

        int newYOff = 0; // always positive

        boolean colliding;
        do {
            newYOff += 1;
            colliding = isColliding(CONTACT_AREA_TOP, 0, newYOff);
        } while (colliding);

        float newY = nextHitbox.getPosY() + newYOff;
        nextHitbox.setPosition(newX, newY);
    }

    private void updateHorizontalPos() {
        if (!isNoCollideWorld()) {
            // check right
            if (veloX >= 0.5) {
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_RIGHT) && !isColliding(CONTACT_AREA_LEFT)) {
                    adjustHitRight();
                    elasticReflectX();
                }
                else if (isColliding(CONTACT_AREA_RIGHT, 1, 0)
                        && !isColliding(CONTACT_AREA_LEFT, -1, 0)) {
                    elasticReflectX();
                }
                else {
                }
            }
            else if (veloX <= 0.5) {
                System.out.println("collidingleft");
                // order of the if-elseif chain is IMPORTANT
                if (isColliding(CONTACT_AREA_LEFT) && !isColliding(CONTACT_AREA_RIGHT)) {
                    adjustHitLeft();
                    elasticReflectX();
                }
                else if (isColliding(CONTACT_AREA_LEFT, -1, 0)
                        && !isColliding(CONTACT_AREA_RIGHT, 1, 0)) {
                    elasticReflectX();
                }
                else {
                }
            }
            else {
                System.out.println("updatehorizontal - |velo| < 0.5");
                if (isColliding(CONTACT_AREA_LEFT) || isColliding(CONTACT_AREA_RIGHT)) {
                    // elasticReflectX();
                }
            }
        }
    }

    private void adjustHitRight() {
        float newY = nextHitbox.getPosY(); // look carefully, getPos or getPointed
        // int-ify posY of nextHitbox
        nextHitbox.setPositionX( FastMath.floor(nextHitbox.getPosX() + nextHitbox.getWidth())
                - nextHitbox.getWidth()
        );

        int newXOff = 0; // always positive

        boolean colliding;
        do {
            newXOff += 1;
            colliding = isColliding(CONTACT_AREA_BOTTOM, -newXOff, 0);
        } while (newXOff < TSIZE && colliding);

        float newX = nextHitbox.getPosX() - newXOff;
        nextHitbox.setPosition(newX, newY);
    }

    private void adjustHitLeft() {
        float newY = nextHitbox.getPosY();
        // int-ify posY of nextHitbox
        nextHitbox.setPositionX( FastMath.ceil(nextHitbox.getPosX()) );

        int newXOff = 0; // always positive

        boolean colliding;
        do {
            newXOff += 1;
            colliding = isColliding(CONTACT_AREA_TOP, newXOff, 0);
        } while (newXOff < TSIZE && colliding);

        float newX = nextHitbox.getPosX() + newXOff;
        nextHitbox.setPosition(newX, newY); // + 1; float-point rounding compensation (i think...)
    }

    private void elasticReflectX() {
        if (veloX != 0) veloX = -veloX * elasticity;
    }

    private void elasticReflectY() {
        if (veloY != 0) veloY = -veloY * elasticity;
    }

    private boolean isColliding(int side) {
        return isColliding(side, 0, 0);
    }

    private boolean isColliding(int side, int tx, int ty) {
        return getContactingArea(side, tx, ty) > 1;
    }

    private int getContactingArea(int side) {
        return getContactingArea(side, 0, 0);
    }

    private int getContactingArea(int side, int translateX, int translateY) {
        int contactAreaCounter = 0;
        for (int i = 0
                ; i < Math.round((side % 2 == 0) ? nextHitbox.getWidth() : nextHitbox.getHeight())
                ; i++) {
            // set tile positions
            int tileX, tileY;
            /*if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxStart().getX())
                        + i + translateX);
                tileY = div16TruncateToMapHeight(FastMath.floor(nextHitbox.getHitboxEnd().getY())
                        + translateY);
            }
            else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxStart().getX())
                        + i + translateX);
                tileY = div16TruncateToMapHeight(FastMath.ceil(nextHitbox.getHitboxStart().getY())
                        + translateY);
            }
            else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(FastMath.floor(nextHitbox.getHitboxEnd().getX())
                        + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxStart().getY())
                        + i + translateY);
            }
            else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(FastMath.ceil(nextHitbox.getHitboxStart().getX())
                        + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxStart().getY())
                        + i + translateY);
            }*/
            if (side == CONTACT_AREA_BOTTOM) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxStart().getX())
                        + i + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxEnd().getY())
                        + translateY);
            }
            else if (side == CONTACT_AREA_TOP) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxStart().getX())
                        + i + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxStart().getY())
                        + translateY);
            }
            else if (side == CONTACT_AREA_RIGHT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxEnd().getX())
                        + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxStart().getY())
                        + i + translateY);
            }
            else if (side == CONTACT_AREA_LEFT) {
                tileX = div16TruncateToMapWidth(Math.round(nextHitbox.getHitboxStart().getX())
                        + translateX);
                tileY = div16TruncateToMapHeight(Math.round(nextHitbox.getHitboxStart().getY())
                        + i + translateY);
            }
            else {
                throw new IllegalArgumentException(String.valueOf(side) + ": Wrong side input");
            }

            // evaluate
            if (TilePropCodex.getProp(map.getTileFromTerrain(tileX, tileY)).isSolid()) {
                contactAreaCounter += 1;
            }
        }

        return contactAreaCounter;
    }

    /**
     * [N] = [kg * m / s^2]
     * F(bo) = density * submerged_volume * gravitational_acceleration [N]
     */
    private void applyBuoyancy() {
        int fluidDensity = getTileDensity();
        float submergedVolume = getSubmergedVolume();

        if (!isPlayerNoClip() && !grounded) {
            // System.out.println("density: "+density);
            veloY -= ((fluidDensity - this.density)
                            * map.getGravitation() * submergedVolume
                            * Math.pow(mass, -1)
                    * SI_TO_GAME_ACC);
        }
    }

    private float getSubmergedVolume() {
        float GAME_TO_SI_VOL = FastMath.pow((1f/METER), 3);

        if( density > 0 ){
            return getSubmergedHeight()
                    * nextHitbox.getWidth() * nextHitbox.getWidth()
                    * GAME_TO_SI_VOL
            ;
            //System.out.println("fluidHeight: "+fluidHeight+", submerged: "+submergedVolume);
            //submergedHeight / TILE_SIZE * 1^2 (pixel to meter)
        }
        else{
            return 0;
        }
    }

    private float getSubmergedHeight() {
        return FastMath.clamp(
                nextHitbox.getPointedY() - getFluidLevel()
                , 0
                , nextHitbox.getHeight()
        );
    }

    private int getFluidLevel() {
        int tilePosXStart = Math.round(nextHitbox.getPosX() / TSIZE);
        int tilePosXEnd = Math.round(nextHitbox.getHitboxEnd().getX() / TSIZE);
        int tilePosY = Math.round(nextHitbox.getPosY() / TSIZE);

        int fluidHeight = 2147483647;

        for (int x = tilePosXStart; x <= tilePosXEnd; x++) {
            int tile = map.getTileFromTerrain(x, tilePosY);
            if ( TilePropCodex.getProp(tile).isFluid()
                    && tilePosY * TSIZE < fluidHeight ){
                fluidHeight = tilePosY * TSIZE;
            }
        }

        return fluidHeight;
    }

    /**
     * Get highest friction value from feet tiles.
     * @return
     */
    private int getTileFriction(){
        int friction = 0;

        int tilePosXStart = Math.round(nextHitbox.getPosX() / TSIZE);
        int tilePosXEnd = Math.round(nextHitbox.getHitboxEnd().getX() / TSIZE);
        int tilePosY = Math.round(nextHitbox.getPointedY() / TSIZE);

        //get density
        for (int x = tilePosXStart; x <= tilePosXEnd; x++) {
            int tile = map.getTileFromTerrain(x, tilePosY);
            if (TilePropCodex.getProp(tile).isFluid()) {
                int thisFluidDensity = TilePropCodex.getProp(tile).getFriction();

                if (thisFluidDensity > friction) friction = thisFluidDensity;
            }
        }

        return friction;
    }

    /**
     * Get highest movement resistance value from tiles that the body occupies.
     * @return
     */
    private int getTileMvmtRstc(){
        int resistance = 0;

        int tilePosXStart = Math.round(nextHitbox.getPosX() / TSIZE);
        int tilePosYStart = Math.round(nextHitbox.getPosY() / TSIZE);
        int tilePosXEnd = Math.round(nextHitbox.getHitboxEnd().getX() / TSIZE);
        int tilePosYEnd = Math.round(nextHitbox.getHitboxEnd().getY() / TSIZE);

        //get density
        for (int y = tilePosYStart; y <= tilePosYEnd; y++) {
            for (int x = tilePosXStart; x <= tilePosXEnd; x++) {
                int tile = map.getTileFromTerrain(x, y);
                if (TilePropCodex.getProp(tile).isFluid()) {
                    int thisFluidDensity = TilePropCodex.getProp(tile).getMovementResistance();

                    if (thisFluidDensity > resistance) resistance = thisFluidDensity;
                }
            }
        }

        return resistance;
    }

    /**
     * Get highest density (specific gravity) value from tiles that the body occupies.
     * @return
     */
    private int getTileDensity() {
        int density = 0;

        int tilePosXStart = Math.round(nextHitbox.getPosX() / TSIZE);
        int tilePosYStart = Math.round(nextHitbox.getPosY() / TSIZE);
        int tilePosXEnd = Math.round(nextHitbox.getHitboxEnd().getX() / TSIZE);
        int tilePosYEnd = Math.round(nextHitbox.getHitboxEnd().getY() / TSIZE);

        //get density
        for (int y = tilePosYStart; y <= tilePosYEnd; y++) {
            for (int x = tilePosXStart; x <= tilePosXEnd; x++) {
                int tile = map.getTileFromTerrain(x, y);
                if (TilePropCodex.getProp(tile).isFluid()) {
                    int thisFluidDensity = TilePropCodex.getProp(tile).getDensity();

                    if (thisFluidDensity > density) density = thisFluidDensity;
                }
            }
        }

        return density;
    }

    private float mvmtRstcToMultiplier(int movementResistanceValue) {
        return 1f / (1 + (movementResistanceValue / 16f));
    }

    private void clampHitbox() {
        hitbox.setPositionFromPoint(
                clampW(hitbox.getPointedX())
                , clampH(hitbox.getPointedY())
        );
    }

    private void clampNextHitbox() {
        nextHitbox.setPositionFromPoint(
                clampW(nextHitbox.getPointedX())
                , clampH(nextHitbox.getPointedY())
        );
    }

    private void updateNextHitboxFromVelo() {
        float fluidResistance = mvmtRstcToMultiplier(getTileMvmtRstc());
        float submergedRatio = FastMath.clamp(
                getSubmergedHeight() / nextHitbox.getHeight()
                , 0f, 1f
        );

        boolean applyResistance = (!isNoSubjectToFluidResistance()
                && submergedRatio > FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO
        );
        float resistanceMulInterValueSize = FLUID_RESISTANCE_APPLY_FULL_RATIO - FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO;
        float resistanceMultiplier = FastMath.interpolateLinear(
                (submergedRatio - FLUID_RESISTANCE_IGNORE_THRESHOLD_RATIO)
                * FastMath.pow(resistanceMulInterValueSize, -1)
                , 0, 1
        );
        float adjustedResistance = FastMath.interpolateLinear(
                resistanceMultiplier
                , 1f, fluidResistance
        );

        nextHitbox.set(
                  Math.round(hitbox.getPosX()
                          + (veloX
                          * (!applyResistance ? 1 : adjustedResistance)
                  ))
                , Math.round(hitbox.getPosY()
                          + (veloY
                          * (!applyResistance ? 1 : adjustedResistance)
                  ))
                , Math.round(baseHitboxW * scale)
                , Math.round(baseHitboxH * scale)
                /** Full quantisation; wonder what havoc these statements would wreak...
                 */
        );
    }

    private void updateHitboxX() {
        hitbox.setDimension(
                nextHitbox.getWidth()
                , nextHitbox.getHeight()
        );
        hitbox.setPositionX(nextHitbox.getPosX());
    }

    private void updateHitboxY() {
        hitbox.setDimension(
                nextHitbox.getWidth()
                , nextHitbox.getHeight()
        );
        hitbox.setPositionY(nextHitbox.getPosY());
    }

    @Override
    public void drawGlow(GameContainer gc, Graphics g) {
        if (visible && spriteGlow != null) {
            if (!sprite.flippedHorizontal()) {
                spriteGlow.render(g
                        , (hitbox.getPosX() - (hitboxTranslateX * scale))
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 2
                        , scale
                );
            }
            else {
                spriteGlow.render(g
                        , (hitbox.getPosX() - scale)
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 2
                        , scale
                );
            }
        }
    }

    @Override
    public void drawBody(GameContainer gc, Graphics g) {
        if (visible && sprite != null) {
            if (!sprite.flippedHorizontal()) {
                sprite.render(g
                        , (hitbox.getPosX() - (hitboxTranslateX * scale))
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 2
                        , scale
                );
            }
            else {
                sprite.render(g
                        , (hitbox.getPosX() - scale)
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 2
                        , scale
                );
            }
        }
    }

    @Override
    public void updateGlowSprite(GameContainer gc, int delta_t) {
        if (spriteGlow != null) spriteGlow.update(delta_t);
    }

    @Override
    public void updateBodySprite(GameContainer gc, int delta_t) {
        if (sprite != null) sprite.update(delta_t);
    }

    @Override
    public long getRefID() {
        return referenceID;
    }

    private float clampW(float x) {
        if (x < TSIZE + nextHitbox.getWidth() / 2) {
            return TSIZE + nextHitbox.getWidth() / 2;
        }
        else if (x >= map.width * TSIZE - TSIZE - nextHitbox.getWidth() / 2) {
            return map.width * TSIZE - 1 - TSIZE - nextHitbox.getWidth() / 2;
        }
        else {
            return x;
        }
    }

    private float clampH(float y) {
        if (y < TSIZE + nextHitbox.getHeight()) {
            return TSIZE + nextHitbox.getHeight();
        }
        else if (y >= map.height * TSIZE - TSIZE - nextHitbox.getHeight()) {
            return map.height * TSIZE - 1 - TSIZE - nextHitbox.getHeight();
        }
        else {
            return y;
        }
    }

    private int clampWtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= map.width) {
            return map.width - 1;
        }
        else {
            return x;
        }
    }

    private int clampHtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= map.height) {
            return map.height - 1;
        }
        else {
            return x;
        }
    }

    private boolean isPlayerNoClip() {
        return (this instanceof Player && ((Player) this).isNoClip());
    }

    private static int div16(int x) {
        if (x < 0) { throw new IllegalArgumentException("div16: Positive integer only: "
                + String.valueOf(x)); }
        return (x & 0x7FFF_FFFF) >> 4;
    }

    private static int div16TruncateToMapWidth(int x) {
        if (x < 0) return 0;
        else if (x >= Terrarum.game.map.width << 4) return Terrarum.game.map.width - 1;
        else return (x & 0x7FFF_FFFF) >> 4;
    }

    private static int div16TruncateToMapHeight(int y) {
        if (y < 0) return 0;
        else if (y >= Terrarum.game.map.height << 4) return Terrarum.game.map.height - 1;
        else return (y & 0x7FFF_FFFF) >> 4;
    }

    private static int mod16(int x) {
        if (x < 0) { throw new IllegalArgumentException("mod16: Positive integer only: "
                + String.valueOf(x)); }
        return x & 0b1111;
    }

    private static float clampCeil(float x, float ceil) {
        return (Math.abs(x) > ceil ? ceil : x);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public void setVeloX(float veloX) {
        this.veloX = veloX;
    }

    public void setVeloY(float veloY) {
        this.veloY = veloY;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public boolean isVisible() {
        return visible;
    }

    public float getScale() {
        return scale;
    }

    public float getMass() {
        return mass;
    }

    public float getVeloX() {
        return veloX;
    }

    public float getVeloY() {
        return veloY;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public int getBaseHitboxW() {
        return baseHitboxW;
    }

    public int getBaseHitboxH() {
        return baseHitboxH;
    }

    public float getHitboxTranslateX() {
        return hitboxTranslateX;
    }

    public float getHitboxTranslateY() {
        return hitboxTranslateY;
    }

    public Hitbox getHitbox() {
        return hitbox;
    }

    public Hitbox getNextHitbox() {
        return nextHitbox;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    private int quantiseTSize(float v) {
        return FastMath.floor(v / TSIZE) * TSIZE;
    }

    public boolean isNoSubjectToGrav() {
        return noSubjectToGrav;
    }

    public void setNoSubjectToGrav(boolean noSubjectToGrav) {
        this.noSubjectToGrav = noSubjectToGrav;
    }

    public boolean isNoCollideWorld() {
        return noCollideWorld;
    }

    public boolean isNoSubjectToFluidResistance() {
        return noSubjectToFluidResistance;
    }

    public void setNoCollideWorld(boolean noCollideWorld) {
        this.noCollideWorld = noCollideWorld;
    }

    public void setNoSubjectToFluidResistance(boolean noSubjectToFluidResistance) {
        this.noSubjectToFluidResistance = noSubjectToFluidResistance;
    }

    public float getElasticity() {
        return elasticity;
    }

    public void setElasticity(float elasticity) {
        if (elasticity < 0)
            throw new IllegalArgumentException("[ActorWithBody] " + elasticity + ": valid elasticity value is [0, 1].");

        if (elasticity > 1) {
            System.out.println("[ActorWithBody] Elasticity were capped to 1.");
            this.elasticity = ELASTICITY_MAX;
        }
        else this.elasticity = elasticity * ELASTICITY_MAX;
    }

    public void setDensity(int density) {
        if (density < 0)
            throw new IllegalArgumentException("[ActorWithBody] " + density + ": density cannot be negative.");

        this.density = density;
    }
}

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