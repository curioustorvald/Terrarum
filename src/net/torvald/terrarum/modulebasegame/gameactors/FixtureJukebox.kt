package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.AudioMixer.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.modulebasegame.TerrarumMusicGovernor
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-01-11.
 */
class FixtureJukebox : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 3),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_EMITTER"] }
    )

    @Transient private var discCurrentlyPlaying: Int? = null

    @Transient private val testMusic = ModMgr.getGdxFile("basegame", "audio/music/discs/01 Thousands of Shards.ogg").let {
        MusicContainer("Thousands of Shards", it.file(), Gdx.audio.newMusic(it)) {
            discCurrentlyPlaying = null
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
        }
    }

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
        AudioMixer.requestFadeOut(AudioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2f) {
            startAudio(testMusic)
        }
    }

    @Transient override var despawnHook: (FixtureBase) -> Unit = {
        (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(pauseLen = Math.random().toFloat() * 30f + 30f)
    }

    override fun reload() {
        super.reload()
        // cannot resume playback, just stop the music
        discCurrentlyPlaying = null
    }

    override fun dispose() {
        super.dispose()
//        testMusic.gdxMusic.dispose()
    }
}