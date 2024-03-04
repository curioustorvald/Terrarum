package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.MusicContainer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumMusicGovernor
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.gameitems.MusicDiscHelper
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarum.ui.MouseLatch
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-02-07.
 */
class FixtureMusicalTurntable : Electric, PlaysMusic {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_TURNTABLE"] }
    ) {
        clickLatch.forceLatch()
    }

    @Transient var musicNowPlaying: MusicContainer? = null; private set

    @Transient private val filterIndex = 0

    val musicIsPlaying: Boolean
        get() = musicNowPlaying != null

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/turntable.tga")
        density = 1400.0
        setHitboxDimension(TILE_SIZE * 1, TILE_SIZE * 1, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 1, TILE_SIZE * 1)).let {
            it.setRowsAndFrames(2,2)
            it.currentRow = 1
        }

        actorValue[AVKey.BASEMASS] = 20.0


        setWireSinkAt(0, 0, "appliance_power")
        setWireConsumptionAt(0, 2, Vector2(80.0, 0.0))


        App.audioMixerReloadHooks[this] = {
            loadEffector(musicTracks[musicNowPlaying])
        }

        despawnHook = {
            stopGracefully()
        }
    }

    internal var disc: ItemID? = null

    @Transient private val clickLatch = MouseLatch()

    override val canBeDespawned: Boolean
        get() = disc == null

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // right click
        if (mouseUp) {
            clickLatch.latch {
                if (disc == null) {
                    if (INGAME.actorNowPlaying != null) {
                        val itemOnGrip =
                            INGAME.actorNowPlaying!!.inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)
                        val itemProp = ItemCodex[itemOnGrip]

                        if (itemProp?.hasAllTagOf("MUSIC", "PHONO") == true) {
                            disc = itemOnGrip
                            INGAME.actorNowPlaying!!.removeItem(itemOnGrip!!)
                            playDisc()
                        }
                    }
                }
                else {
                    stopGracefully()
                    PickaxeCore.dropItem(
                        disc!!,
                        intTilewiseHitbox.canonicalX.toInt(),
                        intTilewiseHitbox.canonicalY.toInt()
                    )
                    disc = null
                }
            }
        }


        // supress the normal background music playback
        if (musicIsPlaying && !flagDespawn) {
            (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, true)
        }
    }

    fun playDisc() {
        if (disc == null) return

        App.printdbg(this, "Play disc!")
        val musicFile = (ItemCodex[disc] as? ItemFileRef)?.getAsGdxFile()

        if (musicFile != null) {
            val (title, artist) = MusicDiscHelper.getMetadata(musicFile)

            App.printdbg(this, "Title: $title, artist: $artist")

            musicNowPlaying = MusicContainer(title, musicFile.file()) {
                unloadEffector(musicNowPlaying)
                musicNowPlaying?.gdxMusic?.tryDispose()
                musicNowPlaying = null

                App.printdbg(this, "Stop music $title - $artist")

                // can't call stopDiscPlayback() because of the recursion

                (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, pauseLen = (INGAME.musicGovernor as TerrarumMusicGovernor).getRandomMusicInterval())
            }

            App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 2f) {
                startAudio(musicNowPlaying!!) { loadEffector(it) }
            }

            (sprite as SheetSpriteAnimation).currentRow = 0
        }

    }

    /**
     * Try to stop the disc being played, and reset the background music cue
     */
    fun stopGracefully() {
        stopDiscPlayback()
        (INGAME.musicGovernor as TerrarumMusicGovernor).stopMusic(this, pauseLen = (INGAME.musicGovernor as TerrarumMusicGovernor).getRandomMusicInterval())

    }

    private fun stopDiscPlayback() {
        musicNowPlaying?.let {
            stopAudio(it)
            unloadEffector(it)
        }

        (sprite as SheetSpriteAnimation).currentRow = 1
    }

    private fun loadEffector(it: TerrarumAudioMixerTrack?) {
        FixtureJukebox.loadConvolver(filterIndex, it, "basegame", "audio/convolution/Soundwoofer - small_speaker_Gallien Krueger GK 250ML B5 Left A 230 200 320.bin", 3.5f / 16f, 0.8f)
        FixtureJukebox.setJitter(it, 1, 0.01f)
    }

    private fun unloadEffector(music: MusicContainer?) {
        FixtureJukebox.unloadConvolver(this, filterIndex, music)
        FixtureJukebox.unsetJitter(this, music)
    }

    override fun reload() {
        super.reload()
        // cannot resume playback, just stop the music
        musicNowPlaying = null
    }

    override fun dispose() {
        App.audioMixerReloadHooks.remove(this)
        super.dispose()

        // no need to dispose of backlamp and playmech: they share the same texture with the main sprite
    }
}