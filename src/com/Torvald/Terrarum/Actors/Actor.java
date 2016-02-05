package com.Torvald.Terrarum.Actors;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Created by minjaesong on 15-12-31.
 */
public interface Actor {

    void update(GameContainer gc, int delta_t);

    long getRefID();
}
