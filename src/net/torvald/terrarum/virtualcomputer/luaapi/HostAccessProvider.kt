package net.torvald.terrarum.virtualcomputer.luaapi

import net.torvald.terrarum.gameactors.ai.toLua
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
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
internal class HostAccessProvider(globals: Globals, computer: TerrarumComputer) {

    init {
        globals["machine"] = LuaTable()
        globals["machine"]["println"] = PrintLn()
        globals["machine"]["isHalted"] = IsHalted(computer)


        globals["machine"]["__readFromStdin"] = NativeReadStdin(computer)

        globals["machine"]["milliTime"] = NativeGetMilliTime(computer)

        globals["machine"]["sleep"] = NativeThreadSleep(computer)

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

    class IsHalted(val computer: TerrarumComputer): ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.isHalted)
        }
    }

    class NativeReadStdin(val computer: TerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return computer.stdin!!.read().toLua()
        }
    }

    class HaltComputer(val computer: TerrarumComputer) : ZeroArgFunction() {
        override fun call() : LuaValue {
            computer.isHalted = true
            computer.luaJ_globals.load("""print(DC4.."system halted")""").call()
            return LuaValue.NONE
        }
    }

    /** Time elapsed since the power is on. */
    class NativeGetMilliTime(val computer: TerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.milliTime)
        }
    }

    class NativeThreadSleep(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(mills: LuaValue): LuaValue {
            computer.currentExecutionThread.join(mills.checklong())
            return LuaValue.NONE
        }
    }
}