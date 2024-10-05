package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-03-10.
 */
class FixtureLogicSignalAdder : Electric, Reorientable {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_ADDER"] }
    )

    override var orientation = 0 // 0 1 2 3

    private fun setEmitterAndSink() {
        clearStatus()
        when (orientation) {
            0 -> {
                setWireSinkAt(0, 0, "digital_bit")
                setWireSinkAt(0, 1, "digital_bit")
                setWireEmitterAt(1, 0, "digital_bit")
                setWireEmitterAt(1, 1, "digital_bit")
            }
            1 -> {
                setWireSinkAt(1, 0, "digital_bit")
                setWireSinkAt(0, 0, "digital_bit")
                setWireEmitterAt(1, 1, "digital_bit")
                setWireEmitterAt(0, 1, "digital_bit")
            }
            2 -> {
                setWireSinkAt(1, 1, "digital_bit")
                setWireSinkAt(1, 0, "digital_bit")
                setWireEmitterAt(0, 1, "digital_bit")
                setWireEmitterAt(0, 0, "digital_bit")
            }
            3 -> {
                setWireSinkAt(0, 1, "digital_bit")
                setWireSinkAt(1, 1, "digital_bit")
                setWireEmitterAt(0, 0, "digital_bit")
                setWireEmitterAt(1, 0, "digital_bit")
            }
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
    }

    private fun reorient() {
        (sprite as SheetSpriteAnimation).currentFrame = orientation
        (spriteEmissive as SheetSpriteAnimation).currentFrame = orientation
    }

    override fun orientClockwise() {
        orientation = (orientation + 1) fmod 4
        reorient(); setEmitterAndSink(); updateK()
    }
    override fun orientAnticlockwise() {
        orientation = (orientation - 1) fmod 4
        reorient(); setEmitterAndSink(); updateK()
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_adder.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_adder_emsv.tga")

        density = 1400.0
        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 2*TILE_SIZE)).let {
            it.setRowsAndFrames(16,4)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, 2*TILE_SIZE, 2*TILE_SIZE)).let {
            it.setRowsAndFrames(16,4)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }

        setEmitterAndSink()
    }

    override fun reload() {
        super.reload()
        reorient()
        setEmitterAndSink()
        updateK()
    }

    private val I: Boolean
        get() = when (orientation) {
//            0 -> getWireStateAt(0, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            1 -> getWireStateAt(1, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            3 -> getWireStateAt(0, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private val J: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            1 -> getWireStateAt(0, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            3 -> getWireStateAt(1, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private fun updateK() {
        val (x1, y1) = when (orientation) {
            0 -> 1 to 0
            1 -> 1 to 1
            2 -> 0 to 1
            3 -> 0 to 0
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
        val (x2, y2) = when (orientation) {
            0 -> 1 to 1
            1 -> 0 to 1
            2 -> 0 to 0
            3 -> 1 to 0
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
        val output1 = (I xor J).toInt().toDouble()
        val output2 = (I and J).toInt().toDouble()
        setWireEmissionAt(x1, y1, Vector2(output1, 0.0))
        setWireEmissionAt(x2, y2, Vector2(output2, 0.0))

        // update sprite
        val one   = isSignalHigh(0, 0)
        val two   = isSignalHigh(1, 0)
        val four  = isSignalHigh(0, 1)
        val eight = isSignalHigh(1, 1)

        val state = one.toInt(0) or two.toInt(1) or four.toInt(2) or eight.toInt(3)

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
    }

    override fun updateSignal() {
        updateK()
    }

}