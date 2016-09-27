package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaTable
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.TwoArgFunction
import li.cil.repack.org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer

/**
 * PC Speaker driver and arpeggiator (MONOTONE-style 4 channels)
 *
 * Created by minjaesong on 16-09-27.
 */
class PcSpeakerDriver(globals: Globals, host: BaseTerrarumComputer) {

    init {
        globals["speaker"] = LuaTable()
        globals["speaker"]["enqueue"] = EnqueueTone(host)
        globals["speaker"]["clear"] = ClearQueue(host)
    }

    class EnqueueTone(val host: BaseTerrarumComputer) : TwoArgFunction() {
        override fun call(millisec: LuaValue, freq: LuaValue): LuaValue {
            host.enqueueBeep(millisec.checkint(), freq.tofloat())
            return LuaValue.NONE
        }
    }

    class ClearQueue(val host: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            host.clearBeepQueue()
            return LuaValue.NONE
        }
    }

}