package net.torvald.terrarum.virtualcomputer.lualib

import li.cil.repack.org.luaj.vm2.*
import li.cil.repack.org.luaj.vm2.lib.*
import net.torvald.terrarum.virtualcomputer.terminal.Terminal

/**
 * APIs must have some extent of compatibility with ComputerCraft by dan200
 *
 * Created by minjaesong on 16-09-12.
 */
internal class TermLib(val vt: Terminal) : ZeroArgFunction() {
    var INSTANCE: TermLib? = null

    init {
        if (INSTANCE == null) INSTANCE = this
    }

    override fun call(): LuaValue {
        throw UnsupportedOperationException("""Invalid usage!
usage:
    luaJ_globals["term"] = LuaValue.tableOf()
    luaJ_globals["term"]["test"] = TermLib.Test(term)
    ...
""")
    }

    class Test(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaDouble.valueOf("TermTest")
        }
    }

    class MoveCursor(val term: Terminal) : TwoArgFunction() {
        override fun call(x: LuaValue, y: LuaValue): LuaValue {
            term.setCursor(x.checkint(), y.checkint())
            return LuaValue.NONE
        }
    }

    class GetCursorPos(val term: Terminal) : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            val ret = arrayOf(LuaValue.valueOf(term.cursorX), LuaValue.valueOf(term.cursorY))
            return LuaValue.varargsOf(ret)
        }
    }

    class SetCursorBlink(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.cursorBlink = p0.toboolean()
            return LuaValue.NONE
        }
    }

    class GetSize(val term: Terminal) : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            val ret = arrayOf(LuaValue.valueOf(term.width), LuaValue.valueOf(term.height))
            return LuaValue.varargsOf(ret)
        }
    }

    class IsColor(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

}
