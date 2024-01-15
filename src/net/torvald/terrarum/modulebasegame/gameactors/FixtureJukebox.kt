package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.audio.AudioMixer.Companion.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.audio.dsp.Convolv
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.MusicContainer
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
class FixtureJukebox : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 3),
        nameFun = { Lang["ITEM_JUKEBOX"] },
        mainUI = UIJukebox()
    )

    @Transient var discCurrentlyPlaying: Int? = null; private set
    @Transient var musicNowPlaying: MusicContainer? = null; private set

    @Transient private val backLamp: SheetSpriteAnimation

    internal val discInventory = ArrayList<ItemID>()

    val musicIsPlaying: Boolean
        get() = musicNowPlaying != null

    init {
        (mainUI as UIJukebox).parent = this

        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/jukebox.tga")

        density = 1400.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 3, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 3)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 200.0

        setWireSinkAt(0, 2, "appliance_power")
        setWireConsumptionAt(0, 2, Vector2(350.0, 0.0))

        val backLampTex = Texture(ModMgr.getGdxFile("basegame", "sprites/fixtures/jukebox_innerlamp.tga"))
        backLamp = SheetSpriteAnimation(this).also {
            it.setSpriteImage(TextureRegionPack(backLampTex, TILE_SIZE * 2, TILE_SIZE * 3))
            it.setRowsAndFrames(1, 1)
        }
    }

    override val canBeDespawned: Boolean
        get() = discInventory.isEmpty()

    override fun update(delta: Float) {
        super.update(delta)

        // supress the normal background music playback
        if (musicIsPlaying && !flagDespawn) {
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic()
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
                unloadConvolver(musicNowPlaying)
                discCurrentlyPlaying = null
                musicNowPlaying?.gdxMusic?.tryDispose()
                musicNowPlaying = null

                printdbg(this, "Stop music $title - $artist")

                (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
            }

            discCurrentlyPlaying = index

            App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2f) {
                startAudio(musicNowPlaying!!) {
                    it.filters[2] = Convolv(
                        ModMgr.getFile(
                            "basegame",
                            "audio/convolution/Soundwoofer - large_speaker_Marshall JVM 205C SM57 A 0 0 1.bin"
                        ), 0f, 5f / 16f
                    )
                }
            }
        }

    }

    @Transient private var lampDecay = 0f

    /**
     * Try to stop the disc being played, and reset the background music cue
     */
    fun stopGracefully() {
        stopDiscPlayback()
        (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        blendNormalStraightAlpha(batch)
        super.drawBody(frameDelta, batch)

        if (isVisible && musicNowPlaying != null) {
            val vol0 = (musicTracks[musicNowPlaying]?.processor?.maxSigLevel?.average() ?: 0.0).toFloat()
            val vol = FastMath.interpolateLinear(0.8f, vol0, lampDecay)

            blendScreen(batch)
            backLamp.colourFilter = Color(0f, vol.coerceIn(0f, 1f), 0f, 1f)
            drawSpriteInGoodPosition(frameDelta, backLamp, batch)

            lampDecay = vol
        }
    }

    private fun stopDiscPlayback() {
        musicNowPlaying?.let {
            stopAudio(it)
            unloadConvolver(it)
        }
    }

    private fun unloadConvolver(music: MusicContainer?) {
        if (music != null) {
            musicTracks[music]?.let {
                it.filters[2] = NullFilter
            }
        }
    }

    @Transient override var despawnHook: (FixtureBase) -> Unit = { stopGracefully() }

    override fun reload() {
        super.reload()
        // cannot resume playback, just stop the music
        discCurrentlyPlaying = null
        musicNowPlaying = null
    }

    override fun dispose() {
        super.dispose()
//        testMusic.gdxMusic.dispose()
    }
}