package com.Torvald.Terrarum.Actors;

import com.Torvald.spriteAnimation.SpriteAnimation;

/**
 * Created by minjaesong on 16-01-14.
 */
public class PlayerDebugger {

    private Actor actor;

    public PlayerDebugger(Actor actor) {
        this.actor = actor;
    }

    public Player getPlayer() {
        if (actor instanceof Player) {
            return (Player) actor;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Delegates for Player instances
     */

    public float baseHitboxW() { return getPlayer().getBaseHitboxW(); }
    public float baseHitboxH() { return getPlayer().getBaseHitboxH(); }
    public float hitboxTranslateX() { return getPlayer().getHitboxTranslateX(); }
    public float hitboxTranslateY() { return getPlayer().getHitboxTranslateY(); }
    public float veloX() { return getPlayer().getVeloX(); }
    public float veloY() { return getPlayer().getVeloY(); }
    public int baseSpriteWidth() { return getPlayer().baseSpriteWidth; }
    public int baseSpriteHeight() { return getPlayer().baseSpriteHeight; }
    public SpriteAnimation sprite() { return getPlayer().sprite; }
    public float scale() { return getPlayer().getScale(); }
    public Hitbox hitbox() { return getPlayer().getHitbox(); }
    public Hitbox nextHitbox() { return getPlayer().getNextHitbox(); }
    public boolean grounded() { return getPlayer().isGrounded(); }
    public ActorValue actorValue() { return getPlayer().getActorValue(); }
    public float mass() { return getPlayer().getMass(); }
    public boolean noClip() { return getPlayer().isNoClip(); }
    public int collisionEvent() { return getPlayer().collisionEvent; }
}
