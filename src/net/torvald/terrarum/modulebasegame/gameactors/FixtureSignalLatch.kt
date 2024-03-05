package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Implements a D-Flip Flop with extra Set/Reset pins.
 *
 * Created by minjaesong on 2024-03-05.
 */
class FixtureSignalLatch : Electric, Reorientable {

    @Transient override val spawnNeedsWall = false
    @Transient override val spawnNeedsFloor = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 3),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_LATCH"] }
    )

    override var orientation = 0 // 0 2, where 2 is a mirror-image rather than rotation

    private fun setEmitterAndSink() {
        when (orientation) {
            0 -> {
                setWireSinkAt(0, 0, "digital_bit") // D
                setWireEmitterAt(1, 0, "digital_bit") // Q
                setWireSinkAt(0, 1, "digital_bit") // CLK
                setWireSinkAt(0, 2, "digital_bit") // S
                setWireSinkAt(1, 2, "digital_bit") // R
            }
            2 -> {
                setWireSinkAt(1, 0, "digital_bit") // D
                setWireEmitterAt(0, 0, "digital_bit") // Q
                setWireSinkAt(1, 0, "digital_bit") // CLK
                setWireSinkAt(1, 2, "digital_bit") // S
                setWireSinkAt(0, 2, "digital_bit") // R
            }
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
    }


    override fun orientClockwise() {
        orientation = (orientation + 2) % 4
        setEmitterAndSink(); updateQ()
    }
    override fun orientAnticlockwise() {
        orientation = (orientation - 2) % 4
        setEmitterAndSink(); updateQ()
    }


    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_latch.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_latch_emsv.tga")

        density = 1400.0
        setHitboxDimension(2*TILE_SIZE, 3*TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 3*TILE_SIZE)).let {
            it.setRowsAndFrames(16,4)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, 2*TILE_SIZE, 3*TILE_SIZE)).let {
            it.setRowsAndFrames(16,4)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }

        setEmitterAndSink()
    }

    override fun reload() {
        super.reload()
        setEmitterAndSink()
        updateQ()
    }

    private var internalState = false

    private val D: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 0, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private var CLK0old: Boolean = false

    private val CLK0: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 1, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private val S: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 2, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 2, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private val R: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(1, 2, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
            2 -> getWireStateAt(0, 2, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH
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
        val CLK0 = this.CLK0
        val CLK = (!CLK0old && CLK0) // only TRUE on rising edge
        val S = this.S
        val R = this.R

        // force set internal state
        if (S && !R) internalState = true // Set
        else if (!S && R) internalState = false // Reset
        else if (S && R) internalState = !internalState // Toggle
        else if (CLK) internalState = D

        // if force set pin is not high and clock is pulsed, make transition; stay otherwise
        val output = internalState
        setWireEmissionAt(x, y, Vector2(output.toInt().toDouble(), 0.0))

        CLK0old = CLK0

        // TODO update sprite
    }

    override fun updateSignal() {
        updateQ()
    }
}