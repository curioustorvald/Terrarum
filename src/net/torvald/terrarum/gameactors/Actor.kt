package net.torvald.terrarum.gameactors

import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.audio.MusicContainer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TrackVolume
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.savegame.toBigEndian
import net.torvald.terrarum.utils.PasswordBase32
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


typealias ActorID = Int

/**
 * @param renderOrder invisible/technical must use "Actor.RenderOrder.MIDDLE"
 *
 * Created by minjaesong on 2015-12-31.
 */
abstract class Actor : Comparable<Actor>, Runnable {

    /**
     * Valid RefID is equal to or greater than 16777216.
     * @return Reference ID. (16777216-0x7FFF_FFFF)
     */
    open var referenceID: ActorID = Terrarum.generateUniqueReferenceID() // in old time this was nullable without initialiser. If you're going to revert to that, add the reason why this should be nullable.

    /**
     * RenderOrder does not affect ReferenceID "too much" (ID generation will still depend on it, but it's just because of ye olde tradition by now)
     *
     * IngameRenderer will only look for RenderOrder and won't look for referenceID, so if you want to change the RenderOrder, just modify this field and not the referenceID.
     */
    var renderOrder = RenderOrder.MIDDLE

    protected constructor()

    // needs zero-arg constructor for serialiser to work
    constructor(renderOrder: RenderOrder, id: ActorID?) : this() {
        if (id != null) referenceID = id
        this.renderOrder = renderOrder
    }


    enum class RenderOrder {
        FAR_BEHIND, // wires
        BEHIND, // tapestries, some particles (obstructed by terrain)
        MIDDLE, // actors
        MIDTOP, // bullets, thrown items
        FRONT,  // front walls ("blocks" that obstruct actors)
        OVERLAY // screen overlay, not affected by lightmap
    }

    fun update(delta: Float) {
        if (!canBeDespawned) flagDespawn = false // actively deny despawning request if cannot be despawned
        if (canBeDespawned && flagDespawn) {
            despawn()
            despawned = true
        }
        if (!despawned) {
            updateImpl(delta)
        }
    }

    abstract fun updateImpl(delta: Float)

    var actorValue = ActorValue(this)
    @Volatile var flagDespawn = false
    @Transient var despawnHook: (Actor) -> Unit = {}
    @Transient open val canBeDespawned = true
    @Volatile internal var despawned = false

    @Transient open val stopMusicOnDespawn = true

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        return referenceID == (other as Actor).referenceID
    }
    override fun hashCode() = referenceID
    override fun toString() =
            if (actorValue.getAsString("name").isNullOrEmpty())
                "${hashCode()}"
            else
                "${hashCode()} (${actorValue.getAsString("name")})"
    override fun compareTo(other: Actor): Int = (this.referenceID - other.referenceID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    /* called when the actor is loaded from the save; one use of this function is to "re-sync" the
     * Transient variables such as `mainUI` of FixtureBase
     */
    open fun reload() {
        actorValue.actor = this

        if (this is Pocketed)
            inventory.actor = this
        if (this is ActorHumanoid && vehicleRidingActorID != null) {
            vehicleRiding = INGAME.getActorByID(vehicleRidingActorID!!) as Controllable
        }
    }

    open fun despawn() {
        if (canBeDespawned) {
            musicTracks1.forEach { name ->
                val it = App.audioMixer.dynamicTracks[name.substring(2).toInt() - 1]

                printdbg(this, "stop track $name")

                if (stopMusicOnDespawn) {
                    it.stop()
                }

                it.filters[0] = NullFilter
                it.filters[1] = NullFilter
                it.processor.streamBuf?.pitch = 1f
            }

            despawnHook(this)
        }
    }

    /**
     * ActorValue change event handler
     *
     * @param value null if the key is deleted
     */
    abstract @Event fun onActorValueChange(key: String, value: Any?)

//    @Transient val soundTracks = HashMap<Sound, TerrarumAudioMixerTrack>()
    @Transient val musicTracks = HashMap<MusicContainer, TerrarumAudioMixerTrack>()
    @Transient private val musicTracks1 = ArrayList<String>()

    /*open fun startAudio(sound: Sound) {
        getTrackByAudio(sound)?.let {
            it.trackingTarget = if (this is ActorWithBody) this else null
            it.currentSound = sound
            it.play()
        }
    }*/

    fun getTrackByAudio(music: MusicContainer): TerrarumAudioMixerTrack? {
        // get existing track
        var track = musicTracks[music]

        // if there is no existing track, try to get one
        if (track == null) {
            track = if (this == Terrarum.ingame?.actorNowPlaying)
                App.audioMixer.getFreeTrackNoMatterWhat()
            else
                App.audioMixer.getFreeTrack()
            // if the request was successful, put it into the hashmap
            if (track != null) {
                musicTracks[music] = track
                musicTracks1.add(track.name)
                track.stop()
                track.trackingTarget = this
            }
            else {
                printdbgerr(this, "Could not get a free track")
            }
        }

//        printdbg(this, "Dynamic Source ${track?.name}")

        return track
    }

    /**
     * To loop the audio, set `music.gdxMusic.isLooping` to `true`
     */
    open fun startAudio(music: MusicContainer, volume: TrackVolume = 1.0, doSomethingWithTrack: (TerrarumAudioMixerTrack) -> Unit = {}) {
        getTrackByAudio(music).let {
            if (it == null) {
                printdbg(this, "cannot startAudio $music")
            }
            else {
                printdbg(this, "startAudio $music")
                it.trackingTarget = this
                it.currentTrack = music
                it.maxVolumeFun = { volume }
                it.volume = volume
//                it.play()
                it.playRequested.set(true)
                doSomethingWithTrack(it)
            }
        }
    }

    /*open fun stopAudio(sound: Sound) {

    }*/

    open fun stopAudio(music: MusicContainer, doSomethingWithTrack: (TerrarumAudioMixerTrack) -> Unit = {}) {
        musicTracks[music].let {
            if (it == null) {
//                printdbg(this, "cannot stopAudio $music")
            }
            else {
//                printdbg(this, "stopAudio $music")
                doSomethingWithTrack(it)
                it.stop()
            }
        }
    }

    /*open fun onAudioInterrupt(sound: Sound) {

    }*/

    open @Event fun onAudioInterrupt(music: MusicContainer) {
        music.songFinishedHook(music)
    }

    abstract fun dispose()

    @Transient val localHash = HQRNG().nextInt()
    @Transient val localHashStr = PasswordBase32.encode(localHash.toBigEndian()).substringBefore('=')
}

annotation class Event
