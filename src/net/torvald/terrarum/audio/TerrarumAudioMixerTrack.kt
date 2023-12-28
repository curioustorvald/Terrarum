package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.audio.dsp.TerrarumAudioFilter
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.getHashStr
import net.torvald.terrarum.hashStrMap
import net.torvald.terrarum.modulebasegame.MusicContainer
import kotlin.math.log10
import kotlin.math.pow

typealias TrackVolume = Double

enum class TrackType {
    STATIC_SOURCE, DYNAMIC_SOURCE, BUS, MASTER
}

class TerrarumAudioMixerTrack(
    val name: String,
    val trackType: TrackType,
    var doGaplessPlayback: Boolean = false, // if true, the audio will be pulled from the `nextTrack` to always fully fill the read-buffer
    var maxVolumeFun: () -> Double = {1.0}
): Disposable {

    var pullNextTrack = {}

    companion object {
        const val SAMPLING_RATE = 48000
        const val SAMPLING_RATEF = 48000f
        const val SAMPLING_RATED = 48000.0
        val AUDIO_BUFFER_SIZE = App.getConfigInt("audio_buffer_size") // n ms -> 384 * n
    }

    val hash = getHashStr()
    private val hashCode0 = hash.map { hashStrMap.indexOf(it) }.foldIndexed(0) { i, acc, c ->
        acc or (c shl (5*i))
    }

    var currentTrack: MusicContainer? = null
    var nextTrack: MusicContainer? = null

    var currentSound: Sound? = null // DYNAMIC_SOURCE only

    var volume: TrackVolume = 1.0
        get() = field
        set(value) {
            field = value
            currentTrack?.gdxMusic?.volume = volume.toFloat()
        }

    val maxVolume: Double
        get() = maxVolumeFun()

    var dBfs: Double
        get() = fullscaleToDecibels(volume)
        set(value) { volume = decibelsToFullscale(value) }

    val filters: Array<TerrarumAudioFilter> = Array(4) { NullFilter }

    var trackingTarget: Actor? = null

    var playStartedTime = 0L; private set

    inline fun <reified T> getFilter() = filters.filterIsInstance<T>().first()!!

    internal val sidechainInputs = ArrayList<Pair<TerrarumAudioMixerTrack, TrackVolume>>()
    internal fun getSidechains(): List<TerrarumAudioMixerTrack?> = sidechainInputs.map { it.first }
    fun addSidechainInput(input: TerrarumAudioMixerTrack, inputVolume: TrackVolume) {
        if (input.trackType == TrackType.MASTER)
            throw IllegalArgumentException("Cannot add master track as a sidechain")

        if (sidechainInputs.map { it.first }.any { it.hash == input.hash })
            throw IllegalArgumentException("The track '${input.hash}' already exists")

        if (getSidechains().any { mySidechain ->
            val theirSidechains = mySidechain?.getSidechains()
                theirSidechains?.any { theirSidechain -> theirSidechain?.hash == this.hash } == true
        })
            throw IllegalArgumentException("The track '${input.hash}' contains current track (${this.hash}) as its sidechain")


        sidechainInputs.add(input to inputVolume)
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
        if (trackType == TrackType.MASTER) {
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
        playStartedTime = System.nanoTime()
        streamPlaying = true
//        currentTrack?.gdxMusic?.play()
    }

    val isPlaying: Boolean
        get() = streamPlaying//currentTrack?.gdxMusic?.isPlaying

    override fun dispose() {
        /*if (isMaster) { // uncomment to multithread
            queueDispatcher.stop()
            queueDispatcherThread.join()
        }
        processor.stop()
        processorThread.join()*/
        adev?.dispose()
    }

    override fun equals(other: Any?) = this.hash == (other as TerrarumAudioMixerTrack).hash

    fun stop() {
        currentTrack?.reset()

        streamPlaying = false
//        playStartedTime = 0L

        if (trackingTarget != null && currentTrack != null) {
            trackingTarget!!.onAudioInterrupt(currentTrack!!)
        }

        fireSongFinishHook()
        // fireSoundFinishHook()

        trackingTarget = null
        processor.streamBuf = null
    }

    fun fireSongFinishHook() {
        currentTrack?.songFinishedHook?.invoke(currentTrack!!.gdxMusic)
    }


    // 1st ring of the hell: the THREADING HELL //

    internal var processor = MixerTrackProcessor(AUDIO_BUFFER_SIZE, SAMPLING_RATE, this)
    /*private val processorThread = Thread(processor).also { // uncomment to multithread
        it.priority = MAX_PRIORITY // higher = more predictable; audio delay is very noticeable so it gets high priority
        it.start()
    }*/
    internal var pcmQueue = Queue<List<FloatArray>>()
    private lateinit var queueDispatcher: FeedSamplesToAdev
    private lateinit var queueDispatcherThread: Thread

    init {
        pcmQueue.addLast(listOf(FloatArray(AUDIO_BUFFER_SIZE), FloatArray(AUDIO_BUFFER_SIZE)))
        pcmQueue.addLast(listOf(FloatArray(AUDIO_BUFFER_SIZE), FloatArray(AUDIO_BUFFER_SIZE)))

        /*if (isMaster) { // uncomment to multithread
            queueDispatcher = FeedSamplesToAdev(BUFFER_SIZE, SAMPLING_RATE, this)
            queueDispatcherThread = Thread(queueDispatcher).also {
                it.priority = MAX_PRIORITY // higher = more predictable; audio delay is very noticeable so it gets high priority
                it.start()
            }
        }*/
    }

    override fun hashCode() = hashCode0
}

fun fullscaleToDecibels(fs: Double) = 20.0 * log10(fs)
fun decibelsToFullscale(db: Double) = 10.0.pow(db / 20.0)