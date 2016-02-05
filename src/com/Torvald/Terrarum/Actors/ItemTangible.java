package com.Torvald.Terrarum.Actors;

import com.Torvald.spriteAnimation.SpriteAnimation;
import com.sun.istack.internal.NotNull;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Created by minjaesong on 16-01-03.
 */
public class ItemTangible extends ActorWithBody {

    @NotNull private float mass;

    public ItemTangible() {
        super.setVisible(true);
    }

    @Override
    public void update(GameContainer gc, int delta_t) {

    }

    @Override
    public void drawBody(GameContainer gc, Graphics g) {
        super.drawBody(gc, g);
    }
}
