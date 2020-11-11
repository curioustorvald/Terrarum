package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.Second
import net.torvald.terrarum.ceilInt
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.io.Reader
import java.nio.ByteBuffer
import java.util.*

/**
 * A part that makes "computer fixture" actually work
 *
 * @param avFixtureComputer : actor values for FixtureComputerBase
 *
 * @param term : terminal that is connected to the computer fixtures, null if not connected any.
 * Created by minjaesong on 2016-09-10.
 */
class LuaComputerVM(val display: MDA) {

    val DEBUG = true

    lateinit private var luaJ_globals: Globals

    var stdout: PrintStream? = null
        private set
    var stderr: PrintStream? = null
        private set
    var stdin: InputStream? = null
        private set


    val UUID = java.util.UUID.randomUUID().toString()

    val computerValue = KVHashMap()

    var isHalted = false

    var stdinInput: Int = -1
        private set


    // os-related functions. These are called "machine" library-wise.
    private val startupTimestamp: Long = System.currentTimeMillis()
    /** Time elapsed since the power is on. */
    val milliTime: Int
        get() = (System.currentTimeMillis() - startupTimestamp).toInt()


    init {
        initSandbox()
    }

    fun initSandbox() {
        val luaJ_globals = Globals()
        luaJ_globals.load(JseBaseLib())
        luaJ_globals.load(PackageLib())
        luaJ_globals.load(Bit32Lib())
        luaJ_globals.load(TableLib())
        luaJ_globals.load(JseStringLib())
        luaJ_globals.load(CoroutineLib())
        luaJ_globals.load(JseMathLib())
        luaJ_globals.load(JseIoLib())
        luaJ_globals.load(JseOsLib())
        luaJ_globals.load(LuajavaLib())
        LoadState.install(luaJ_globals)
        LuaC.install(luaJ_globals)

        stdout = TerminalPrintStream(this)
        stderr = TerminalPrintStream(this)
        stdin =  TerminalInputStream(this)

        luaJ_globals.STDOUT = stdout
        luaJ_globals.STDERR = stderr
        luaJ_globals.STDIN = stdin

        luaJ_globals["bit"] = luaJ_globals["bit32"]

        // load libraries
        LoadTerrarumTermLib(luaJ_globals, display)

        // secure the sandbox
        //luaJ_globals["io"] = LuaValue.NIL
        // dubug should be sandboxed in BOOT.lua (use OpenComputers code)
        //val sethook = luaJ_globals["debug"]["sethook"]
        //luaJ_globals["debug"] = LuaValue.NIL


    }

    fun update(delta: Float) {

        if (currentExecutionThread.state == Thread.State.TERMINATED) {
            threadRun = false
        }




        if (!isHalted) {
            runBeepQueueManager(delta)
        }
    }

    fun keyPressed(c: Int) {
        stdinInput = c

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

    /**
     * @link https://stackoverflow.com/questions/16758346/how-pause-and-then-resume-a-thread#16758373
     */
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
                //lua.STDERR.println("${SimpleTextTerminal.ASCII_DLE}${e.message}${SimpleTextTerminal.ASCII_DC4}")
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

    class ComputerEmitTone(val computer: LuaComputerVM) : TwoArgFunction() {
        override fun call(millisec: LuaValue, freq: LuaValue): LuaValue {
            computer.playTone(millisec.checkdouble().toFloat(), freq.checkdouble())
            return LuaValue.NONE
        }
    }

    ///////////////////
    // BEEPER DRIVER //
    ///////////////////

    private val beepMaxLen = 10f
    // let's regard it as a tracker...
    private val beepQueue = ArrayList<Pair<Second, Double>>()
    private var beepCursor = -1
    private var beepQueueLineExecTimer: Second = 0f
    private var beepQueueFired = false

    private fun runBeepQueueManager(delta: Float) {
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
        beepQueueLineExecTimer = 0f

        //AL.destroy()

        if (DEBUG) println("[TerrarumComputerOld] !! Beep queue clear")
    }

    fun enqueueBeep(duration: Double, freq: Double) {
        beepQueue.add(Pair(Math.min(duration.toFloat(), beepMaxLen), freq))
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
    private fun makeAudioData(duration: Second, freq: Double,
                              rampUp: Boolean = true, rampDown: Boolean = true): ByteBuffer {

        TODO("with duration as Seconds")

        val audioDataSize = duration.times(sampleRate).ceilInt()
        val audioData = BufferUtils.createByteBuffer(audioDataSize)

        /*val realDuration = duration * sampleRate / 1000
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
            for (_ in 0..audioDataSize - 1) {
                audioData.put(0x00.toByte())
            }
        }
        else if (freq < transitionThre) { // chopper generator (for low freq)
            for (tsart in 0..audioDataSize - 1) {
                var sine: Double = amp * Math.cos(Math.PI * 2 * () * chopSize)
                if (sine > 0.79) sine = 0.79
                else if (sine < -0.79) sine = -0.79
                audioData.put(
                        (0.5 + 0.5 * sine).times(0xFF).roundToInt().toByte()
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
                        (0.5 + 0.5 * sine).times(0xFF).roundToInt().toByte()
                )
            }
        }*/

        audioData.rewind()

        return audioData
    }

    private fun playTone(length: Second, freq: Double) {
        /*audioData = makeAudioData(leninmilli, freq)


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
        }*/
    }

    // Custom implementation of Util.checkALError() that uses our custom exception.
    private fun checkALError() {
        val errorCode = AL10.alGetError()
        if (errorCode != AL10.AL_NO_ERROR) {
            throw ALException(errorCode)
        }
    }

}

class TerminalPrintStream(val host: LuaComputerVM) : PrintStream(TerminalOutputStream(host))

class TerminalOutputStream(val host: LuaComputerVM) : OutputStream() {
    override fun write(b: Int) = host.display.write(b.and(0xFF).toByte())
}

class TerminalInputStream(val host: LuaComputerVM) : InputStream() {

    override fun read(): Int {
        //System.err.println(Thread.currentThread().name)
        // would display "LuaJ Separated", which means this InputStream will not block main thread

        /*host.openStdin()
        synchronized(this) {
            (this as java.lang.Object).wait()
        }*/

        return 65//host.stdinInput
    }

}

class ALException(errorCode: Int) : Exception("ALerror: $errorCode") {

}


/**
 * Install a function into the lua.
 * @param identifier How you might call this lua function. E.g. "term.println"
 */
fun Globals.addOneArgFun(identifier: String, function: (p0: LuaValue) -> LuaValue) {
    val theActualFun = object : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            return function(p0)
        }
    }

