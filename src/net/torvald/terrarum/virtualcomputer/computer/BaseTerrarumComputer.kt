package net.torvald.terrarum.virtualcomputer.computer

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaError
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.ZeroArgFunction
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.virtualcomputer.luaapi.Filesystem
import net.torvald.terrarum.virtualcomputer.luaapi.HostAccessProvider
import net.torvald.terrarum.virtualcomputer.luaapi.Security
import net.torvald.terrarum.virtualcomputer.luaapi.Term
import net.torvald.terrarum.virtualcomputer.terminal.*
import net.torvald.terrarum.virtualcomputer.worldobject.ComputerPartsCodex
import net.torvald.terrarum.virtualcomputer.worldobject.FixtureComputerBase
import org.newdawn.slick.GameContainer
import java.io.*

/**
 * A part that makes "computer fixture" actually work
 *
 * @param avFixtureComputer : actor values for FixtureComputerBase
 *
 * @param term : terminal that is connected to the computer fixtures, null if not connected any.
 * Created by minjaesong on 16-09-10.
 */
class BaseTerrarumComputer(val term: Teletype? = null) {

    val DEBUG_UNLIMITED_MEM = false

    val luaJ_globals: Globals = JsePlatform.debugGlobals()

    var termOut: PrintStream? = null
        private set
    var termErr: PrintStream? = null
        private set
    var termIn: InputStream? = null
        private set

    val processorCycle: Int // number of Lua statement to process per tick (1/100 s)
        get() = ComputerPartsCodex.getProcessorCycles(computerValue.getAsInt("processor") ?: 0)
    val memSize: Int // max: 8 GB
        get() {
            if (DEBUG_UNLIMITED_MEM) return 1.shl(30)// 1 GB

            var size = 0
            for (i in 0..3)
                size += ComputerPartsCodex.getRamSize(computerValue.getAsInt("memSlot$i") ?: 0)

            return 16.shl(20)
            return size
        }

    val UUID = java.util.UUID.randomUUID().toString()

    val computerValue = KVHashMap()

    var isHalted = false

    init {
        computerValue["memslot0"] = 4864 // -1 indicates mem slot is empty
        computerValue["memslot1"] = -1 // put index of item here
        computerValue["memslot2"] = -1 // ditto.
        computerValue["memslot3"] = -1 // do.

        computerValue["processor"] = -1 // do.

        // as in "dev/hda"; refers hard disk drive (and no partitioning)
        computerValue["hda"] = "testhda" // 'UUID rendered as String' or "none"
        computerValue["hdb"] = "none"
        computerValue["hdc"] = "none"
        computerValue["hdd"] = "none"
        // as in "dev/fd1"; refers floppy disk drive
        computerValue["fd1"] = "none"
        computerValue["fd2"] = "none"
        computerValue["fd3"] = "none"
        computerValue["fd4"] = "none"
        // SCSI connected optical drive
        computerValue["sda"] = "none"

        // boot device
        computerValue["boot"] = computerValue.getAsString("hda")!!
        
        
        if (term != null) {
            termOut = TerminalPrintStream(term)
            termErr = TerminalPrintStream(term)
            termIn = TerminalInputStream(term)

            luaJ_globals.STDOUT = termOut
            luaJ_globals.STDERR = termErr
            luaJ_globals.STDIN = termIn

            // load libraries
            Term(luaJ_globals, term)
            Security(luaJ_globals)
            Filesystem(luaJ_globals, this)
            HostAccessProvider(luaJ_globals, this)
        }

        // ROM BASIC
        val inputStream = javaClass.getResourceAsStream("/net/torvald/terrarum/virtualcomputer/assets/lua/BOOT.lua")
        runCommand(InputStreamReader(inputStream), "=boot")

        // computer-related global functions
        luaJ_globals["getTotalMem"] = LuaFunGetTotalMem(this)
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

    fun keyPressed(key: Int, c: Char) {

    }

    var currentExecutionThread = Thread()
    var threadRun = false

    fun runCommand(line: String, env: String) {
        if (!threadRun && !isHalted) {
            currentExecutionThread = Thread(ThreadRunCommand(luaJ_globals, line, env))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    fun runCommand(reader: Reader, filename: String) {
        if (!threadRun && !isHalted) {
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
                lua.STDERR.println("${SimpleTextTerminal.ASCII_DLE}${e.message}${SimpleTextTerminal.ASCII_DC4}")
                if (DEBUGTHRE) e.printStackTrace(System.err)
            }
        }

        val DEBUGTHRE = true
    }

    class LuaFunGetTotalMem(val computer: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.memSize)
        }
    }
}