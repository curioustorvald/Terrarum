package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.getHashStr
import net.torvald.terrarum.modulebasegame.MusicContainer
import kotlin.math.log10
import kotlin.math.pow

typealias TrackVolume = Double

class TerrarumAudioMixerTrack(val name: String, val isMaster: Boolean = false): Disposable {

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

    val filters: Array<TerrarumAudioFilter> = Array(4) { NullFilter }

    internal val sidechainInputs = Array<Pair<TerrarumAudioMixerTrack, TrackVolume>?>(16)  { null }
    internal fun getSidechains(): List<TerrarumAudioMixerTrack?> = sidechainInputs.map { it?.first }
    fun addSidechainInput(input: TerrarumAudioMixerTrack, inputVolume: TrackVolume) {
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
    internal val deviceBufferSize = Gdx.audio.javaClass.getDeclaredField("deviceBufferSize").let {
        it.isAccessible = true
        it.get(Gdx.audio) as Int
    }
    internal val deviceBufferCount = Gdx.audio.javaClass.getDeclaredField("deviceBufferCount").let {
        it.isAccessible = true
        it.get(Gdx.audio) as Int
    }
    internal val adev: OpenALBufferedAudioDevice? =
        if (isMaster) {
            OpenALBufferedAudioDevice(
                Gdx.audio as OpenALLwjgl3Audio,
                SAMPLING_RATE,
                false,
                deviceBufferSize,
                deviceBufferCount
            ) {}
        }
        else null


    /**
     * assign nextTrack to currentTrack, then assign nextNext to nextTrack.
     * Whatever is on the currentTrack will be lost.
     */
    fun queueNext(nextNext: MusicContainer? = null) {
        currentTrack = nextTrack
        nextTrack = nextNext
    }


    internal var streamPlaying = false
    fun play() {
        streamPlaying = true
//        currentTrack?.gdxMusic?.play()
    }

    val isPlaying: Boolean?
        get() = currentTrack?.gdxMusic?.isPlaying

    override fun dispose() {
        if (isMaster) {
            queueDispatcher.stop()
            queueDispatcherThread.join()
        }
        processor.stop()
        processorThread.join()
        adev?.dispose()
    }

    override fun equals(other: Any?) = this.hash == (other as TerrarumAudioMixerTrack).hash

    fun fireSongFinishHook() {
        currentTrack?.songFinishedHook?.invoke(currentTrack!!.gdxMusic)
    }


    // 1st ring of the hell: the THREADING HELL //

    val BUFFER_SIZE = 16384

    internal var processor = MixerTrackProcessor(BUFFER_SIZE, SAMPLING_RATE, this)
    private val processorThread = Thread(processor).also {
        it.start()
    }
    internal var pcmQueue = Queue<List<FloatArray>>()
    private lateinit var queueDispatcher: FeedSamplesToAdev
    private lateinit var queueDispatcherThread: Thread

    init {
        pcmQueue.addLast(listOf(FloatArray(BUFFER_SIZE / 4), FloatArray(BUFFER_SIZE / 4)))
        pcmQueue.addLast(listOf(FloatArray(BUFFER_SIZE / 4), FloatArray(BUFFER_SIZE / 4)))

        if (isMaster) {
            queueDispatcher = FeedSamplesToAdev(BUFFER_SIZE, SAMPLING_RATE, this)
            queueDispatcherThread = Thread(queueDispatcher).also {
                it.start()
            }
        }
    }

}

fun fullscaleToDecibels(fs: Double) = 20.0 * log10(fs)
fun decibelsToFullscale(db: Double) = 10.0.pow(db / 20.0)