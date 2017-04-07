package net.torvald.terrarum.virtualcomputer.computer

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.virtualcomputer.luaapi.*
import net.torvald.terrarum.virtualcomputer.peripheral.*
import net.torvald.terrarum.virtualcomputer.terminal.*
import net.torvald.terrarum.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.virtualcomputer.tvd.VirtualDisk
import net.torvald.terrarum.virtualcomputer.worldobject.ComputerPartsCodex
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Level
import kotlin.collections.HashMap

/**
 * A part that makes "computer fixture" actually work
 *
 * @param avFixtureComputer : actor values for FixtureComputerBase
 *
 * @param term : terminal that is connected to the computer fixtures, null if not connected any.
 * Created by minjaesong on 16-09-10.
 */
class TerrarumComputer(peripheralSlots: Int) {

    var maxPeripherals: Int = peripheralSlots
        private set

    val DEBUG_UNLIMITED_MEM = false
    val DEBUG = true


    lateinit var luaJ_globals: Globals
        private set

    var stdout: PrintStream? = null
        private set
    var stderr: PrintStream? = null
        private set
    var stdin: InputStream? = null
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

    val peripheralTable = ArrayList<Peripheral>()

    var stdinInput: Int = -1
        private set


    // os-related functions. These are called "machine" library-wise.
    private val startupTimestamp: Long = System.currentTimeMillis()
    /** Time elapsed since the power is on. */
    val milliTime: Int
        get() = (System.currentTimeMillis() - startupTimestamp).toInt()

    /** String:
     *    if it's UUID, formatted UUID as string, always 36 chars
     *    if not (test purpose only!), just String
     */
    val diskRack = HashMap<String, VirtualDisk>()

    fun attachDisk(slot: String, filename: String) {
        computerValue[slot] = filename

        // put disk in diskRack
        if (filename.isNotEmpty() && filename.isNotBlank()) {
            diskRack[slot] = VDUtil.readDiskArchive(
                    File(Terrarum.currentSaveDir.path + "/computers/$filename").absoluteFile,
                    Level.WARNING,
                    { },
                    Filesystem.sysCharset
            )
        }
    }

    init {
        computerValue["memslot0"] = 4864 // -1 indicates mem slot is empty
        computerValue["memslot1"] = -1 // put index of item here
        computerValue["memslot2"] = -1 // ditto.
        computerValue["memslot3"] = -1 // do.

        computerValue["processor"] = -1 // do.

        // as in "dev/hda"; refers hard disk drive (and no partitioning)
        attachDisk("hda", "uuid_testhda")
        attachDisk("hdb", "")
        attachDisk("hdc", "")
        attachDisk("hdd", "")
        // as in "dev/fd1"; refers floppy disk drive
        attachDisk("fd1", "")
        attachDisk("fd2", "")
        attachDisk("fd3", "")
        attachDisk("fd4", "")
        // SCSI connected optical drive
        attachDisk("sda", "")

        // boot device
        computerValue["boot"] = "hda"
    }

    fun getPeripheral(tableName: String): Peripheral? {
        peripheralTable.forEach {
            if (it.tableName == tableName)
                return it
        }
        return null
    }

    fun attachPeripheral(peri: Peripheral) {
        if (peripheralTable.size < maxPeripherals) {
            peripheralTable.add(peri)
            peri.loadLib(luaJ_globals)
            println("[TerrarumComputer] loading peripheral $peri")
        }
        else {
            throw Error("No vacant peripheral slot")
        }
    }

    fun detachPeripheral(peri: Peripheral) {
        if (peripheralTable.contains(peri)) {
            peripheralTable.remove(peri)
            println("[TerrarumComputer] unloading peripheral $peri")
        }
        else {
            throw IllegalArgumentException("Peripheral not exists: $peri")
        }
    }

    fun attachTerminal(term: Teletype) {
        this.term = term
        initSandbox(term)
    }

    fun initSandbox(term: Teletype) {
        luaJ_globals = JsePlatform.debugGlobals()

        stdout = TerminalPrintStream(this)
        stderr = TerminalPrintStream(this)
        stdin = TerminalInputStream(this)

        luaJ_globals.STDOUT = stdout
        luaJ_globals.STDERR = stderr
        luaJ_globals.STDIN = stdin

        luaJ_globals["bit"] = luaJ_globals["bit32"]

        // load libraries
        Term(luaJ_globals, term)
        Security(luaJ_globals)
        Filesystem(luaJ_globals, this)
        HostAccessProvider(luaJ_globals, this)
        Input(luaJ_globals, this)
        PcSpeakerDriver(luaJ_globals, this)
        WorldInformationProvider(luaJ_globals)

        // secure the sandbox
        //luaJ_globals["io"] = LuaValue.NIL
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



        // load every peripheral if we're in DEBUG
        if (DEBUG) {
            maxPeripherals = 32
            attachPeripheral(PeripheralInternet(this))
            attachPeripheral(PeripheralPSG(this))
            // ...
        }
    }

