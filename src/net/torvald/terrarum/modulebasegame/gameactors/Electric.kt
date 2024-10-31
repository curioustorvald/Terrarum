package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.ui.UICanvas
import org.dyn4j.geometry.Vector2
import java.util.ArrayList

typealias WireEmissionType = String

/**
 * Created by minjaesong on 2021-08-10.
 */
open class Electric : FixtureBase {

    protected constructor() : super() {
        oldSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
//        newSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
    }

    /**
     * Making the sprite: do not address the CommonResourcePool directly; just do it like this snippet:
     *
     * ```makeNewSprite(FixtureBase.getSpritesheet("basegame", "sprites/fixtures/tiki_torch.tga", 16, 32))```
     */
    constructor(
        blockBox0: BlockBox,
        blockBoxProps: BlockBoxProps = BlockBoxProps(0),
        renderOrder: RenderOrder = RenderOrder.MIDDLE,
        nameFun: () -> String,
        mainUI: UICanvas? = null,
        inventory: FixtureInventory? = null,
        id: ActorID? = null
    ) : super(renderOrder, PhysProperties.IMMOBILE(), id) {
        blockBox = blockBox0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE * blockBox.width, TerrarumAppConfiguration.TILE_SIZE * blockBox.height, 0, 0)
        this.blockBoxProps = blockBoxProps
        this.renderOrder = renderOrder
        this.nameFun = nameFun
        this.mainUI = mainUI
        this.inventory = inventory

        if (mainUI != null)
            App.disposables.add(mainUI)

        oldSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
//        newSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
    }

    companion object {
        const val ELECTRIC_THRESHOLD_HIGH = 0.6666666666666666
        const val ELECTRIC_THRESHOLD_LOW = 0.3333333333333333
        const val ELECTRIC_THRESHOLD_EDGE_DELTA = 0.33333333333333337

        const val ELECTRIC_EPSILON_GENERIC = 1.0 / 1024.0
    }

    fun getWireEmitterAt(blockBoxIndex: BlockBoxIndex) = this.wireEmitterTypes[blockBoxIndex]
    fun getWireEmitterAt(point: Point2i) = this.wireEmitterTypes[pointToBlockBoxIndex(point)]
    fun getWireEmitterAt(x: Int, y: Int) = this.wireEmitterTypes[pointToBlockBoxIndex(x, y)]
    fun getWireSinkAt(blockBoxIndex: BlockBoxIndex) = this.wireSinkTypes[blockBoxIndex]
    fun getWireSinkAt(point: Point2i) = this.wireSinkTypes[pointToBlockBoxIndex(point)]
    fun getWireSinkAt(x: Int, y: Int) = this.wireSinkTypes[pointToBlockBoxIndex(x, y)]

    fun setWireEmitterAt(x: Int, y: Int, type: WireEmissionType) { wireEmitterTypes[pointToBlockBoxIndex(x, y)] = type }
    fun setWireSinkAt(x: Int, y: Int, type: WireEmissionType) { wireSinkTypes[pointToBlockBoxIndex(x, y)] = type }
    fun setWireEmissionAt(x: Int, y: Int, emission: Vector2) { wireEmission[pointToBlockBoxIndex(x, y)] = emission }
    fun setWireConsumptionAt(x: Int, y: Int, consumption: Vector2) { wireConsumption[pointToBlockBoxIndex(x, y)] = consumption }

    fun clearStatus() {
        wireSinkTypes.clear()
        wireEmitterTypes.clear()
        wireEmission.clear()
        wireConsumption.clear()
    }

    // these are characteristic properties of the fixture (they have constant value) so must not be serialised
    @Transient val wireEmitterTypes: HashMap<BlockBoxIndex, WireEmissionType> = HashMap()
    @Transient val wireSinkTypes: HashMap<BlockBoxIndex, WireEmissionType> = HashMap()

    val wireEmission: HashMap<BlockBoxIndex, Vector2> = HashMap()
    val wireConsumption: HashMap<BlockBoxIndex, Vector2> = HashMap()

    // these are NOT constant so they ARE serialised. Type: Map<SinkType (String) -> Charge (Double>
    // Use case: signal buffer (sinkType=digital_bit), battery (sinkType=electricity), etc.
    val chargeStored: HashMap<String, Double> = HashMap()

    private val newStates = HashMap<BlockBoxIndex, Vector2>()

    /** Triggered when 'digital_bit' rises from low to high. Edge detection only considers the real component (labeled as 'x') of the vector */
    open fun onRisingEdge(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' rises from high to low. Edge detection only considers the real component (labeled as 'x') of the vector */
    open fun onFallingEdge(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' is held high. This function WILL NOT be triggered simultaneously with the rising edge. Level detection only considers the real component (labeled as 'x') of the vector */
    //open fun onSignalHigh(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' is held low. This function WILL NOT be triggered simultaneously with the falling edge. Level detection only considers the real component (labeled as 'x') of the vector */
    //open fun onSignalLow(readFrom: BlockBoxIndex) {}

    open fun updateSignal() {}

    fun getWireStateAt(offsetX: Int, offsetY: Int, sinkType: WireEmissionType): Vector2 {
        val wx = offsetX + worldBlockPos!!.x
        val wy = offsetY + worldBlockPos!!.y

        return WireCodex.getAllWiresThatAccepts(sinkType).fold(Vector2()) { acc, (id, _) ->
            INGAME.world.getWireEmitStateOf(wx, wy, id).let {
                Vector2(acc.x + (it?.x ?: 0.0), acc.y + (it?.y ?: 0.0))
            }
        }
    }

    fun getWireEmissionAt(offsetX: Int, offsetY: Int): Vector2 {
        return wireEmission[pointToBlockBoxIndex(offsetX, offsetY)] ?: Vector2()
    }

    /**
     * returns true if at least one of following condition is `true`
     * - `getWireStateAt(x, y, "digital_bit").x` is equal to or greater than `ELECTIC_THRESHOLD_HIGH`
     * - `getWireEmissionAt(x, y).x` is equal to or greater than `ELECTIC_THRESHOLD_HIGH`
     *
     * This function does NOT check if the given port receives/emits `digital_bit` signal; if not, the result is undefined.
     */
    fun isSignalHigh(offsetX: Int, offsetY: Int) =
        getWireStateAt(offsetX, offsetY, "digital_bit").x >= ELECTRIC_THRESHOLD_HIGH ||
        getWireEmissionAt(offsetX, offsetY).x >= ELECTRIC_THRESHOLD_HIGH

    /**
     * returns true if at least one of following condition is `true`
     * - `getWireStateAt(x, y, "digital_bit").x` is equal to or lesser than `ELECTRIC_THRESHOLD_LOW`
     * - `getWireEmissionAt(x, y).x` is equal to or lesser than `ELECTRIC_THRESHOLD_LOW`
     *
     * This function does NOT check if the given port receives/emits `digital_bit` signal; if not, the result is undefined.
     */
    fun isSignalLow(offsetX: Int, offsetY: Int) =
        getWireStateAt(offsetX, offsetY, "digital_bit").x <= ELECTRIC_THRESHOLD_LOW ||
        getWireEmissionAt(offsetX, offsetY).x <= ELECTRIC_THRESHOLD_LOW

    protected var oldSinkStatus: Array<Vector2>
//    protected var newSinkStatus: Array<Vector2>

    open fun updateOnWireGraphTraversal(offsetX: Int, offsetY: Int, sinkType: WireEmissionType) {
        val index = pointToBlockBoxIndex(offsetX, offsetY)
        val wx = offsetX + worldBlockPos!!.x
        val wy = offsetY + worldBlockPos!!.y

        val new2 = WireCodex.getAllWiresThatAccepts(wireSinkTypes[index] ?: "").fold(Vector2()) { acc, (id, _) ->
            INGAME.world.getWireEmitStateOf(wx, wy, id).let {
                Vector2(acc.x + (it?.x ?: 0.0), acc.y + (it?.y ?: 0.0))
            }
        }
    }

    /**
     * Refrain from updating signal output from this function: there will be 1 extra tick delay if you do so
     */
    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        val risingEdgeIndices = ArrayList<BlockBoxIndex>()
        val fallingEdgeIndices = ArrayList<BlockBoxIndex>()

        for (y in 0 until blockBox.height) {
            for (x in 0 until blockBox.width) {
                // get indices of "rising edges"
                // get indices of "falling edges"
                val index = pointToBlockBoxIndex(x, y)
                val type = getWireSinkAt(index) ?: ""

                if (type.isNotBlank()) {
                    val old = oldSinkStatus[index]
                    val new = getWireStateAt(x, y, type)

                    val wx = x + worldBlockPos!!.x
                    val wy = y + worldBlockPos!!.y

                    println("Wxy($wx,$wy) getWireState($type)=$new, oldState($type)=$old")

                    if (new.x - old.x >= ELECTRIC_THRESHOLD_EDGE_DELTA && new.x >= ELECTRIC_THRESHOLD_HIGH)
                        risingEdgeIndices.add(index)
                    else if (old.x - new.x >= ELECTRIC_THRESHOLD_EDGE_DELTA && new.x <= ELECTRIC_THRESHOLD_LOW)
                        fallingEdgeIndices.add(index)


                    oldSinkStatus[index].set(new)
                }
            }
        }

        if (risingEdgeIndices.isNotEmpty()) {
            println("risingEdgeIndices=$risingEdgeIndices")
        }

        risingEdgeIndices.forEach { onRisingEdge(it) }
        fallingEdgeIndices.forEach { onFallingEdge(it) }
        updateSignal()
    }
}