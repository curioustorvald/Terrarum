package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.AudioMixer.Companion.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.audio.MusicContainer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.audio.dsp.Phono
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumMusicGovernor
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.gameitems.MusicDiscHelper
import net.torvald.terrarum.modulebasegame.ui.UIJukebox
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-01-11.
 */
class FixtureJukebox : Electric, PlaysMusic {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 3),
        nameFun = { Lang["ITEM_JUKEBOX"] },
        mainUI = UIJukebox()
    )

    @Transient var discCurrentlyPlaying: Int? = null; private set
    @Transient var musicNowPlaying: MusicContainer? = null; private set

    @Transient private val backLamp: SheetSpriteAnimation
    @Transient private val playMech: SheetSpriteAnimation

    @Transient private val filterIndex = 0

    internal val discInventory = ArrayList<ItemID>()

    val musicIsPlaying: Boolean
        get() = musicNowPlaying != null

    init {
        (mainUI as UIJukebox).parent = this

        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/jukebox.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/jukebox_emsv.tga")

        density = 1400.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 3, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 3)).let {
            it.setRowsAndFrames(3,5)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, TILE_SIZE * 2, TILE_SIZE * 3)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 200.0

        setWireSinkAt(0, 2, "appliance_power")
        setWireConsumptionAt(0, 2, Vector2(350.0, 0.0))

        backLamp = SheetSpriteAnimation(this).also {
            it.setSpriteImage(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 3))
            it.setRowsAndFrames(3,5)
            it.currentRow = 1
            it.currentFrame = 0
        }

        playMech = SheetSpriteAnimation(this).also {
            it.setSpriteImage(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 3))
            it.setRowsAndFrames(3,5)
            it.currentRow = 2
            it.currentFrame = 0
        }


        App.audioMixerReloadHooks[this] = {
            loadEffector(musicTracks[musicNowPlaying])
        }

        despawnHook = {
            stopGracefully()
        }
    }

    @Transient override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TILE_SIZED * 2, TILE_SIZED * 3), Cvec(0.44f, 0.41f, 0.40f, 0.2f)))

    override val canBeDespawned: Boolean
        get() = discInventory.isEmpty()

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // supress the normal background music playback
        if (musicIsPlaying && !flagDespawn) {
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, true)
        }
    }


    fun playDisc(index: Int) {
        if (index !in discInventory.indices) return


        printdbg(this, "Play disc $index!")

        val disc = discInventory[index]
        val musicFile = (ItemCodex[disc] as? ItemFileRef)?.getAsGdxFile()

        if (musicFile != null) {
            val (title, artist) = MusicDiscHelper.getMetadata(musicFile)

            printdbg(this, "Title: $title, artist: $artist")

            musicNowPlaying = MusicContainer(title, musicFile.file(), Gdx.audio.newMusic(musicFile)) {
                unloadEffector(musicNowPlaying)
                discCurrentlyPlaying = null
                musicNowPlaying?.gdxMusic?.tryDispose()
                musicNowPlaying = null

                printdbg(this, "Stop music $title - $artist")

                // can't call stopDiscPlayback() because of the recursion

                (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, pauseLen = (INGAME.musicGovernor as TerrarumMusicGovernor).getRandomMusicInterval())

                backLamp.currentFrame = 0
                playMech.currentFrame = 0
            }

            discCurrentlyPlaying = index

            App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2f) {
                startAudio(musicNowPlaying!!) { loadEffector(it) }
            }


            backLamp.currentFrame = 1 + (index / 2)
            playMech.currentFrame = 1 + (index / 2)
        }

    }

    @Transient private var lampDecay = 0f
    @Transient private var vol = 0f
    @Transient private var lampIntensity = 0f

    /**
     * Try to stop the disc being played, and reset the background music cue
     */
    fun stopGracefully() {
        stopDiscPlayback()
        (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, pauseLen = (INGAME.musicGovernor as TerrarumMusicGovernor).getRandomMusicInterval())

    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        blendNormalStraightAlpha(batch)
        super.drawBody(frameDelta, batch)

        if (isVisible && musicNowPlaying != null) {
            val vol0 = (musicTracks[musicNowPlaying]?.processor?.maxSigLevel?.average() ?: 0.0).toFloat()
            vol = FastMath.interpolateLinear(0.8f, vol0, lampDecay)
        }
        else {
            vol = 0f
        }

        lampIntensity = vol.coerceIn(0f, 1f)

        blendScreen(batch)
        backLamp.colourFilter = Color(0f, lampIntensity, 0f, 1f)
        drawSpriteInGoodPosition(frameDelta, backLamp, batch)

        blendNormalStraightAlpha(batch)
        drawSpriteInGoodPosition(frameDelta, playMech, batch)

        lampDecay = vol
    }

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        blendNormalStraightAlpha(batch)
        super.drawEmissive(frameDelta, batch)

        if (isVisible && musicNowPlaying != null) {
            blendScreen(batch)
            backLamp.colourFilter = Color(0f, lampIntensity / 2f, 0f, 1f)
            drawSpriteInGoodPosition(frameDelta, backLamp, batch)
        }
    }

    private fun stopDiscPlayback() {
        musicNowPlaying?.let {
            stopAudio(it)
            unloadEffector(it)
        }

        backLamp.currentFrame = 0
        playMech.currentFrame = 0
    }

    private fun loadEffector(it: TerrarumAudioMixerTrack?) {
        loadConvolver(filterIndex, it, "basegame", "audio/convolution/Soundwoofer - large_speaker_Marshall JVM 205C SM57 A 0 0 1.bin")
        setJitter(it, 1, 0.005f)
    }

    private fun unloadEffector(music: MusicContainer?) {
        unloadConvolver(this, filterIndex, music)
        unsetJitter(this, music)
    }

    override fun reload() {
        super.reload()
        // cannot resume playback, just stop the music
        discCurrentlyPlaying = null
        musicNowPlaying = null
    }

    override fun dispose() {
        App.audioMixerReloadHooks.remove(this)
        super.dispose()

        // no need to dispose of backlamp and playmech: they share the same texture with the main sprite
    }

    companion object {
        fun loadConvolver(filterIndex: Int, it: TerrarumAudioMixerTrack?, module: String, path: String, gain: Float = 5f / 16f, satLim: Float = 1f) {
            it?.filters?.set(filterIndex, Phono(
                module,
                path,
                0f, gain, satLim
            ))
        }

        fun setJitter(it: TerrarumAudioMixerTrack?, mode: Int, intensity: Float) {
//            it?.let {
//                it.processor.jitterMode = mode
//                it.processor.jitterIntensity = intensity
//            }
        }

        fun unloadConvolver(actor: Actor, filterIndex: Int, music: MusicContainer?) {
            if (music != null) {
                actor.musicTracks[music]?.let {
                    it.filters[filterIndex] = NullFilter
                }
            }
        }

        fun unsetJitter(actor: Actor, music: MusicContainer?) {
//            actor.musicTracks[music]?.let {
//                it.processor.jitterMode = 0
//                it.processor.jitterIntensity = 0f
//            }
        }
    }
}