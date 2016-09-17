package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.*
import li.cil.repack.org.luaj.vm2.lib.*
import net.torvald.terrarum.virtualcomputer.terminal.Teletype
import net.torvald.terrarum.virtualcomputer.terminal.Terminal

/**
 * APIs must have some extent of compatibility with ComputerCraft by dan200
 *
 * Created by minjaesong on 16-09-12.
 */
internal class Term(globals: Globals, term: Teletype) {

    init {
        // load things. WARNING: THIS IS MANUAL!
        globals["term"] = LuaValue.tableOf()
        globals["term"]["write"] = Term.WriteString(term)
        globals["term"]["print"] = Term.PrintString(term)
        globals["term"]["newLine"] = Term.NewLine(term)
        globals["term"]["moveCursor"] = Term.MoveCursor(term) // TTY function
        globals["term"]["width"] = Term.GetWidth(term)
        globals["term"]["scroll"] = Term.Scroll(term)
        globals["term"]["isTeletype"] = Term.IsTeletype(term)

        if (term is Terminal) {
            globals["term"]["emitRaw"] = Term.EmitRaw(term)
            globals["term"]["emit"] = Term.Emit(term)
            globals["term"]["resetColor"] = Term.ResetColour(term)
            globals["term"]["resetColour"] = Term.ResetColour(term)
            globals["term"]["clear"] = Term.Clear(term)
            globals["term"]["clearLine"] = Term.ClearLine(term)
            globals["term"]["moveCursor"] = Term.SetCursorPos(term)
            globals["term"]["getCursor"] = Term.GetCursorPos(term)
            globals["term"]["getX"] = Term.GetCursorX(term)
            globals["term"]["getY"] = Term.GetCursorY(term)
            globals["term"]["blink"] = Term.SetCursorBlink(term)
            globals["term"]["size"] = Term.GetSize(term)
            globals["term"]["isCol"] = Term.IsColour(term)
            globals["term"]["setForeCol"] = Term.SetForeColour(term)
            globals["term"]["setBackCol"] = Term.SetBackColour(term)
            globals["term"]["foreCol"] = Term.GetForeColour(term)
            globals["term"]["backCol"] = Term.GetBackColour(term)
        }
    }

    class WriteString(val tty: Teletype) : LuaFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.writeString(p0.checkjstring())
            else
                tty.writeChars(p0.checkjstring())
            return LuaValue.NONE
        }

        override fun call(s: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.writeString(s.checkjstring(), x.checkint(), y.checkint())
            else
                throw LuaError("couldn't move cursor; TTY is one-dimensional")
            return LuaValue.NONE
        }
    }

    class PrintString(val tty: Teletype) : LuaFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.printString(p0.checkjstring())
            else
                tty.printChars(p0.checkjstring())
            return LuaValue.NONE
        }

        override fun call(s: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.printString(s.checkjstring(), x.checkint(), y.checkint())
            else
                throw LuaError("couldn't move cursor; TTY is one-dimensional")
            return LuaValue.NONE
        }
    }

    class NewLine(val tty: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            tty.newLine()
            return LuaValue.NONE
        }
    }

    class EmitRaw(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.emitChar(p0.checkint())
            return LuaValue.NONE
        }
    }

    class Emit(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.emitChar(p0.checkint().toChar())
            return LuaValue.NONE
        }
    }

    class ResetColour(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            term.resetColour()
            return LuaValue.NONE
        }
    }

    class Clear(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            term.clear()
            return LuaValue.NONE
        }
    }

    class ClearLine(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            term.clearLine()
            return LuaValue.NONE
        }
    }

    /** term.setCursorPos(number x, number y) */
    class SetCursorPos(val term: Terminal) : TwoArgFunction() {
        override fun call(x: LuaValue, y: LuaValue): LuaValue {
            term.setCursor(x.checkint(), y.checkint())
            return LuaValue.NONE
        }
    }

    /** term.setCursorPos(number x) */
    class MoveCursor(val tty: Teletype) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            for (i in 1..p0.checkint())
                tty.printChar(' ')
            return LuaValue.NONE
        }
    }

    class GetCursorPos(val term: Terminal) : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            val ret = arrayOf(LuaValue.valueOf(term.cursorX), LuaValue.valueOf(term.cursorY))
            return LuaValue.varargsOf(ret)
        }
    }

    class GetCursorX(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.cursorX)
        }
    }

    class GetCursorY(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.cursorY)
        }
    }

    /** term.setCursorBlink(boolean bool) */
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

    class GetWidth(val tty: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(tty.width)
        }
    }

    class IsColour(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.coloursCount > 4)
        }
    }

    /** term.scroll(number n) */
    class Scroll(val tty: Teletype) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (tty is Terminal) tty.scroll(p0.checkint())
            else for (i in 1..p0.checkint()) tty.newLine()
            return LuaValue.NONE
        }
    }

    /** term.setTextColor(number color) */
    class SetForeColour(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.foreColour = p0.checkint()
            return LuaValue.NONE
        }
    }

    /** term.setBackgroundColor(number color) */
    class SetBackColour(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.backColour = p0.checkint()
            return LuaValue.NONE
        }
    }

    class GetForeColour(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.foreColour)
        }
    }

    class GetBackColour(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.backColour)
        }
    }

    class IsTeletype(val termInQuestion: Teletype) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(termInQuestion.coloursCount == 0)
        }
    }

}
