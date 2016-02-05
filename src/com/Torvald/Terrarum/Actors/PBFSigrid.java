package com.Torvald.Terrarum.Actors;

import com.Torvald.Terrarum.Game;
import com.Torvald.spriteAnimation.SpriteAnimation;
import org.newdawn.slick.SlickException;

/**
 * Created by minjaesong on 16-02-03.
 */
public class PBFSigrid {

    public Player build() throws SlickException {
        Player p = new Player();

        p.referenceID = Game.PLAYER_REF_ID;

        p.setVisible(true);

        p.sprite = new SpriteAnimation();
        p.sprite.setDimension(28, 50);
        p.sprite.setSpriteImage("res/graphics/sprites/test_player.png");
        p.sprite.setDelay(200);
        p.sprite.setRowsAndFrames(1, 1);
        p.sprite.setAsVisible();
        p.sprite.composeSprite();

        p.spriteGlow = new SpriteAnimation();
        p.spriteGlow.setDimension(28, 50);
        p.spriteGlow.setSpriteImage("res/graphics/sprites/test_player_glow.png");
        p.spriteGlow.setDelay(200);
        p.spriteGlow.setRowsAndFrames(1, 1);
        p.spriteGlow.setAsVisible();
        p.spriteGlow.composeSprite();


        p.actorValue = new ActorValue();
        p.actorValue.set("scale", 1.0f);
        p.actorValue.set("speed", 3.0f);
        p.actorValue.set("speedmult", 1.0f);
        p.actorValue.set("accel", p.WALK_ACCEL_BASE);
        p.actorValue.set("accelmult", 1.0f);

        p.actorValue.set("jumppower", 6f);
        // in frames
        p.actorValue.set("jumplength", 30f);

        p.actorValue.set("basemass", 80f);

        p.actorValue.set("physiquemult", 1); // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue.set("strength", 1250);
        p.actorValue.set("encumbrance", 1000);

        p.setHitboxDimension(20, 47, 7, 0);

        p.inventory = new ActorInventory((int) p.actorValue.get("encumbrance"), true);

        return p;
    }

}
