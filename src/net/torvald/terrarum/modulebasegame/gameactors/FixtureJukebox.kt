package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.AudioMixer.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.audio.dsp.Convolv
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.blendScreen
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.modulebasegame.TerrarumMusicGovernor
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
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

    @Transient private var discCurrentlyPlaying: Int? = null
    @Transient private var musicNowPlaying: MusicContainer? = null

    @Transient private val testMusic = ModMgr.getGdxFile("basegame", "audio/music/discs/01 Thousands of Shards.ogg").let {
        MusicContainer("Thousands of Shards", it.file(), Gdx.audio.newMusic(it)) {
            unloadConvolver(musicNowPlaying)
            discCurrentlyPlaying = null
            musicNowPlaying = null
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
        }
    }

    @Transient private val backLamp: SheetSpriteAnimation

    init {
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

    private var waitAkku = 0f

    override fun update(delta: Float) {
        super.update(delta)

        if (discCurrentlyPlaying == null) {
            // wait 3 seconds
            if (waitAkku >= 3f) {
                discCurrentlyPlaying = 0
                playDisc(discCurrentlyPlaying!!)
                waitAkku = 0f
            }

            waitAkku += delta
        }
        else if (!flagDespawn) {
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic()
        }
    }


    private fun playDisc(index: Int) {
        printdbg(this, "Play disc $index!")

        musicNowPlaying = testMusic // todo use index

        AudioMixer.requestFadeOut(AudioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2f) {
            startAudio(musicNowPlaying!!) {
                it.filters[2] = Convolv(ModMgr.getFile("basegame", "audio/convolution/Soundwoofer - large_speaker_Marshall JVM 205C SM57 A 0 0 1.bin"), 0f, 5f / 16f)
            }
        }
    }

    @Transient private var lampDecay = 0f

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

    private fun forceStop() {
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

    @Transient override var despawnHook: (FixtureBase) -> Unit = {
        forceStop()
        (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
    }

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