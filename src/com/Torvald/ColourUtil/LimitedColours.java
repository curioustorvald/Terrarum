package com.Torvald.ColourUtil;

import org.newdawn.slick.Color;

/**
 * Created by minjaesong on 16-02-11.
 */
public interface LimitedColours {



    Color createSlickColor(int raw);
    Color createSlickColor(int r, int g, int b);

    void create(int raw);
    void create(int r, int g, int b);

}
