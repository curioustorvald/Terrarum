package net.torvald.terrarum.gamecontroller

import net.torvald.terrarum.Terrarum
import java.util.Hashtable

/**
 * Created by minjaesong on 15-12-31.
 */
object KeyMap {

    var map_code = Hashtable<EnumKeyFunc, Int>()

    init {
        map_code.put(EnumKeyFunc.MOVE_UP, Terrarum.getConfigInt("keyup"))
        map_code.put(EnumKeyFunc.MOVE_LEFT, Terrarum.getConfigInt("keyleft"))
        map_code.put(EnumKeyFunc.MOVE_DOWN, Terrarum.getConfigInt("keydown"))
        map_code.put(EnumKeyFunc.MOVE_RIGHT, Terrarum.getConfigInt("keyright"))
        map_code.put(EnumKeyFunc.JUMP, Terrarum.getConfigInt("keyjump"))
        map_code.put(EnumKeyFunc.UI_CONSOLE, Key.GRAVE)
        map_code.put(EnumKeyFunc.UI_BASIC_INFO, Key.F3)
    }

    fun getKeyCode(fn: EnumKeyFunc): Int {
        return map_code[fn]!!
    }

    fun set(func: EnumKeyFunc, key: Int) {
        map_code.put(func, key)
    }

}