    val tableNames = identifier.split('.')

    if (tableNames.isEmpty()) throw IllegalArgumentException("Identifier is empty")

    //println(tableNames)

    if (this[tableNames[0]].isnil()) {
        this[tableNames[0]] = LuaValue.tableOf()
    }
    else if (!this[tableNames[0]].istable()) {
        throw IllegalStateException("Redefinition: '${tableNames[0]}' (${this[tableNames[0]]})")
    }

    var currentTable = this[tableNames[0]]

    // turn nils into tables
    if (tableNames.size > 1) {
        tableNames.slice(1..tableNames.lastIndex).forEachIndexed { index, it ->
            if (currentTable[it].isnil()) {
                currentTable[it] = LuaValue.tableOf()
            }
            else if (!currentTable[it].istable()) {
                throw IllegalStateException("Redefinition: '${tableNames.slice(0..(index + 1)).joinToString(".")}' (${currentTable[it]})")
            }

            currentTable = currentTable[it]
        }

        // actually put the function onto the target
        // for some reason, memoisation doesn't work here so we use recursion to reach the target table as generated above
        tailrec fun putIntoTheTableRec(luaTable: LuaValue, recursionCount: Int) {
            if (recursionCount == tableNames.lastIndex - 1) {
                luaTable[tableNames[tableNames.lastIndex]] = theActualFun
            }
            else {
                putIntoTheTableRec(luaTable[tableNames[recursionCount + 1]], recursionCount + 1)
            }
        }

        putIntoTheTableRec(this[tableNames[0]], 0)
    }
    else {
        this[tableNames[0]] = theActualFun
    }
}

/**
 * Install a function into the lua.
 * @param identifier How you might call this lua function. E.g. "term.println"
 */
fun Globals.addZeroArgFun(identifier: String, function: () -> LuaValue) {
    val theActualFun = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return function()
        }
    }

    val tableNames = identifier.split('.')

    if (tableNames.isEmpty()) throw IllegalArgumentException("Identifier is empty")

    //println(tableNames)

    if (this[tableNames[0]].isnil()) {
        this[tableNames[0]] = LuaValue.tableOf()
    }
    else if (!this[tableNames[0]].istable()) {
        throw IllegalStateException("Redefinition: '${tableNames[0]}' (${this[tableNames[0]]})")
    }

    var currentTable = this[tableNames[0]]

    // turn nils into tables
    if (tableNames.size > 1) {
        tableNames.slice(1..tableNames.lastIndex).forEachIndexed { index, it ->
            if (currentTable[it].isnil()) {
                currentTable[it] = LuaValue.tableOf()
            }
            else if (!currentTable[it].istable()) {
                throw IllegalStateException("Redefinition: '${tableNames.slice(0..(index + 1)).joinToString(".")}' (${currentTable[it]})")
            }

            currentTable = currentTable[it]
        }

        // actually put the function onto the target
        // for some reason, memoisation doesn't work here so we use recursion to reach the target table as generated above
        tailrec fun putIntoTheTableRec(luaTable: LuaValue, recursionCount: Int) {
            if (recursionCount == tableNames.lastIndex - 1) {
                luaTable[tableNames[tableNames.lastIndex]] = theActualFun
            }
            else {
                putIntoTheTableRec(luaTable[tableNames[recursionCount + 1]], recursionCount + 1)
            }
        }

        putIntoTheTableRec(this[tableNames[0]], 0)
    }
    else {
        this[tableNames[0]] = theActualFun
    }
}

