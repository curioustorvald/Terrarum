package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.gameitems.MusicDiscHelper
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-02-07.
 */
class FixtureMusicalTurntable : Electric, PlaysMusic {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_TURNTABLE"] }
    )

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

        addQuickLookupParam {
            musicNowPlaying.let {
                if (it == null) "" else "♫ ${App.fontGame.toColorCode(5,15,5)}${it.name}${App.fontGame.noColorCode}"
            }
        }
    }

    internal var disc: ItemID? = null

    override val canBeDespawned: Boolean
        get() = disc == null

    override fun onInteract(mx: Double, my: Double) {
        if (disc == null) {
            if (INGAME.actorNowPlaying != null) {
                val itemOnGrip =
                    INGAME.actorNowPlaying!!.inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)
                val itemProp = ItemCodex[itemOnGrip]

                if (itemProp?.hasAllTagsOf("MUSIC", "PHONO") == true) {
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

    override fun updateImpl(delta: Float) {
        inOperation = musicIsPlaying
        super.updateImpl(delta)
    }

    fun playDisc() {
        if (disc == null) return

        App.printdbg(this, "Play disc!")
        val musicFile = (ItemCodex[disc] as? ItemFileRef)?.getAsGdxFile()

        if (musicFile != null) {
            val (title, artist) = MusicDiscHelper.getMetadata(musicFile)

            App.printdbg(this, "Title: $title, artist: $artist")

            val returnToInitialState = {
                unloadEffector(musicNowPlaying)
                musicNowPlaying?.tryDispose()
                musicNowPlaying = null

            }

            musicNowPlaying = MusicContainer(title, musicFile.file()) {
                returnToInitialState()
                App.printdbg(this, "Stop music $title - $artist")
            }

            MusicService.playMusicalFixture(
                /* action: () -> Unit */ {
                    startAudio(musicNowPlaying!!) { loadEffector(it) }
                },
                /* musicFinished: () -> Boolean */ {
                    !musicIsPlaying
                },
                /* onSuccess: () -> Unit */ {

                },
                /* onFailure: (Throwable) -> Unit */ {
                    returnToInitialState
                }
            )


            (sprite as SheetSpriteAnimation).currentRow = 0
        }

    }

    /**
     * Try to stop the disc being played, and reset the background music cue
     */
    fun stopGracefully() {
        stopDiscPlayback()
        try {
            if (musicIsPlaying)
                MusicService.enterIntermission()
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }
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
        // update sprite
        (sprite as SheetSpriteAnimation).currentRow = (disc == null).toInt()
    }

    override fun dispose() {
        App.audioMixerReloadHooks.remove(this)
        super.dispose()

        // no need to dispose of backlamp and playmech: they share the same texture with the main sprite
    }
}