    fun update(gc: GameContainer, delta: Int) {
        input = gc.input

        if (currentExecutionThread.state == Thread.State.TERMINATED) {
            threadRun = false
        }




        if (!isHalted) {
            runBeepQueueManager(delta)
        }
    }

    fun keyPressed(key: Int, c: Char) {
        stdinInput = c.toInt()

        // wake thread
        runnableRunCommand.resume()

        synchronized(stdin!!) {
            (stdin as java.lang.Object).notifyAll()
        }
    }

    fun openStdin() {
        stdinInput = -1
        // sleep the thread
        runnableRunCommand.pause()
    }

    lateinit var currentExecutionThread: Thread
        private set
    lateinit var runnableRunCommand: ThreadRunCommand
        private set
    private var threadRun = false

    fun runCommand(line: String, env: String) {
        if (!threadRun) {
            runnableRunCommand = ThreadRunCommand(luaJ_globals, line, env)
            currentExecutionThread = Thread(null, runnableRunCommand, "LuaJ Separated")
            currentExecutionThread.start()
            threadRun = true
        }
    }

    fun runCommand(reader: Reader, filename: String) {
        if (!threadRun) {
            runnableRunCommand = ThreadRunCommand(luaJ_globals, reader, filename)
            currentExecutionThread = Thread(null, runnableRunCommand, "LuaJ Separated")
            currentExecutionThread.start()
            threadRun = true
        }
    }

    class ThreadRunCommand : Runnable {

        private val mode: Int
        private val arg1: Any
        private val arg2: String
        private val lua: Globals

