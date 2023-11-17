package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.*
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.getHashStr
import net.torvald.terrarum.modulebasegame.MusicContainer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log10
import kotlin.math.pow

typealias TrackVolume = Double

class TerrarumAudioMixerTracks(val isMaster: Boolean = false): Disposable {

    companion object {
        const val SAMPLING_RATE = 48000
    }

    val hash = getHashStr()

    var currentTrack: MusicContainer? = null
    var nextTrack: MusicContainer? = null

    var volume: TrackVolume = 1.0
        get() = field
        set(value) {
            field = value
            currentTrack?.gdxMusic?.volume = volume.toFloat()
        }

    var pan = 0.0

    var dBfs: Double
        get() = fullscaleToDecibels(volume)
        set(value) { volume = decibelsToFullscale(value) }

    val filters = Array(4) { NullFilter }

    private val sidechainInputs = Array<Pair<TerrarumAudioMixerTracks, TrackVolume>?>(16)  { null }
    internal fun getSidechains(): List<TerrarumAudioMixerTracks?> = sidechainInputs.map { it?.first }
    fun addSidechainInput(input: TerrarumAudioMixerTracks, inputVolume: TrackVolume) {
        if (input.isMaster)
            throw IllegalArgumentException("Cannot add master track as a sidechain")

        if (sidechainInputs.map { it?.first }.any { it?.hash == input.hash })
            throw IllegalArgumentException("The track '${input.hash}' already exists")

        if (getSidechains().any { mySidechain ->
            val theirSidechains = mySidechain?.getSidechains()
                theirSidechains?.any { theirSidechain -> theirSidechain?.hash == this.hash } == true
        })
            throw IllegalArgumentException("The track '${input.hash}' contains current track (${this.hash}) as its sidechain")

        val emptySpot = sidechainInputs.indexOf(null)
        if (emptySpot != -1) {
            sidechainInputs[emptySpot] = (input to inputVolume)
        }
        else {
            throw IllegalStateException("Sidechain is full (${sidechainInputs.size})!")
        }
    }


    // in bytes
    private val deviceBufferSize = Gdx.audio.javaClass.getDeclaredField("deviceBufferSize").let {
        it.isAccessible = true
        it.get(Gdx.audio) as Int
    }
    private val deviceBufferCount = Gdx.audio.javaClass.getDeclaredField("deviceBufferCount").let {
        it.isAccessible = true
        it.get(Gdx.audio) as Int
    }
    private val adev = OpenALBufferedAudioDevice(
        Gdx.audio as OpenALLwjgl3Audio,
        SAMPLING_RATE,
        false,
        deviceBufferSize,
        deviceBufferCount
    ) {}


    /**
     * assign nextTrack to currentTrack, then assign nextNext to nextTrack.
     * Whatever is on the currentTrack will be lost.
     */
    fun queueNext(nextNext: MusicContainer? = null) {
        currentTrack = nextTrack
        nextTrack = nextNext
    }


    private var streamPlaying = false
    fun play() {
        streamPlaying = true
//        currentTrack?.gdxMusic?.play()
    }

    val isPlaying: Boolean?
        get() = currentTrack?.gdxMusic?.isPlaying

    override fun dispose() {
        adev.dispose()
    }

    override fun equals(other: Any?) = this.hash == (other as TerrarumAudioMixerTracks).hash


    // 1st ring of the hell: the THREADING HELL //

    private val processJob: Job
    private var processContinuation: Continuation<Unit>? = null



    private val streamBuf = AudioProcessBuf(deviceBufferSize)
    private val sideChainBufs = Array(sidechainInputs.size) { AudioProcessBuf(deviceBufferSize) }
    private val outBufL0 = FloatArray(deviceBufferSize / 4)
    private val outBufR0 = FloatArray(deviceBufferSize / 4)
    private val outBufL1 = FloatArray(deviceBufferSize / 4)
    private val outBufR1 = FloatArray(deviceBufferSize / 4)

    init {
        processJob = GlobalScope.launch { // calling 'launch' literally launches the coroutine right awya
            // fetch deviceBufferSize amount of sample from the disk
            if (streamPlaying) {
                currentTrack?.gdxMusic?.forceInvoke<Unit>("read", arrayOf(streamBuf.shift()))
            }

            // also fetch samples from sidechainInputs
            // TODO

            // combine all the inputs
            // TODO this code just uses streamBuf

            val samplesL0 = streamBuf.getL0()
            val samplesR0 = streamBuf.getR0()
            val samplesL1 = streamBuf.getL1()
            val samplesR1 = streamBuf.getR1()

            // run the input through the stack of filters
            // TODO skipped lol

            // final writeout
            System.arraycopy(samplesL0, 0, outBufL0, 0, outBufL0.size)
            System.arraycopy(samplesR0, 0, outBufR0, 0, outBufR0.size)
            System.arraycopy(samplesL1, 0, outBufL1, 0, outBufL1.size)
            System.arraycopy(samplesR1, 0, outBufR1, 0, outBufR1.size)

            // by this time, the output buffer is filled with processed results, pause the execution
            if (!isMaster) {
                suspendCoroutine<Unit> {
                    processContinuation = it
                }
            }
            else {
                getSidechains().forEach {
                    it?.processContinuation?.resume(Unit)
                }
            }
        }
    }
}

fun fullscaleToDecibels(fs: Double) = 10.0 * log10(fs)
fun decibelsToFullscale(db: Double) = 10.0.pow(db / 10.0)