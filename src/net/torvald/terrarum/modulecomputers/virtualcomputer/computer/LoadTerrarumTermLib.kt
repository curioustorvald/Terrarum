package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import net.torvald.terrarum.gameactors.ai.luaTableOf
import net.torvald.terrarum.gameactors.ai.toLua
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue

/**
 * Created by minjaesong on 2019-07-10.
 */
object LoadTerrarumTermLib {

    operator fun invoke(globals: Globals, terminal: MDA) {
        globals.addZeroArgFun("term.getCursor") {
            luaTableOf(
                    (terminal.cursor % terminal.width).toLua(),
                    (terminal.cursor / terminal.height).toLua()
            )
        }
        globals.addTwoArgFun("term.setCursor") { p0, p1 ->
            terminal.setCursor(p0.checkint(), p1.checkint())
            LuaValue.NIL
        }
        globals.addZeroArgFun("term.getCursorBlink") {
            terminal.blink.toLua()
        }
        globals.addOneArgFun("term.setCursorBlink") { p0 ->
            terminal.blink = p0.checkboolean()
            LuaValue.NIL
        }
        globals.addZeroArgFun("term.getTextColor") {
            terminal.foreground.toLua()
        }
        globals.addZeroArgFun("term.getBackgroundColor") {
            terminal.background.toLua()
        }
        globals.addOneArgFun("term.setTextColor") { p0 ->
            terminal.foreground = p0.checkint()
            LuaValue.NIL
        }
        globals.addOneArgFun("term.setBackgroundColor") { p0 ->
            terminal.background = p0.checkint()
            LuaValue.NIL
        }
        globals.addZeroArgFun("term.getSize") {
            luaTableOf(
                    (terminal.width).toLua(),
                    (terminal.height).toLua()
            )
        }
        globals.addZeroArgFun("term.clear") {
            terminal.clear()
            LuaValue.NIL
        }
        globals.addZeroArgFun("term.clearLine") {
            terminal.clearCurrentLine()
            LuaValue.NIL
        }
        globals.addOneArgFun("term.scroll") { p0 ->
            if (p0.checkint() < 0)
                throw LuaError("Scroll amount must be a positive number")
            terminal.scroll(p0.toint())
            LuaValue.NIL
        }
        globals.addOneArgFun("term.write") { p0 ->
            terminal.print(p0.checkjstring())
            LuaValue.NIL
        }
        globals.addThreeArgFun("term.setText") { p0, p1, p2 ->
            terminal.setOneText(p0.checkint(), p1.checkint(), p2.checkint().toByte())
            LuaValue.NIL
        }
    }

}