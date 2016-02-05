package com.Torvald.Terrarum.Actors;

import com.Torvald.Rand.HighQualityRandom;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
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
    private @NotNull float veloX, veloY;
    private final float VELO_HARD_LIMIT = 10000;

    private boolean grounded = false;

    SpriteAnimation sprite;
    @Nullable SpriteAnimation spriteGlow;
    private boolean visible = false;
    private boolean update = true;

    @NotNull int baseSpriteWidth, baseSpriteHeight;

    /**
     * Positions: top-left point
     */
    private @NotNull Hitbox hitbox, nextHitbox;

    /**
     * Physical properties
     */
    private float scale = 1;
    private float mass = 1f;

    private static int TSIZE = MapDrawer.TILE_SIZE;

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

    /**
     * A constant to make falling faster so that the game is more playable
     */
    private final float G_MUL_PLAYABLE_CONST = 1.4142f;

    long referenceID;

    public ActorWithBody() {
        referenceID = new HighQualityRandom(0x7E22A211AAL).nextLong();
        actorValue = new ActorValue();
    }

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
            gravitation = Game.map.getGravitation();

            if (!playerNoClip()) {
                applyGravitation();
            }

            // hard limit velocity
            if (veloX > VELO_HARD_LIMIT) veloX = VELO_HARD_LIMIT;
            if (veloY > VELO_HARD_LIMIT) veloY = VELO_HARD_LIMIT;

            // Set 'next' positions to fiddle with
            updateNextHitbox(delta_t);

            if (!playerNoClip()) {
                updateVerticalPos();
                updateHorizontalPos();
            }

            // Apply previous fiddling
            updateHitbox();


            /**
             *  clamp position
             */
            hitbox.setPositionFromPoint(
                    clampW(hitbox.getPointedX())
                    , clampH(hitbox.getPointedY())
            );
            nextHitbox.setPositionFromPoint(
                    clampW(nextHitbox.getPointedX())
                    , clampH(nextHitbox.getPointedY())
            );
        }
    }

    @Override
    public void drawGlow(GameContainer gc, Graphics g) {
        if (visible && spriteGlow != null) {
            if (!sprite.flippedHorizontal()) {
                spriteGlow.render(g
                        , Math.round(hitbox.getPosX() - (hitboxTranslateX * scale))
                        , Math.round(hitbox.getPosY() - hitboxTranslateY * scale)
                                  - (baseSpriteHeight - baseHitboxH) * scale
                                  + 1
                        , scale
                );
            }
            else {
                spriteGlow.render(g
                        , Math.round(hitbox.getPosX() - scale)
                        , Math.round(hitbox.getPosY() - hitboxTranslateY * scale)
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
                        , Math.round(hitbox.getPosX() - (hitboxTranslateX * scale))
                        , Math.round(hitbox.getPosY() - hitboxTranslateY * scale)
                                - (baseSpriteHeight - baseHitboxH) * scale
                                + 1
                        , scale
                );
            }
            else {
                sprite.render(g
                        , Math.round(hitbox.getPosX() - scale)
                        , Math.round(hitbox.getPosY() - hitboxTranslateY * scale)
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

    boolean collideBottomAndAdjusted() {
        // noclip off?
        int feetTileX = clampWtile(Math.round((nextHitbox.getPointedX()) / TSIZE));
        int feetTileY = clampHtile(FastMath.floor(nextHitbox.getPointedY() / TSIZE));

        if (feetTileX < 0) feetTileX = 0;
        if (feetTileY < 0) feetTileY = 0;

        int feetTile = Game.map.getTileFromTerrain(feetTileX, feetTileY);

        if (feetTile != 0) {
            nextHitbox.setPositionYFromPoint(
                    feetTileY * TSIZE
            );
            return true;
        }
        else {
            return false;
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
        if (collideBottomAndAdjusted()) {
            grounded = true;
            veloY = 0;
        }
        else {
            grounded = false;
        }
    }

    private void updateHorizontalPos() {

    }

    private void updateNextHitbox(int delta_t) {
        nextHitbox.set(
                hitbox.getPosX() + veloX
                , hitbox.getPosY() + veloY
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    private void updateHitbox() {
        hitbox.set(
                nextHitbox.getPosX()
                , nextHitbox.getPosY()
                , baseHitboxW * scale
                , baseHitboxH * scale
        );
    }

    @Override
    public long getRefID() {
        return referenceID;
    }

    public float pointedPosX() { return hitbox.getPointedX(); }
    public float pointedPosY() { return hitbox.getPointedY(); }
    public float topLeftPosX() { return hitbox.getPosX(); }
    public float topLeftPosY() { return hitbox.getPosY(); }

    private static float clampW(float x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Game.map.width * TSIZE) {
            return Game.map.width * TSIZE - 1;
        }
        else {
            return x;
        }
    }

    private static float clampH(float x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Game.map.height * TSIZE) {
            return Game.map.height * TSIZE - 1;
        }
        else {
            return x;
        }
    }

    private static int clampWtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Game.map.width) {
            return Game.map.width - 1;
        }
        else {
            return x;
        }
    }

    private static int clampHtile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x >= Game.map.height) {
            return Game.map.height - 1;
        }
        else {
            return x;
        }
    }

    private boolean playerNoClip() {
        return (this instanceof Player && ((Player) this).isNoClip());
    }

    private static int div16(int x) {
        if (x < 0) { throw new IllegalArgumentException("Positive integer only!"); }
        return (x & 0x7FFF_FFFF) >> 4;
    }

    private static int mod16(int x) {
        if (x < 0) { throw new IllegalArgumentException("Positive integer only!"); }
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
}
