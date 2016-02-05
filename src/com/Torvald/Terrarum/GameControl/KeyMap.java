package com.Torvald.Terrarum.GameControl;

import java.util.Hashtable;

/**
 * Created by minjaesong on 15-12-31.
 */
public class KeyMap {

    public static Hashtable<EnumKeyFunc, Integer> map_code = new Hashtable<>();

    public static void build(){

        map_code.put(EnumKeyFunc.MOVE_UP, Key.E);
        map_code.put(EnumKeyFunc.MOVE_LEFT, Key.S);
        map_code.put(EnumKeyFunc.MOVE_DOWN, Key.D);
        map_code.put(EnumKeyFunc.MOVE_RIGHT, Key.F);
        map_code.put(EnumKeyFunc.JUMP, Key.SPACE);
        map_code.put(EnumKeyFunc.UI_CONSOLE, Key.GRAVE);
        map_code.put(EnumKeyFunc.UI_BASIC_INFO, Key.F3);
    }

    public static int getKeyCode(EnumKeyFunc fn) {
        return map_code.get(fn);
    }

    public static void set(EnumKeyFunc func, int key){
        map_code.put(func, key);
    }

}
