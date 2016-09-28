package net.torvald.terrarum.virtualcomputer.luaapi

import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.luaapi.Term.Companion.checkIBM437
import net.torvald.terrarum.virtualcomputer.terminal.Teletype
import org.luaj.vm2.*

/**
 * Provide Lua an access to computer object that is in Java
 *
 * The "machine" refers to the computer fixture itself in the game world.
 *
 * Created by minjaesong on 16-09-19.
 */
internal class HostAccessProvider(globals: Globals, computer: BaseTerrarumComputer) {

    init {
        globals["machine"] = LuaTable()
        globals["machine"]["println"] = PrintLn()
        globals["machine"]["isHalted"] = IsHalted(computer)

        globals["machine"]["closeInputString"] = NativeCloseInputString(computer.term)
        globals["machine"]["closeInputKey"] = NativeCloseInputKey(computer.term)
        globals["machine"]["openInput"] = NativeOpenInput(computer.term)
        globals["machine"]["getLastStreamInput"] = NativeGetLastStreamInput(computer.term)
        globals["machine"]["getLastKeyPress"] = NativeGetLastKeyPress(computer.term)

        globals["machine"]["milliTime"] = NativeGetMilliTime(computer)

        globals["machine"]["sleep"] = NativeBusySleep(computer)

        globals["__haltsystemexplicit__"] = HaltComputer(computer)
    }

    class PrintLn(): OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (p0.isnumber())
                println(p0.checkdouble())
            else
                println(p0.checkIBM437())
            return LuaValue.NONE
        }
    }

    class IsHalted(val computer: BaseTerrarumComputer): ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.isHalted)
        }
    }

    class NativeCloseInputString(val term: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            term.closeInputString()
            return LuaValue.NONE
        }
    }

    class NativeCloseInputKey(val term: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            //term.closeInputKey()
            return LuaValue.NONE
        }
    }

    class NativeOpenInput(val term: Teletype) : LuaFunction() {
        override fun call(): LuaValue {
            term.openInput(true)
            return LuaValue.NONE
        }

        override fun call(echo: LuaValue): LuaValue {
            term.openInput(if (echo.checkint() == 1) false else true)
            return LuaValue.NONE
        }
    }

    class NativeGetLastStreamInput(val term: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (term.lastStreamInput == null) LuaValue.NIL
            else                                     LuaValue.valueOf(term.lastStreamInput)
        }
    }

    class NativeGetLastKeyPress(val term: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (term.lastKeyPress == null) LuaValue.NIL
            else                                  LuaValue.valueOf(term.lastKeyPress!!)
        }
    }

    class HaltComputer(val computer: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call() : LuaValue {
            computer.isHalted = true
            computer.luaJ_globals.load("""print(DC4.."system halted")""").call()
            return LuaValue.NONE
        }
    }

    /** Time elapsed since the power is on. */
    class NativeGetMilliTime(val computer: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.milliTime)
        }
    }

    class NativeBusySleep(val computer: BaseTerrarumComputer) : OneArgFunction() {
        override fun call(mills: LuaValue): LuaValue {
            val starttime = computer.milliTime
            val sleeptime = mills.checkint()
            if (sleeptime > 1000) throw LuaError("Cannot busy-sleep more than a second.")
            while (computer.milliTime - starttime < sleeptime) { }
            return LuaValue.NONE
        }
    }
}