        @Volatile private var running = true
        @Volatile private var paused = false
        private val pauseLock = java.lang.Object()

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
            synchronized(pauseLock) {
                if (!running) { // may have changed while waiting to
                    // synchronize on pauseLock
                    return
                }
                if (paused) {
                    try {
                        pauseLock.wait() // will cause this Thread to block until
                        // another thread calls pauseLock.notifyAll()
                        // Note that calling wait() will
                        // relinquish the synchronized lock that this
                        // thread holds on pauseLock so another thread
                        // can acquire the lock to call notifyAll()
                        // (link with explanation below this code)
                    }
                    catch (ex: InterruptedException) {
                        return
                    }

                    if (!running) { // running might have changed since we paused
                        return
                    }
                }
            }


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
                e.printStackTrace(System.err)
                lua.STDERR.println("${SimpleTextTerminal.ASCII_DLE}${e.message}${SimpleTextTerminal.ASCII_DC4}")
            }
        }

        fun stop() {
            running = false
            // you might also want to do this:
            //interrupt()
        }

        fun pause() {
            // you may want to throw an IllegalStateException if !running
            paused = true
        }

        fun resume() {
            synchronized(pauseLock) {
                paused = false
                pauseLock.notifyAll() // Unblocks thread
            }
        }
    }

    class LuaFunGetTotalMem(val computer: TerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(computer.memSize)
        }
    }

    class ComputerEmitTone(val computer: TerrarumComputer) : TwoArgFunction() {
        override fun call(millisec: LuaValue, freq: LuaValue): LuaValue {
            computer.playTone(millisec.checkint(), freq.checkdouble())
            return LuaValue.NONE
        }
    }

    ///////////////////
    // BEEPER DRIVER //
    ///////////////////

    private val beepMaxLen = 10000
    // let's regard it as a tracker...
    private val beepQueue = ArrayList<Pair<Int, Double>>()
    private var beepCursor = -1
    private var beepQueueLineExecTimer: Millisec = 0
    private var beepQueueFired = false

    private fun runBeepQueueManager(delta: Int) {
        // start emitTone queue
        if (beepQueue.size > 0 && beepCursor == -1) {
            beepCursor = 0
        }

        // advance emitTone queue
        if (beepCursor >= 0 && beepQueueLineExecTimer >= beepQueueGetLenOfPtn(beepCursor)) {
            beepQueueLineExecTimer -= beepQueueGetLenOfPtn(beepCursor)
            beepCursor += 1
            beepQueueFired = false
        }

        // complete emitTone queue
        if (beepCursor >= beepQueue.size) {
            clearBeepQueue()
        }

        // actually play queue
        if (beepCursor >= 0 && beepQueue.size > 0 && !beepQueueFired) {
            playTone(beepQueue[beepCursor].first, beepQueue[beepCursor].second)
            beepQueueFired = true

            // delete sources that is finished. AL is limited to 256 sources. If you exceed it,
            // we won't get any more sounds played.
            AL10.alSourcei(oldBeepSource, AL10.AL_BUFFER, 0)
            AL10.alDeleteSources(oldBeepSource)
            AL10.alDeleteBuffers(oldBeepBuffer)
        }

        if (beepQueueFired) beepQueueLineExecTimer += delta
    }

    fun clearBeepQueue() {
        beepQueue.clear()
        beepCursor = -1
        beepQueueLineExecTimer = 0

        //AL.destroy()

        if (DEBUG) println("[TerrarumComputer] !! Beep queue clear")
    }

    fun enqueueBeep(duration: Int, freq: Double) {
        beepQueue.add(Pair(Math.min(duration, beepMaxLen), freq))
    }

    fun beepQueueGetLenOfPtn(ptnIndex: Int) = beepQueue[ptnIndex].first


    ////////////////////
    // TONE GENERATOR //
    ////////////////////

    private val sampleRate = 44100
    private var beepSource: Int = -1
    private var beepBuffer: Int = -1
    private var oldBeepSource: Int = -1
    private var oldBeepBuffer: Int = -1
    var audioData: ByteBuffer? = null

    /**
     * @param duration : milliseconds
     * @param rampUp
     * @param rampDown
     *
     *     ,---. (true, true) ,---- (true, false) ----. (false, true) ----- (false, false)
     */
    private fun makeAudioData(duration: Millisec, freq: Double,
                              rampUp: Boolean = true, rampDown: Boolean = true): ByteBuffer {
        val audioData = BufferUtils.createByteBuffer(duration.times(sampleRate).div(1000))

        val realDuration = duration * sampleRate / 1000
        val chopSize = freq / sampleRate

        val amp = Math.max(4600.0 / freq, 1.0)
        val nHarmonics = if (freq >= 22050.0) 1
                         else if (freq >= 11025.0) 2
                         else if (freq >= 5512.5) 3
                         else if (freq >= 2756.25) 4
                         else if (freq >= 1378.125) 5
                         else if (freq >= 689.0625) 6
                         else 7

        val transitionThre = 974.47218

        // TODO volume ramping?
        if (freq == 0.0) {
            for (x in 0..realDuration - 1) {
                audioData.put(0x00.toByte())
            }
        }
        else if (freq < transitionThre) { // chopper generator (for low freq)
            for (x in 0..realDuration - 1) {
                var sine: Double = amp * Math.cos(Math.PI * 2 * x * chopSize)
                if (sine > 0.79) sine = 0.79
                else if (sine < -0.79) sine = -0.79
                audioData.put(
                        (0.5 + 0.5 * sine).times(0xFF).roundInt().toByte()
                )
            }
        }
        else { // harmonics generator (for high freq)
            for (x in 0..realDuration - 1) {
                var sine: Double = 0.0
                for (k in 1..nHarmonics) { // mix only odd harmonics in order to make a squarewave
                    sine += Math.sin(Math.PI * 2 * (2*k - 1) * chopSize * x) / (2*k - 1)
                }
                audioData.put(
                        (0.5 + 0.5 * sine).times(0xFF).roundInt().toByte()
                )
            }
        }

        audioData.rewind()

        return audioData
    }

    private fun playTone(leninmilli: Int, freq: Double) {
        audioData = makeAudioData(leninmilli, freq)


        if (!AL.isCreated()) AL.create()


        // Clear error stack.
        AL10.alGetError()

        oldBeepBuffer = beepBuffer
        beepBuffer = AL10.alGenBuffers()
        checkALError()

        try {
            AL10.alBufferData(beepBuffer, AL10.AL_FORMAT_MONO8, audioData, sampleRate)
            checkALError()

            oldBeepSource = beepSource
            beepSource = AL10.alGenSources()
            checkALError()

            try {
                AL10.alSourceQueueBuffers(beepSource, beepBuffer)
                checkALError()

                AL10.alSource3f(beepSource, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcef(beepSource, AL10.AL_REFERENCE_DISTANCE, 1f)
                AL10.alSourcef(beepSource, AL10.AL_MAX_DISTANCE, 1f)
                AL10.alSourcef(beepSource, AL10.AL_GAIN, 0.3f)
                checkALError()

                AL10.alSourcePlay(beepSource)
                checkALError()
            }
            catch (e: ALException) {
                AL10.alDeleteSources(beepSource)
            }
        }
        catch (e: ALException) {
            AL10.alDeleteSources(beepSource)
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