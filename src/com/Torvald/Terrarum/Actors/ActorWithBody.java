package com.Torvald.Terrarum.Actors;

import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.spriteAnimation.SpriteAnimation;
import com.jme3.math.FastMath;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

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
    private final float VELO_HARD_LIMIT = 10000;

    boolean grounded = false;
    boolean walledLeft = false;
    boolean walledRight = false;

    SpriteAnimation sprite;
    @Nullable SpriteAnimation spriteGlow;
    private boolean visible = false;
    private boolean update = true;

    @NotNull int baseSpriteWidth, baseSpriteHeight;

    /**
     * Positions: top-left point
     */
    private volatile @NotNull Hitbox hitbox, nextHitbox;

    /**
     * Physical properties
     */
    private volatile float scale = 1;
    private volatile float mass = 1f;

    private static final int TSIZE = MapDrawer.TILE_SIZE;
    private static final int AUTO_CLIMB_RATE = TSIZE / 4;

    /**
     * Gravitational Constant G. Load from GameMap.
     * [m / s^2]
     * s^2 = 1/FPS = 1/60 if FPS is targeted to 60
     * meter to pixel : 24/FPS
     */
    private final float METER = 24f;
    private final float SI_TO_GAME_ACC = METER / (Terrarum.TARGET_FPS * Terrarum.TARGET_FPS);
    private final float SI_TO_GAME_VEL = METER / Terrarum.TARGET_FPS;
    private float gravitation;
    private final float DRAG_COEFF = 1f;

    private final int CONTACT_AREA_TOP = 0;
    private final int CONTACT_AREA_RIGHT = 1;
    private final int CONTACT_AREA_BOTTOM = 2;
    private final int CONTACT_AREA_LEFT = 3;

    private final int UD_COMPENSATOR_MAX = TSIZE;
    private final int LR_COMPENSATOR_MAX = TSIZE;
    private final int TILE_CLIMB_RATE = 4;

    /**
     * A constant to make falling faster so that the game is more playable
     */
    private final float G_MUL_PLAYABLE_CONST = 1.4142f;

    long referenceID;

    /**
     * Give new random ReferenceID and initialise ActorValue
     */
    public ActorWithBody() {
        referenceID = new HQRNG(0x7E22A211AAL).nextLong();
        actorValue = new ActorValue();
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
            baseSpriteHeight = sprite.getHeight();
            baseSpriteWidth = sprite.getWidth();
            gravitation = Terrarum.game.map.getGravitation();

            if (!playerNoClip()) {
                applyGravitation();
            }

            // hard limit velocity
            if (veloX > VELO_HARD_LIMIT) veloX = VELO_HARD_LIMIT;
            if (veloY > VELO_HARD_LIMIT) veloY = VELO_HARD_LIMIT;

            // Set 'next' positions to fiddle with
            updateNextHitboxY();
            updateVerticalPos();
            clampNextHitbox();
            updateHitboxY();

            updateNextHitboxX();
            updateHorizontalPos();
            clampNextHitbox();
            updateHitboxX();

            clampHitbox();
        }
    }

    /**
     * Apply gravitation to the every falling body (unless not levitating)
     *
     * Apply only if not grounded; normal force is not implemented (and redundant)
     * so we manually reset G to zero (not applying G. force) if grounded.
     */
    // FIXME abnormal jump behaviour if mass == 1, same thing happens if mass == 0 but zero mass
    // is invalid anyway.
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

            veloY += clampCeil(((W - D) / mass) * SI_TO_GAME_ACC * G_MUL_PLAYABLE_CONST
                    , VELO_HARD_LIMIT
            );
        }
    }

    private void updateVerticalPos() {
        if (!playerNoClip()) {
            if (collidedBottomAndAdjusted()) {
                grounded = true;
                veloY = 0;
            }
            else {
                grounded = false;
            }

            if (collidedTopAndAdjusted()) {
                veloY = 0;
            }
        }
    }

    boolean collidedBottomAndAdjusted() {
        if (getContactArea(CONTACT_AREA_BOTTOM, 0, 1) == 0) {
            return false;
        }
        /**
         * seemingly adjusted and one pixel below has ground
         *
         * seemingly adjusted: adjustHitBottom sets position one pixel above the ground
         * (stepping on ground in-game look, as the sprite render is one pixel offseted to Y)
         */
        else if (getContactArea(CONTACT_AREA_BOTTOM, 0, 1) > 0
                && getContactArea(CONTACT_AREA_BOTTOM, 0, 0) == 0) {
            return true;
        }
        else {
            adjustHitBottom();
            return true;
        }
    }

    boolean collidedTopAndAdjusted() {
        if (getContactArea(CONTACT_AREA_TOP, 0, -1) == 0) {
            return false;
        }
        /**
         * seemingly adjusted and one pixel below has ground
         *
         * seemingly adjusted: adjustHitBottom sets position one pixel above the ground
         * (stepping on ground in-game look, as the sprite render is one pixel offseted to Y)
         */
        else if (getContactArea(CONTACT_AREA_TOP, 0, -1) > 0
                && getContactArea(CONTACT_AREA_TOP, 0, 0) == 0) {
            return true;
        }
        else {
            adjustHitTop();
            return true;
        }
    }

    private void updateHorizontalPos() {
        if (!playerNoClip()) {
            if (collidedRightAndAdjusted()) { // treat as 'event--collided right'
                veloX = 0;
                walledRight = true;

                // TODO remove above two lines and implement tile climb (multi-frame calculation.)
                // Use variable TILE_CLIMB_RATE
            }
            else if (collidedLeftAndAdjusted()) { // treat as 'event--collided left'
                veloX = 0;
                walledLeft = true;
            }
            else {
                walledRight = false;
                walledLeft = false;
            }
        }
    }

    boolean collidedRightAndAdjusted() {
        if (getContactArea(CONTACT_AREA_RIGHT, 1, 0) == 0) {
            return false;
        }
        /**
         * seemingly adjusted and one pixel below has ground
         *
         * seemingly adjusted: adjustHitBottom sets position one pixel above the ground
         * (stepping on ground in-game look, as the sprite render is one pixel offseted to Y)
         */
        else if (getContactArea(CONTACT_AREA_RIGHT, 1, 0) > 0
                && getContactArea(CONTACT_AREA_RIGHT, 0, 0) == 0) {
            return true;
        }
        else {
            adjustHitRight();
            return true;
        }
    }

    boolean collidedLeftAndAdjusted() {
        if (getContactArea(CONTACT_AREA_LEFT, -1, 0) == 0) {
            return false;
        }
        /**
         * seemingly adjusted and one pixel below has ground
         *
         * seemingly adjusted: adjustHitBottom sets position one pixel above the ground
         * (stepping on ground in-game look, as the sprite render is one pixel offseted to Y)
         */
        else if (getContactArea(CONTACT_AREA_LEFT, -1, 0) > 0
                && getContactArea(CONTACT_AREA_LEFT, 0, 0) == 0) {
            return true;
        }
        else {
            adjustHitLeft();
            return true;
        }
    }

    private void updateNextHitboxX() {
        nextHitbox.set(
                hitbox.getPosX() + veloX
                , hitbox.getPosY()
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    private void updateNextHitboxY() {
        nextHitbox.set(
                hitbox.getPosX()
                , hitbox.getPosY() + veloY
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    private void updateHitboxX() {
        hitbox.set(
                nextHitbox.getPosX()
                , hitbox.getPosY()
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    private void updateHitboxY() {
        hitbox.set(
                hitbox.getPosX()
                , nextHitbox.getPosY()
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    private void adjustHitBottom() {
        int tY = 0;
        int contactArea = getContactArea(CONTACT_AREA_BOTTOM, 0, 0);
        for (int lim = 0; lim < UD_COMPENSATOR_MAX; lim++) {
            /**
             * get contact area and move up and get again.
             * keep track of this value, and some point they will be set as lowest
             * and become static. The very point where the value first became lowest
             * is the value what we want.
             */
            int newContactArea = getContactArea(CONTACT_AREA_BOTTOM, 0, -lim);

            if (newContactArea < contactArea) {
                tY = -lim;
            }
            contactArea = newContactArea;
        }
        nextHitbox.setPositionYFromPoint(FastMath.ceil(nextHitbox.getPointedY() + tY));
    }

    private void adjustHitTop() {
        int tY = 0;
        int contactArea = getContactArea(CONTACT_AREA_TOP, 0, 0);
        for (int lim = 0; lim < UD_COMPENSATOR_MAX; lim++) {
            /**
             * get contact area and move up and get again.
             * keep track of this value, and some point they will be set as lowest
             * and become static. The very point where the value first became lowest
             * is the value what we want.
             */
            int newContactArea = getContactArea(CONTACT_AREA_TOP, 0, lim);

            if (newContactArea < contactArea) {
                tY = lim;
            }
            contactArea = newContactArea;
        }
        nextHitbox.setPositionYFromPoint(FastMath.floor(nextHitbox.getPointedY() + tY));
    }

    private void adjustHitRight() {
        int tX = 0;
        int contactArea = getContactArea(CONTACT_AREA_RIGHT, 0, 0);
        for (int lim = 0; lim < LR_COMPENSATOR_MAX; lim++) {
            /**
             * get contact area and move up and get again.
             * keep track of this value, and some point they will be set as lowest
             * and become static. The very point where the value first became lowest
             * is the value what we want.
             */
            int newContactArea = getContactArea(CONTACT_AREA_RIGHT, -lim, 0);

            if (newContactArea < contactArea) {
                tX = -lim;
            }
            contactArea = newContactArea;
        }
        //nextHitbox.setPositionYFromPoint(nextHitbox.getPointedX() + tX);
        nextHitbox.set(
                FastMath.ceil(nextHitbox.getPosX() + tX)
                , nextHitbox.getPosY()
                , nextHitbox.getWidth()
                , nextHitbox.getHeight()
        );
    }

    private void adjustHitLeft() {
        int tX = 0;
        int contactArea = getContactArea(CONTACT_AREA_LEFT, 0, 0);
        for (int lim = 0; lim < LR_COMPENSATOR_MAX; lim++) {
            /**
             * get contact area and move up and get again.
             * keep track of this value, and some point they will be set as lowest
             * and become static. The very point where the value first became lowest
             * is the value what we want.
             */
            int newContactArea = getContactArea(CONTACT_AREA_LEFT, lim, 0);

            if (newContactArea < contactArea) {
                tX = lim;
            }
            contactArea = newContactArea;
        }
        //nextHitbox.setPositionYFromPoint(nextHitbox.getPointedX() + tX);
        nextHitbox.set(
                FastMath.floor(nextHitbox.getPosX() + tX)
                , nextHitbox.getPosY()
                , nextHitbox.getWidth()
                , nextHitbox.getHeight()
        );
    }

    private int getContactArea(int side, int translateX, int translateY) {
        int contactAreaCounter = 0;
        for (int i = 0
                ; i < Math.round((side % 2 == 0) ? nextHitbox.getWidth() : nextHitbox.getHeight())
                ; i++) {
            // set tile positions
            int tileX, tileY;
            if (side == CONTACT_AREA_BOTTOM) {
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
            }
            else {
                throw new IllegalArgumentException(String.valueOf(side) + ": Wrong side input");
            }

            // evaluate
            if (Terrarum.game.map.getTileFromTerrain(tileX, tileY) > 0) {
                contactAreaCounter += 1;
            }
        }

        return contactAreaCounter;
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

    @Override
    public void drawGlow(GameContainer gc, Graphics g) {
        if (visible && spriteGlow != null) {
            if (!sprite.flippedHorizontal()) {
                spriteGlow.render(g
                        , (hitbox.getPosX() - (hitboxTranslateX * scale))
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 1
                        , scale
                );
            }
            else {
                spriteGlow.render(g
                        , (hitbox.getPosX() - scale)
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 1
                        , scale
                );
            }
        }
    }

    @Override
    public void drawBody(GameContainer gc, Graphics g) {
        if (visible) {
            if (!sprite.flippedHorizontal()) {
                sprite.render(g
                        , (hitbox.getPosX() - (hitboxTranslateX * scale))
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 1
                        , scale
                );
            }
            else {
                sprite.render(g
                        , (hitbox.getPosX() - scale)
                        , (hitbox.getPosY() + (hitboxTranslateY * scale))
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 1
                        , scale
                );
            }
        }
    }

    @Override
    public void updateGlowSprite(GameContainer gc, int delta_t) {
        if (spriteGlow != null) {
            spriteGlow.update(delta_t);
        }
    }

    @Override
    public void updateBodySprite(GameContainer gc, int delta_t) {
        sprite.update(delta_t);
    }

    @Override
    public long getRefID() {
        return referenceID;
    }

    public float pointedPosX() { return hitbox.getPointedX(); }
    public float pointedPosY() { return hitbox.getPointedY(); }
    public float topLeftPosX() { return hitbox.getPosX(); }
    public float topLeftPosY() { return hitbox.getPosY(); }

    private float clampW(float x) {
        if (x < TSIZE + nextHitbox.getWidth() / 2) {
            return TSIZE + nextHitbox.getWidth() / 2;
        }
        else if (x >= Terrarum.game.map.width * TSIZE - TSIZE - nextHitbox.getWidth() / 2) {
            return Terrarum.game.map.width * TSIZE - 1 - TSIZE - nextHitbox.getWidth() / 2;
        }
        else {
            return x;
        }
    }

    private float clampH(float y) {
        if (y < TSIZE + nextHitbox.getHeight()) {
            return TSIZE + nextHitbox.getHeight();
        }
        else if (y >= Terrarum.game.map.height * TSIZE - TSIZE - nextHitbox.getHeight()) {
            return Terrarum.game.map.height * TSIZE - 1 - TSIZE - nextHitbox.getHeight();
        }
        else {
            return y;
        }
    }

    private int clampWtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Terrarum.game.map.width) {
            return Terrarum.game.map.width - 1;
        }
        else {
            return x;
        }
    }

    private int clampHtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Terrarum.game.map.height) {
            return Terrarum.game.map.height - 1;
        }
        else {
            return x;
        }
    }

    private boolean playerNoClip() {
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

    public boolean isWalledLeft() {
        return walledLeft;
    }

    public boolean isWalledRight() {
        return walledRight;
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

    private int clampMulOfTSize(float v) {
        return (Math.round(v) / TSIZE) * TSIZE;
    }
}
