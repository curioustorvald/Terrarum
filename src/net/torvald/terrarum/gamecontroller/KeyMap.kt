package net.torvald.terrarum.gamecontroller

import java.util.Hashtable

/**
 * Created by minjaesong on 15-12-31.
 */
object KeyMap {

    var map_code = Hashtable<EnumKeyFunc, Int>()

    fun build() {

        map_code.put(EnumKeyFunc.MOVE_UP, Key.E)
        map_code.put(EnumKeyFunc.MOVE_LEFT, Key.S)
        map_code.put(EnumKeyFunc.MOVE_DOWN, Key.D)
        map_code.put(EnumKeyFunc.MOVE_RIGHT, Key.F)
        map_code.put(EnumKeyFunc.JUMP, Key.SPACE)
        map_code.put(EnumKeyFunc.UI_CONSOLE, Key.GRAVE)
        map_code.put(EnumKeyFunc.UI_BASIC_INFO, Key.F3)
    }

    fun getKeyCode(fn: EnumKeyFunc): Int {
        return map_code[fn]!!
    }

    operator fun set(func: EnumKeyFunc, key: Int) {
        map_code.put(func, key)
    }

}
