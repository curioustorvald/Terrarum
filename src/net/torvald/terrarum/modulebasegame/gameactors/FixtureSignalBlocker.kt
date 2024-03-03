package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-03-04.
 */
interface Reorientable {
    /**
     * Usually includes code snippet of `orientation = (orientation + 1) % 4`
     */
    fun orientClockwise()
    /**
     * Usually includes code snippet of `orientation = (orientation - 1) % 4`
     */
    fun orientAnticlockwise()
    /**
     * strictly 0 1 2 3. If your fixture can only be oriented in two ways, use value 0 2.
     */
    var orientation: Int
}

/**
 * Created by minjaesong on 2024-03-04.
 */
class FixtureSignalBlocker : Electric, Reorientable {

    @Transient override val spawnNeedsWall = false
    @Transient override val spawnNeedsFloor = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_BLOCKER"] }
    )

    override var orientation = 0

    private fun setEmitterAndSink() {
        when (orientation) {
            0 -> {
                setWireSinkAt(0, 0, "digital_bit")
                setWireSinkAt(0, 1, "digital_bit")
                setWireEmitterAt(1, 0, "digital_bit")
            }
            1 -> {
                setWireSinkAt(1, 0, "digital_bit")
                setWireSinkAt(0, 0, "digital_bit")
                setWireEmitterAt(1, 1, "digital_bit")
            }
            2 -> {
                setWireSinkAt(1, 1, "digital_bit")
                setWireSinkAt(1, 0, "digital_bit")
                setWireEmitterAt(0, 1, "digital_bit")
            }
            3 -> {
                setWireSinkAt(0, 1, "digital_bit")
                setWireSinkAt(1, 1, "digital_bit")
                setWireEmitterAt(0, 0, "digital_bit")
            }
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
    }

    override fun orientClockwise() {
        orientation = (orientation + 1) % 4
        setEmitterAndSink(); updateK()
    }
    override fun orientAnticlockwise() {
        orientation = (orientation - 1) % 4
        setEmitterAndSink(); updateK()
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_blocker.tga")

        density = 1400.0
        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 2*TILE_SIZE)).let {
            it.setRowsAndFrames(8,4)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        setEmitterAndSink()
    }

    override fun reload() {
        super.reload()
        setEmitterAndSink()
        updateK()
    }

    private val I: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 0).x >= ELECTIC_THRESHOLD_HIGH
            1 -> getWireStateAt(1, 0).x >= ELECTIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 1).x >= ELECTIC_THRESHOLD_HIGH
            3 -> getWireStateAt(0, 1).x >= ELECTIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private val J: Boolean
        get() = when (orientation) {
            0 -> getWireStateAt(0, 1).x >= ELECTIC_THRESHOLD_HIGH
            1 -> getWireStateAt(0, 0).x >= ELECTIC_THRESHOLD_HIGH
            2 -> getWireStateAt(1, 0).x >= ELECTIC_THRESHOLD_HIGH
            3 -> getWireStateAt(1, 1).x >= ELECTIC_THRESHOLD_HIGH
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }

    private fun updateK() {
        val (x, y) = when (orientation) {
            0 -> 1 to 0
            1 -> 1 to 1
            2 -> 0 to 1
            3 -> 0 to 0
            else -> throw IllegalStateException("Orientation not in range ($orientation)")
        }
        setWireEmissionAt(x, y, Vector2(I nimply J, 0.0))
    }

    override fun onRisingEdge(readFrom: BlockBoxIndex) {
        updateK()
    }

    override fun onFallingEdge(readFrom: BlockBoxIndex) {
        updateK()
    }

    override fun onSignalHigh(readFrom: BlockBoxIndex) {
        updateK()
    }

    override fun onSignalLow(readFrom: BlockBoxIndex) {
        updateK()
    }

    private infix fun Boolean.nimply(other: Boolean) = (this && !other).toInt().toDouble()

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        super.drawBody(frameDelta, batch)
    }
}