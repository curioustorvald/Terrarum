package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaFunction
import li.cil.repack.org.luaj.vm2.LuaTable
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.OneArgFunction
import li.cil.repack.org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.luaapi.Term.Companion.checkIBM437
import net.torvald.terrarum.virtualcomputer.terminal.Teletype

/**
 * Provide Lua an access to computer object that is in Java
 *
 * Created by minjaesong on 16-09-19.
 */
internal class HostAccessProvider(globals: Globals, computer: BaseTerrarumComputer) {

    init {
        globals["native"] = LuaTable()
        globals["native"]["println"] = PrintLn()
        globals["native"]["isHalted"] = IsHalted(computer)

        globals["native"]["closeInputString"] = NativeCloseInputString(computer.term!!)
        globals["native"]["closeInputKey"] = NativeCloseInputKey(computer.term!!)
        globals["native"]["openInput"] = NativeOpenInput(computer.term!!)
        globals["native"]["getLastStreamInput"] = NativeGetLastStreamInput(computer.term!!)
        globals["native"]["getLastKeyPress"] = NativeGetLastKeyPress(computer.term!!)

        // while lua's dofile/require is fixiated to fs, this command allows
        // libraries in JAR to be loaded.
        //globals["native"]["loadBuiltInLib"] = NativeLoadBuiltInLib()

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
}