package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-03-08.
 */
class FixtureLogicSignalRepeaterHorz : Electric, Reorientable {


    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 1),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_REPEATER"] }
    )

    override var orientation = 0 // 0 2, where 2 is a mirror-image rather than rotation

    private fun setEmitterAndSink() {
        clearStatus()
        when (orientation) {
            0 -> {
                setWireSinkAt(0, 0, "digital_bit") // D
                setWireEmitterAt(1, 0, "digital_bit") // Q
            }
            2 -> {
                setWireSinkAt(1, 0, "digital_bit") // D
                setWireEmitterAt(0, 0, "digital_bit") // Q
            }
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
    }

    private fun reorient() {
        (sprite as SheetSpriteAnimation).currentFrame = orientation / 2
        (spriteEmissive as SheetSpriteAnimation).currentFrame = orientation / 2
    }

    override fun orientClockwise() {
        orientation = (orientation + 2) fmod 4
        reorient(); setEmitterAndSink(); updateQ()
    }
    override fun orientAnticlockwise() {
        orientation = (orientation - 2) fmod 4
        reorient(); setEmitterAndSink(); updateQ()
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_repeater.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_repeater_emsv.tga")

        density = 1400.0
        setHitboxDimension(2*TILE_SIZE, 1*TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 1*TILE_SIZE)).let {
            it.setRowsAndFrames(4,2)
            it.delays = FloatArray(4) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, 2*TILE_SIZE, 1*TILE_SIZE)).let {
            it.setRowsAndFrames(4,2)
            it.delays = FloatArray(4) { Float.POSITIVE_INFINITY }
        }

        setEmitterAndSink()
    }


    override fun reload() {
        super.reload()
        reorient()
        setEmitterAndSink()
        updateQ()
    }

    private val D: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private fun updateQ() {
        val (x, y) = when (orientation) {
            0 -> 1 to 0
            2 -> 0 to 0
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
        // "capture" the input pin states
        val D = this.D

        setWireEmissionAt(x, y, Vector2(D.toInt().toDouble(), 0.0))

        // update sprite
        val one = isSignalHigh(0, 0)
        val two = isSignalHigh(1, 0)

        val state = one.toInt(0) or two.toInt(1)

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
    }

    override fun updateSignal() {
        updateQ()
    }
}