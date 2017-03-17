package net.torvald.terrarum

import net.torvald.terrarum.Terrarum.STATE_ID_TEST_INPUT
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.toUint
import net.torvald.terrarum.virtualcomputer.terminal.ALException
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.nio.ByteBuffer
import java.util.ArrayList
import javax.sound.midi.*
import kotlin.experimental.and

/**
 * Midi input test for Spieluhr (this game's version of Note Block)
 *
 * Spieluhrs can make sound ranged from C1 to C6
 * (61 keys, which is the most common Midi Keyboard configuration)
 *
 * There is some latency if you are on Windows. Mac and Linux should be okay
 * because real musicians use Mac anyway, for a reason.
 *
 * Created by SKYHi14 on 2017-03-17.
 */
class StateMidiInputTest : BasicGameState() {

    var midiKeyboard: MidiDevice? = null
    val beeperSlave = BeeperSlave()

    val preferredDeviceList = arrayOf(
            "USB MIDI"
    )
    val avoidedDeviceList = arrayOf(
            "Real Time Sequencer"
    )

    init {
        val midiDevInfo = MidiSystem.getMidiDeviceInfo()

        midiDevInfo.forEach {
            //println(it)

            val device = MidiSystem.getMidiDevice(it)
            try {
                if (!avoidedDeviceList.contains(device.deviceInfo.name)) {
                    device.transmitter // test if tranmitter available
                    //println("Transmitter: $it")

                    midiKeyboard = device
                }
            }
            catch (e: MidiUnavailableException) {
                //println("(no transmitter found)")
            }

            //println()
        }

        //midiKeyboard = MidiSystem.getMidiDevice()
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        if (midiKeyboard != null) {
            midiKeyboard!!.open()
            midiKeyboard!!.transmitter.receiver = MidiInputReceiver(beeperSlave)
            println("Opened Midi device ${midiKeyboard!!.deviceInfo.name}")
        }
        else {
            println("Midi keyboard not found, using computer keyboard as a controller.")
        }
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        beeperSlave.runBeepQueueManager(delta)
    }

    override fun getID() = STATE_ID_TEST_INPUT

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        g.font = Terrarum.fontGame
        g.drawString("Listening from ${midiKeyboard!!.deviceInfo.name}", 10f, 10f)
    }


    class MidiInputReceiver(val slave: BeeperSlave) : Receiver {
        override fun send(message: MidiMessage, timeStamp: Long) {
            //println("MIDI Event ${message}")
            val parsedEvent = ParseMidiMessage(message)
            println(parsedEvent ?: "Don't care")
            if (parsedEvent != null) {
                if (!parsedEvent.isNoteOff) {
                    slave.enqueueBeep(100, parsedEvent.frequency())
                }
            }
        }

        override fun close() {
        }
    }

}


class BeeperSlave {

    ///////////////////
    // BEEPER DRIVER //
    ///////////////////

    private val beepMaxLen = 10000
    // let's regard it as a tracker...
    private val beepQueue = ArrayList<Pair<Int, Double>>()
    private var beepCursor = -1
    private var beepQueueLineExecTimer: Millisec = 0
    private var beepQueueFired = false

    fun update(delta: Int) {
        runBeepQueueManager(delta)
    }

    fun runBeepQueueManager(delta: Int) {
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

    fun playTone(leninmilli: Int, freq: Double) {
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

object ParseMidiMessage {
    operator fun invoke(message: MidiMessage): MidiKeyEvent? {
        val bytes = message.message
        val header = bytes[0].toUint().ushr(4) // 0b0000 - 0b1111
        if (header == 0b1000) { // note off
            return MidiKeyEvent(true, bytes[1].toInt(), bytes[2].toInt()) // no need for uint()
        }
        else if (header == 0b1001) { // note on
            return MidiKeyEvent(false, bytes[1].toInt(), bytes[2].toInt()) // no need for uint()
        }
        else { // don't care
            return null
        }
    }

    data class MidiKeyEvent(val isNoteOff: Boolean, val key: Int, val velocity: Int) {
        override fun toString() = "${if (isNoteOff) "Off" else "On "} $key v$velocity"
        /**
         * @param tuning frequency of middle A (default: 440.0)
         */
        fun frequency(tuning: Double = 440.0): Double {
            val a3 = 69 // midi note number for middle A

            return tuning * Math.pow(2.0, (key - a3) / 12.0)
        }
    }
}