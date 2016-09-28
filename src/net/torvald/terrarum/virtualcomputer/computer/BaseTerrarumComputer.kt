package net.torvald.terrarum.virtualcomputer.computer

import com.jme3.math.FastMath
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.virtualcomputer.luaapi.*
import net.torvald.terrarum.virtualcomputer.terminal.*
import net.torvald.terrarum.virtualcomputer.worldobject.ComputerPartsCodex
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import java.io.*
import java.nio.ByteBuffer
import java.util.*

/**
 * A part that makes "computer fixture" actually work
 *
 * @param avFixtureComputer : actor values for FixtureComputerBase
 *
 * @param term : terminal that is connected to the computer fixtures, null if not connected any.
 * Created by minjaesong on 16-09-10.
 */
class BaseTerrarumComputer() {

    val DEBUG_UNLIMITED_MEM = false
    val DEBUG = false


    lateinit var luaJ_globals: Globals
        private set

    var termOut: PrintStream? = null
        private set
    var termErr: PrintStream? = null
        private set
    var termIn: InputStream? = null
        private set

    val processorCycle: Int // number of Lua statement to process per tick (1/100 s)
        get() = ComputerPartsCodex.getProcessorCycles(computerValue.getAsInt("processor") ?: 0)
    val memSize: Int // in bytes; max: 8 GB
        get() {
            if (DEBUG_UNLIMITED_MEM) return 16.shl(20)// 16 MB

            var size = 0
            for (i in 0..3)
                size += ComputerPartsCodex.getRamSize(computerValue.getAsInt("memSlot$i")!!)

            return size
        }

    val UUID = java.util.UUID.randomUUID().toString()

    val computerValue = KVHashMap()

    var isHalted = false

    lateinit var input: Input
        private set

    lateinit var term: Teletype
        private set

