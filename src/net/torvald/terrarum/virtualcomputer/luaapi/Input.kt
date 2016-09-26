package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaTable
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.OneArgFunction
import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer

/**
 * Created by minjaesong on 16-09-25.
 */
class Input(globals: Globals, computer: BaseTerrarumComputer) {

    init {
        globals["input"] = LuaTable()
        globals["input"]["isKeyDown"] = IsKeyDown(computer)
    }

    companion object {
        val keys_alt = intArrayOf(Key.L_ALT, Key.L_COMMAND)
        val keys_caps = intArrayOf(Key.CAPS_LOCK, Key.BACKSPACE, Key.L_CONTROL)
    }

    class IsKeyDown(val computer: BaseTerrarumComputer) : OneArgFunction() {
        override fun call(keyCode: LuaValue): LuaValue {
            val key = keyCode.checkint()

            // L_Alt and L_COMMAND are homogeneous
            if (keys_alt.contains(key)) {
                for (k in keys_alt) {
                    val down = computer.input.isKeyDown(k)
                    if (down) return LuaValue.valueOf(true)
                }
            }

            // Caps, Backspace, L_Control, for Colemak and HHKB
            if (keys_caps.contains(key)) {
                for (k in keys_caps) {
                    val down = computer.input.isKeyDown(k)
                    if (down) return LuaValue.valueOf(true)
                }
            }

            return LuaValue.valueOf(computer.input.isKeyDown(keyCode.checkint()))
        }
    }
}