/**
 * Install a function into the lua.
 * @param identifier How you might call this lua function. E.g. "term.println"
 */
fun Globals.addTwoArgFun(identifier: String, function: (p0: LuaValue, p1: LuaValue) -> LuaValue) {
    val theActualFun = object : TwoArgFunction() {
        override fun call(p0: LuaValue, p1: LuaValue): LuaValue {
            return function(p0, p1)
        }
    }

    val tableNames = identifier.split('.')

    if (tableNames.isEmpty()) throw IllegalArgumentException("Identifier is empty")

    //println(tableNames)

    if (this[tableNames[0]].isnil()) {
        this[tableNames[0]] = LuaValue.tableOf()
    }
    else if (!this[tableNames[0]].istable()) {
        throw IllegalStateException("Redefinition: '${tableNames[0]}' (${this[tableNames[0]]})")
    }

    var currentTable = this[tableNames[0]]

    // turn nils into tables
    if (tableNames.size > 1) {
        tableNames.slice(1..tableNames.lastIndex).forEachIndexed { index, it ->
            if (currentTable[it].isnil()) {
                currentTable[it] = LuaValue.tableOf()
            }
            else if (!currentTable[it].istable()) {
                throw IllegalStateException("Redefinition: '${tableNames.slice(0..(index + 1)).joinToString(".")}' (${currentTable[it]})")
            }

            currentTable = currentTable[it]
        }

        // actually put the function onto the target
        // for some reason, memoisation doesn't work here so we use recursion to reach the target table as generated above
        tailrec fun putIntoTheTableRec(luaTable: LuaValue, recursionCount: Int) {
            if (recursionCount == tableNames.lastIndex - 1) {
                luaTable[tableNames[tableNames.lastIndex]] = theActualFun
            }
            else {
                putIntoTheTableRec(luaTable[tableNames[recursionCount + 1]], recursionCount + 1)
            }
        }

        putIntoTheTableRec(this[tableNames[0]], 0)
    }
    else {
        this[tableNames[0]] = theActualFun
    }
}

/**
 * Install a function into the lua.
 * @param identifier How you might call this lua function. E.g. "term.println"
 */
fun Globals.addThreeArgFun(identifier: String, function: (p0: LuaValue, p1: LuaValue, p2: LuaValue) -> LuaValue) {
    val theActualFun = object : ThreeArgFunction() {
        override fun call(p0: LuaValue, p1: LuaValue, p2: LuaValue): LuaValue {
            return function(p0, p1, p2)
        }
    }

    val tableNames = identifier.split('.')

    if (tableNames.isEmpty()) throw IllegalArgumentException("Identifier is empty")

    //println(tableNames)

    if (this[tableNames[0]].isnil()) {
        this[tableNames[0]] = LuaValue.tableOf()
    }
    else if (!this[tableNames[0]].istable()) {
        throw IllegalStateException("Redefinition: '${tableNames[0]}' (${this[tableNames[0]]})")
    }

    var currentTable = this[tableNames[0]]

    // turn nils into tables
    if (tableNames.size > 1) {
        tableNames.slice(1..tableNames.lastIndex).forEachIndexed { index, it ->
            if (currentTable[it].isnil()) {
                currentTable[it] = LuaValue.tableOf()
            }
            else if (!currentTable[it].istable()) {
                throw IllegalStateException("Redefinition: '${tableNames.slice(0..(index + 1)).joinToString(".")}' (${currentTable[it]})")
            }

            currentTable = currentTable[it]
        }

        // actually put the function onto the target
        // for some reason, memoisation doesn't work here so we use recursion to reach the target table as generated above
        tailrec fun putIntoTheTableRec(luaTable: LuaValue, recursionCount: Int) {
            if (recursionCount == tableNames.lastIndex - 1) {
                luaTable[tableNames[tableNames.lastIndex]] = theActualFun
            }
            else {
                putIntoTheTableRec(luaTable[tableNames[recursionCount + 1]], recursionCount + 1)
            }
        }

        putIntoTheTableRec(this[tableNames[0]], 0)
    }
    else {
        this[tableNames[0]] = theActualFun
    }
}
