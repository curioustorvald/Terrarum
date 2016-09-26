package net.torvald.terrarum


import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.virtualcomputer.terminal.ALException
import org.apache.commons.csv.CSVRecord
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Created by minjaesong on 16-09-05.
 */
class StateTestingSandbox : BasicGameState() {


    override fun init(container: GameContainer?, game: StateBasedGame?) {
        playTone()
    }

    private val sampleRate = 22050
    private var beepSource: Int? = null
    private var beepBuffer: Int? = null

    /**
     * @param duration : milliseconds
     */
    private fun makeAudioData(duration: Int, freq: Float): ByteBuffer {
        val audioData = BufferUtils.createByteBuffer(duration.times(sampleRate).div(1000))

        val realDuration = duration * sampleRate / 1000
        val chopSize = freq * 2f / sampleRate

        val amp = Math.max(4600f / freq, 1f)
        val nHarmonics = 4

        val transitionThre = 1150f

        if (freq < transitionThre) { // chopper generator (for low freq)
            for (x in 0..realDuration - 1) {
                var sine: Float = amp * FastMath.cos(FastMath.PI * x * chopSize)
                if (sine > 1f) sine = 1f
                else if (sine < -1f) sine = -1f
                audioData.put(
                        (0.5f + 0.5f * sine).times(0xFF).toByte()
                )
            }
        }
        else { // harmonics generator (for high freq)
            for (x in 0..realDuration - 1) {
                var sine: Float = 0f
                for (k in 0..nHarmonics) { // mix only odd harmonics to make squarewave
                    sine += (1f / (2*k + 1)) *
                            FastMath.sin((2*k + 1) * FastMath.PI * x * chopSize)
                }
                audioData.put(
                        (0.5f + 0.5f * sine).times(0xFF).toByte()
                )
            }
        }

        audioData.rewind()

        return audioData
    }

    var audioData: ByteBuffer? = null

    private fun playTone() {
        if (audioData == null) audioData = makeAudioData(5000, 27.5f)


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

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {

    }

    // Custom implementation of Util.checkALError() that uses our custom exception.
    private fun checkALError() {
        val errorCode = AL10.alGetError()
        if (errorCode != AL10.AL_NO_ERROR) {
            throw ALException(errorCode)
        }
    }


    override fun getID() = Terrarum.STATE_ID_TEST_SHIT

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics?) {

    }

}