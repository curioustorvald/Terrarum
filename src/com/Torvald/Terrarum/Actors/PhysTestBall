package com.Torvald.Terrarum.Actors;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Created by minjaesong on 16-03-05.
 */
public class PhysTestBall extends ActorWithBody {

    public PhysTestBall() {
        super();
        setHitboxDimension(16, 16, 0, 0);
        setVisible(true);
        setMass(10f);
    }

    @Override
    public void drawBody(GameContainer gc, Graphics g) {
        g.setColor(Color.orange);
        g.fillOval(
                getHitbox().getPosX()
                , getHitbox().getPosY()
                , getHitbox().getWidth()
                , getHitbox().getHeight()
        );
    }
}
