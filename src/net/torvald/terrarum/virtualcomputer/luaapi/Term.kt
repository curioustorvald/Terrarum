package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.*
import li.cil.repack.org.luaj.vm2.lib.*
import net.torvald.terrarum.virtualcomputer.terminal.Teletype
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import java.nio.charset.Charset

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
            globals["term"]["emitString"] = Term.EmitString(term)
            globals["term"]["resetColor"] = Term.ResetColour(term)
            globals["term"]["resetColour"] = Term.ResetColour(term)
            globals["term"]["clear"] = Term.Clear(term)
            globals["term"]["clearLine"] = Term.ClearLine(term)
            globals["term"]["setCursor"] = Term.SetCursor(term)
            globals["term"]["getCursor"] = Term.GetCursorPos(term)
            globals["term"]["getX"] = Term.GetCursorX(term)
            globals["term"]["getY"] = Term.GetCursorY(term)
            globals["term"]["setX"] = Term.SetCursorX(term)
            globals["term"]["setY"] = Term.SetCursorY(term)
            globals["term"]["setCursorBlink"] = Term.SetCursorBlink(term)
            globals["term"]["size"] = Term.GetSize(term)
            globals["term"]["height"] = Term.GetHeight(term)
            globals["term"]["isCol"] = Term.IsColour(term)
            globals["term"]["setForeCol"] = Term.SetForeColour(term)
            globals["term"]["setBackCol"] = Term.SetBackColour(term)
            globals["term"]["foreCol"] = Term.GetForeColour(term)
            globals["term"]["backCol"] = Term.GetBackColour(term)
        }
    }

    companion object {
        fun LuaValue.checkIBM437(): String {
            if (this is LuaString)
                return m_bytes.copyOfRange(m_offset, m_length).toString(Charset.forName("ISO-8859-1"))
                // it only works if Charset is ISO-8859, despite of the name "IBM437"
            else
                throw LuaError("bad argument (string expected, got ${this.typename()})")
        }
    }
    
    class WriteString(val tty: Teletype) : LuaFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.writeString(p0.checkIBM437())
            else
                tty.writeChars(p0.checkIBM437())
            return LuaValue.NONE
        }

        override fun call(s: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.writeString(s.checkIBM437(), x.checkint(), y.checkint())
            else
                throw LuaError("couldn't move cursor; TTY is one-dimensional")
            return LuaValue.NONE
        }
    }

    class PrintString(val tty: Teletype) : LuaFunction() {
        override fun call(p0: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.printString(p0.checkIBM437())
            else
                tty.printChars(p0.checkIBM437())
            return LuaValue.NONE
        }

        override fun call(s: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            if (tty is Terminal)
                tty.printString(s.checkIBM437(), x.checkint(), y.checkint())
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

    class EmitRaw(val term: Terminal) : ThreeArgFunction() {
        override fun call(p0: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            term.emitChar(p0.checkint(), x.checkint() - 1, y.checkint() - 1)
            return LuaValue.NONE
        }
    }

    class Emit(val term: Terminal) : ThreeArgFunction() {
        override fun call(p0: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            term.emitChar(p0.checkint().toChar(), x.checkint() - 1, y.checkint() - 1)
            return LuaValue.NONE
        }
    }

    class EmitString(val term: Terminal) : ThreeArgFunction() {
        override fun call(p0: LuaValue, x: LuaValue, y: LuaValue): LuaValue {
            term.emitString(p0.checkIBM437(), x.checkint() - 1, y.checkint() - 1)
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

    /** term.setCursorPos(number x) */
    class MoveCursor(val tty: Teletype) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            for (i in 1..p0.checkint())
                tty.printChar(' ')
            return LuaValue.NONE
        }
    }

    class SetCursor(val term: Terminal) : TwoArgFunction() {
        override fun call(x: LuaValue, y: LuaValue): LuaValue {
            term.setCursor(x.checkint() - 1, y.checkint() - 1)
            return LuaValue.NONE
        }
    }

    /** One-based */
    class GetCursorPos(val term: Terminal) : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            val ret = arrayOf(LuaValue.valueOf(term.cursorX + 1), LuaValue.valueOf(term.cursorY + 1))
            return LuaValue.varargsOf(ret)
        }
    }

    class GetCursorX(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.cursorX + 1)
        }
    }

    class GetCursorY(val term: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(term.cursorY + 1)
        }
    }

    class SetCursorX(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.setCursor(p0.checkint() - 1, term.cursorY)
            return LuaValue.NONE
        }
    }

    class SetCursorY(val term: Terminal) : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            term.setCursor(term.cursorX - 1, p0.checkint())
            return LuaValue.NONE
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

    class GetHeight(val terminal: Terminal) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(terminal.height)
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