    // os-related functions. These are called "machine" library-wise.
    private val startupTimestamp: Long = System.currentTimeMillis()
    /** Time elapsed since the power is on. */
    val milliTime: Int
        get() = (System.currentTimeMillis() - startupTimestamp).toInt()

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
    }

    fun attachTerminal(term: Teletype) {
        this.term = term
        initSandbox(term)
    }

    fun initSandbox(term: Teletype) {
        luaJ_globals = JsePlatform.debugGlobals()

        termOut = TerminalPrintStream(term)
        termErr = TerminalPrintStream(term)
        termIn = TerminalInputStream(term)

        luaJ_globals.STDOUT = termOut
        luaJ_globals.STDERR = termErr
        luaJ_globals.STDIN = termIn

        luaJ_globals["bit"] = luaJ_globals["bit32"]

        // load libraries
        Term(luaJ_globals, term)
        Security(luaJ_globals)
        Filesystem(luaJ_globals, this)
        HostAccessProvider(luaJ_globals, this)
        Input(luaJ_globals, this)
        Http(luaJ_globals, this)
        PcSpeakerDriver(luaJ_globals, this)
        WorldInformationProvider(luaJ_globals)


        // secure the sandbox
        luaJ_globals["io"] = LuaValue.NIL
        // dubug should be sandboxed in BOOT.lua (use OpenComputers code)
        //val sethook = luaJ_globals["debug"]["sethook"]
        //luaJ_globals["debug"] = LuaValue.NIL

        // ROM BASIC
        val inputStream = javaClass.getResourceAsStream("/net/torvald/terrarum/virtualcomputer/assets/lua/BOOT.lua")
        runCommand(InputStreamReader(inputStream), "=boot")

        // computer-related global functions
        luaJ_globals["totalMemory"] = LuaFunGetTotalMem(this)

        luaJ_globals["computer"] = LuaTable()
        // rest of the "computer" APIs should be implemented in BOOT.lua
        if (DEBUG) luaJ_globals["emittone"] = ComputerEmitTone(this)
    }

    var threadTimer = 0
    val threadMaxTime = 2000

    fun update(gc: GameContainer, delta: Int) {
        input = gc.input


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

        driveBeepQueueManager(delta)
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

        val DEBUGTHRE = true

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
    }

    class LuaFunGetTotalMem(val computer: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.memSize)
        }
    }

    class ComputerEmitTone(val computer: BaseTerrarumComputer) : TwoArgFunction() {
        override fun call(millisec: LuaValue, freq: LuaValue): LuaValue {
            computer.playTone(millisec.toint(), freq.tofloat())
            return LuaValue.NONE
        }
    }

    ///////////////////
    // BEEPER DRIVER //
    ///////////////////

    private val beepMaxLen = 10000
    // let's regard it as a tracker...
    private val beepQueue = ArrayList<Pair<Int, Float>>()
    private var beepCursor = -1
    private var beepQueueLineExecTimer = 0 // millisec
    private var beepQueueFired = false

    private fun driveBeepQueueManager(delta: Int) {
        // start emitTone queue
        if (beepQueue.size > 0 && beepCursor == -1) {
            beepCursor = 0
        }

        // continue emitTone queue
        if (beepCursor >= 0 && beepQueueLineExecTimer >= beepQueueGetLenOfPtn(beepCursor)) {
            beepQueueLineExecTimer -= beepQueueGetLenOfPtn(beepCursor)
            beepCursor += 1
            beepQueueFired = false
        }

        // complete emitTone queue
        if (beepCursor >= beepQueue.size) {
            clearBeepQueue()
            if (DEBUG) println("!! Beep queue clear")
        }

        // actually play queue
        if (beepCursor >= 0 && beepQueue.size > 0 && !beepQueueFired) {
            playTone(beepQueue[beepCursor].first, beepQueue[beepCursor].second)
            beepQueueFired = true
        }

        if (beepQueueFired) beepQueueLineExecTimer += delta
    }

    fun clearBeepQueue() {
        beepQueue.clear()
        beepCursor = -1
        beepQueueLineExecTimer = 0
    }

    fun enqueueBeep(duration: Int, freq: Float) {
        beepQueue.add(Pair(Math.min(duration, beepMaxLen), freq))
    }

    fun beepQueueGetLenOfPtn(ptnIndex: Int) = beepQueue[ptnIndex].first


    ////////////////////
    // TONE GENERATOR //
    ////////////////////

    private val sampleRate = 44100
    private var beepSource: Int? = null
    private var beepBuffer: Int? = null
    var audioData: ByteBuffer? = null

    /**
     * @param duration : milliseconds
     * @param rampUp
     * @param rampDown
     *
     *     ,---. (true, true) ,---- (true, false) ----. (false, true) ----- (false, false)
     */
    private fun makeAudioData(duration: Int, freq: Float,
                              rampUp: Boolean = true, rampDown: Boolean = true): ByteBuffer {
        val audioData = BufferUtils.createByteBuffer(duration.times(sampleRate).div(1000))

        val realDuration = duration * sampleRate / 1000
        val chopSize = freq / sampleRate

        val amp = Math.max(4600f / freq, 1f)
        val nHarmonics = if (freq >= 22050f) 1
                         else if (freq >= 11025f) 2
                         else if (freq >= 5512.5f) 3
                         else if (freq >= 2756.25f) 4
                         else if (freq >= 1378.125f) 5
                         else if (freq >= 689.0625f) 6
                         else 7

        val transitionThre = 974.47218f

        // TODO volume ramping?
        if (freq == 0f) {
            for (x in 0..realDuration - 1) {
                audioData.put(0x00.toByte())
            }
        }
        else if (freq < transitionThre) { // chopper generator (for low freq)
            for (x in 0..realDuration - 1) {
                var sine: Float = amp * FastMath.cos(FastMath.TWO_PI * x * chopSize)
                if (sine > 0.79f) sine = 0.79f
                else if (sine < -0.79f) sine = -0.79f
                audioData.put(
                        (0.5f + 0.5f * sine).times(0xFF).roundInt().toByte()
                )
            }
        }
        else { // harmonics generator (for high freq)
            for (x in 0..realDuration - 1) {
                var sine: Float = 0f
                for (k in 1..nHarmonics) { // mix only odd harmonics in order to make a squarewave
                    sine += FastMath.sin(FastMath.TWO_PI * (2*k - 1) * chopSize * x) / (2*k - 1)
                }
                audioData.put(
                        (0.5f + 0.5f * sine).times(0xFF).roundInt().toByte()
                )
            }
        }

        audioData.rewind()

        return audioData
    }

    private fun playTone(leninmilli: Int, freq: Float) {
        audioData = makeAudioData(leninmilli, freq)


        if (!AL.isCreated()) AL.create()


        // Clear error stack.
        AL10.alGetError()

        beepBuffer = AL10.alGenBuffers()
        checkALError()

        try {
            AL10.alBufferData(beepBuffer!!, AL10.AL_FORMAT_MONO8, audioData, sampleRate)
            checkALError()

            beepSource = AL10.alGenSources()
            checkALError()

            try {
                AL10.alSourceQueueBuffers(beepSource!!, beepBuffer!!)
                checkALError()

                AL10.alSource3f(beepSource!!, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_REFERENCE_DISTANCE, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_MAX_DISTANCE, 1f)
                AL10.alSourcef(beepSource!!, AL10.AL_GAIN, 0.3f)
                checkALError()

                AL10.alSourcePlay(beepSource!!)
                checkALError()

            }
            catch (e: ALException) {
                AL10.alDeleteSources(beepSource!!)
            }
        }
        catch (e: ALException) {
            if (beepSource != null) AL10.alDeleteSources(beepSource!!)
        }
    }

    // Custom implementation of Util.checkALError() that uses our custom exception.
    private fun checkALError() {
        val errorCode = AL10.alGetError()
        if (errorCode != AL10.AL_NO_ERROR) {
            throw ALException(errorCode)
        }
    }

}