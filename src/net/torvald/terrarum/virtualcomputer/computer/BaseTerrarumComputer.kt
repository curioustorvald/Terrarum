package net.torvald.terrarum.virtualcomputer.computer

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaError
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform
import net.torvald.terrarum.virtualcomputer.lualib.TermLib
import net.torvald.terrarum.virtualcomputer.terminal.SimpleTextTerminal
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import net.torvald.terrarum.virtualcomputer.terminal.TerminalInputStream
import net.torvald.terrarum.virtualcomputer.terminal.TerminalPrintStream
import org.newdawn.slick.GameContainer
import java.io.*

/**
 * A part that makes "computer fixtures" actually work
 *
 * @param term : terminal that is connected to the computer fixtures, null if not connected any.
 *
 * Created by minjaesong on 16-09-10.
 */
class BaseTerrarumComputer(term: Terminal?) {

    val luaJ_globals: Globals = JsePlatform.standardGlobals()

    var termOut: PrintStream? = null
        private set
    var termErr: PrintStream? = null
        private set
    var termIn: InputStream? = null
        private set

    init {
        if (term != null) {
            termOut = TerminalPrintStream(term)
            termErr = TerminalPrintStream(term)
            termIn = TerminalInputStream(term)

            luaJ_globals.STDOUT = termOut
            luaJ_globals.STDERR = termErr
            luaJ_globals.STDIN = termIn

            loadTermLib(term)
        }

        // ROM BASIC
        val inputStream = javaClass.getResourceAsStream("/net/torvald/terrarum/virtualcomputer/assets/lua/ROMBASIC.lua")
        runCommand(InputStreamReader(inputStream), "rombasic")
    }

    var threadTimer = 0
    val threadMaxTime = 2000

    fun update(gc: GameContainer, delta: Int) {
        if (currentExecutionThread.state == Thread.State.TERMINATED)
            unsetThreadRun()

        // time the execution time of the thread
        if (threadRun) {
            threadTimer += delta

            // if too long, halt
            if (threadTimer > threadMaxTime) {
                //luaJ_globals.STDERR.println("Interrupted: Too long without yielding.")
                //currentExecutionThread.interrupt()
                unsetThreadRun()
            }
        }
    }

    var currentExecutionThread = Thread()
    var threadRun = false

    fun runCommand(line: String, env: String) {
        if (!threadRun) {
            currentExecutionThread = Thread(ThreadRunCommand(luaJ_globals, line, env))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    fun runCommand(reader: Reader, filename: String) {
        if (!threadRun) {
            currentExecutionThread = Thread(ThreadRunCommand(luaJ_globals, reader, filename))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    private fun unsetThreadRun() {
        threadRun = false
        threadTimer = 0
    }

    class ThreadRunCommand : Runnable {

        val mode: Int
        val arg1: Any
        val arg2: String
        val lua: Globals

        constructor(luaInstance: Globals, line: String, env: String) {
            mode = 0
            arg1 = line
            arg2 = env
            lua = luaInstance
        }

        constructor(luaInstance: Globals, reader: Reader, filename: String) {
            mode = 1
            arg1 = reader
            arg2 = filename
            lua = luaInstance
        }

        override fun run() {
            try {
                val chunk: LuaValue
                if (mode == 0)
                    chunk = lua.load(arg1 as String, arg2)
                else if (mode == 1)
                    chunk = lua.load(arg1 as Reader, arg2)
                else
                    throw IllegalArgumentException("Unsupported mode: $mode")

                chunk.call()
            }
            catch (e: LuaError) {
                lua.STDERR.println("${SimpleTextTerminal.ASCII_DC2}${e.message}${SimpleTextTerminal.ASCII_DC4}")
                if (DEBUGTHRE) e.printStackTrace(System.err)
            }

            lua.load("_COMPUTER.prompt()").call()
        }

        val DEBUGTHRE = true
    }

    /////////////////////////
    // MANUAU LIBRARY LOAD //
    /////////////////////////
    private fun loadTermLib(term: Terminal) {
        luaJ_globals["term"] = LuaValue.tableOf()
        luaJ_globals["term"]["test"] = TermLib.Test(term)
        luaJ_globals["term"]["setCursorPos"] = TermLib.MoveCursor(term)
        luaJ_globals["term"]["setcursorpos"] = TermLib.MoveCursor(term)
        luaJ_globals["term"]["gotoxy"]       = TermLib.MoveCursor(term) // pascal-style alias
        luaJ_globals["term"]["getCursorPos"] = TermLib.GetCursorPos(term)
        luaJ_globals["term"]["getcursorpos"] = TermLib.GetCursorPos(term)
        luaJ_globals["term"]["setCursorBlink"] = TermLib.SetCursorBlink(term)
        luaJ_globals["term"]["setcursorblink"] = TermLib.SetCursorBlink(term)
        luaJ_globals["term"]["getSize"] = TermLib.GetSize(term)
        luaJ_globals["term"]["getsize"] = TermLib.GetSize(term)
    }
}