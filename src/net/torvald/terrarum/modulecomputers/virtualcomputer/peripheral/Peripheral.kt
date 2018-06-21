package net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Created by minjaesong on 2016-09-29.
 */
abstract class Peripheral(val tableName: String) {

    abstract fun loadLib(globals: Globals)

    override fun toString(): String = "Peripheral:$tableName"

    abstract val memSize: Int
}