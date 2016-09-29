package net.torvald.terrarum.virtualcomputer.peripheral

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Created by minjaesong on 16-09-29.
 */
open class Peripheral(val luaG: Globals, val tableName: String) {

    open fun loadLib() {
        luaG[tableName] = LuaTable()
    }
    open fun unloadLib() {
        luaG[tableName] = LuaValue.NIL
    }

    override fun toString(): String = tableName
}