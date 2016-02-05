package com.Torvald.Terrarum.Actors;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Created by minjaesong on 16-01-25.
 */
public interface Visible {

    void drawBody(GameContainer gc, Graphics g);

    void updateBodySprite(GameContainer gc, int delta_t);

}
