
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameactors.WireActor
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.util.SortedArrayList
import org.dyn4j.geometry.Vector2
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.absoluteValue
import kotlin.math.sign

typealias BlockAddress = Long

open class GameWorld : Disposable {

    var worldName: String = "New World"
    /** Index start at 1 */
    var worldIndex: Int
        set(value) {
            if (value <= 0)
                throw Error("World index start at 1; you've entered $value")

            printdbg(this, "Creation of new world with index $value, called by:")
            printStackTrace(this)

            field = value
        }
    val width: Int
    val height: Int

    open val creationTime: Long
    open var lastPlayTime: Long
        internal set // there's a case of save-and-continue-playing
    open var totalPlayTime: Int
        internal set

    /** Used to calculate play time */
    open val loadTime: Long = System.currentTimeMillis() / 1000L

    //layers
    @TEMzPayload("WALL", TEMzPayload.INT16_LITTLE)
    val layerWall: BlockLayer
    @TEMzPayload("TERR", TEMzPayload.INT16_LITTLE)
    val layerTerrain: BlockLayer
    //val layerWire: MapLayer

    //val layerThermal: MapLayerHalfFloat // in Kelvins
    //val layerFluidPressure: MapLayerHalfFloat // (milibar - 1000)

    /** Tilewise spawn point */
    open var spawnX: Int
    /** Tilewise spawn point */
    open var spawnY: Int

    @TEMzPayload("WdMG", TEMzPayload.INT48_FLOAT_PAIR)
    val wallDamages: HashMap<BlockAddress, Float>
    @TEMzPayload("TdMG", TEMzPayload.INT48_FLOAT_PAIR)
    val terrainDamages: HashMap<BlockAddress, Float>
    @TEMzPayload("FlTP", TEMzPayload.INT48_SHORT_PAIR)
    val fluidTypes: HashMap<BlockAddress, FluidType>
    @TEMzPayload("FlFL", TEMzPayload.INT48_FLOAT_PAIR)
    val fluidFills: HashMap<BlockAddress, Float>

    /**
     * Single block can have multiple conduits, different types of conduits are stored separately.
     */
    @TEMzPayload("WiNt", TEMzPayload.EXTERNAL_JSON)
    private val wirings: HashMap<BlockAddress, WiringNode>

    private val wiringGraph = HashMap<BlockAddress, HashMap<ItemID, WiringSimCell>>()
    private val WIRE_POS_MAP = intArrayOf(1,2,4,8)
    private val WIRE_ANTIPOS_MAP = intArrayOf(4,8,1,2)

    /**
     * Used by the renderer. When wirings are updated, `wirings` and this properties must be synchronised.
     */
    //private val wiringBlocks: HashMap<BlockAddress, ItemID>

    //public World physWorld = new World( new Vec2(0, -Terrarum.game.gravitationalAccel) );
    //physics
    /** Meter per second squared. Currently only the downward gravity is supported. No reverse gravity :p */
    open var gravitation: Vector2 = DEFAULT_GRAVITATION
    /** 0.0..1.0+ */
    open var globalLight = Cvec(0f, 0f, 0f, 0f)
    open var averageTemperature = 288f // 15 deg celsius; simulates global warming


    open var generatorSeed: Long = 0
        internal set

    var disposed = false
        private set

    val worldTime: WorldTime = WorldTime( // Year EPOCH (125), Month 1, Day 1 is implied
            7 * WorldTime.HOUR_SEC +
            30L * WorldTime.MINUTE_SEC
    )


    @TEMzPayload("TMaP", TEMzPayload.EXTERNAL_JSON)
    val tileNumberToNameMap: HashMap<Int, ItemID>
    // does not go to the savefile
    val tileNameToNumberMap: HashMap<ItemID, Int>

    /**
     * Create new world
     */
    constructor(worldIndex: Int, width: Int, height: Int, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) {
        if (width <= 0 || height <= 0) throw IllegalArgumentException("Non-positive width/height: ($width, $height)")

        this.worldIndex = worldIndex
        this.width = width
        this.height = height

        this.spawnX = width / 2
        this.spawnY = 200

        layerTerrain = BlockLayer(width, height)
        layerWall = BlockLayer(width, height)
        //layerWire = MapLayer(width, height)

        wallDamages = HashMap()
        terrainDamages = HashMap()
        fluidTypes = HashMap()
        fluidFills = HashMap()

        //wiringBlocks = HashMap()
        wirings = HashMap()

        // temperature layer: 2x2 is one cell
        //layerThermal = MapLayerHalfFloat(width, height, averageTemperature)

        // fluid pressure layer: 4 * 8 is one cell
        //layerFluidPressure = MapLayerHalfFloat(width, height, 13f) // 1013 mBar


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T
        this.totalPlayTime = totalPlayTime


        tileNumberToNameMap = HashMap<Int, ItemID>()
        tileNameToNumberMap = HashMap<ItemID, Int>()
        AppLoader.tileMaker.tags.forEach {
            printdbg(this, "tileNumber ${it.value.tileNumber} <-> tileName ${it.key}")

            tileNumberToNameMap[it.value.tileNumber] = it.key
            tileNameToNumberMap[it.key] = it.value.tileNumber
        }

        // AN EXCEPTIONAL TERM: tilenum 0 is always redirected to Air tile, even if the tilenum for actual Air tile is not zero
        tileNumberToNameMap[0] = Block.AIR
    }

    /**
     * Load existing world
     */
    /*internal constructor(worldIndex: Int, json: JsonObject) {
        this.worldIndex = worldIndex

        // TODO setup layerTerrain, layerWall, etc. from the json

        // before the renaming, update the name maps
        tileNumberToNameMap = HashMap<Int, ItemID>()
        tileNameToNumberMap = HashMap<ItemID, Int>()
        AppLoader.tileMaker.tags.forEach {
            tileNumberToNameMap[it.value.tileNumber] = it.key
            tileNameToNumberMap[it.key] = it.value.tileNumber
        }

        // perform renaming of tile layers
        val oldTileNumberToNameMap = /* todo */
        for (y in 0 until layerTerrain.height) {
            for (x in 0 until layerTerrain.width) {
                layerTerrain.unsafeSetTile(x, y, tileNameToNumberMap[oldTileNumberToNameMap[layerTerrain.unsafeGetTile(x, y)]]!!)
                layerWall.unsafeSetTile(x, y, tileNameToNumberMap[oldTileNumberToNameMap[layerWall.unsafeGetTile(x, y)]]!!)
                // TODO rename fluid map
                // TODO rename wire map
            }
        }

        // AN EXCEPTIONAL TERM: tilenum 0 is always redirected to Air tile, even if the tilenum for actual Air tile is not zero
        tileNumberToNameMap[0] = Block.AIR
    }*/

    /**
     * Get 2d array data of wire

     * @return byte[][] wire layer
     */
    //val wireArray: ByteArray
    //    get() = layerWire.data

    fun coerceXY(x: Int, y: Int) = (x fmod width) to (y.coerceIn(0, height - 1))

    /**
     * @return ItemID, WITHOUT wall tag
     */
    fun getTileFromWall(rawX: Int, rawY: Int): ItemID {
        val (x, y) = coerceXY(rawX, rawY)
        return tileNumberToNameMap[layerWall.unsafeGetTile(x, y)]!!
    }

    /**
     * @return ItemID
     */
    fun getTileFromTerrain(rawX: Int, rawY: Int): ItemID {
        val (x, y) = coerceXY(rawX, rawY)

        try {
            return tileNumberToNameMap[layerTerrain.unsafeGetTile(x, y)]!!
        }
        catch (e: NullPointerException) {
            System.err.println("NPE for tilenum ${layerTerrain.unsafeGetTile(x, y)}")
            throw e
        }
    }

    /**
     * @return Int
     */
    fun getTileNumFromWall(rawX: Int, rawY: Int): Int {
        val (x, y) = coerceXY(rawX, rawY)
        return layerWall.unsafeGetTile(x, y)
    }

    /**
     * @return Int
     */
    fun getTileNumFromTerrain(rawX: Int, rawY: Int): Int {
        val (x, y) = coerceXY(rawX, rawY)
        return layerTerrain.unsafeGetTile(x, y)
    }

    fun getTileFromWallRaw(coercedX: Int, coercedY: Int) = layerWall.unsafeGetTile(coercedX, coercedY)
    fun getTileFromTerrainRaw(coercedX: Int, coercedY: Int) = layerTerrain.unsafeGetTile(coercedX, coercedY)

    /**
     * Set the tile of wall as specified, with damage value of zero.
     * @param x
     * *
     * @param y
     * *
     * @param itemID Tile as in ItemID, with tag removed!
     */
    fun setTileWall(x: Int, y: Int, itemID: ItemID, bypassEvent: Boolean) {
        val (x, y) = coerceXY(x, y)
        val tilenum = tileNameToNumberMap[itemID]!!

        val oldWall = getTileFromWall(x, y)
        layerWall.unsafeSetTile(x, y, tilenum)
        wallDamages.remove(LandUtil.getBlockAddr(this, x, y))

        if (!bypassEvent)
            Terrarum.ingame?.queueWallChangedEvent(oldWall, itemID, x, y)
    }

    /**
     * Set the tile of wall as specified, with damage value of zero.
     *
     * Warning: this function alters fluid lists: be wary of call order!
     *
     * @param x
     * *
     * @param y
     * *
     * @param itemID Tile as in ItemID, with tag removed!
     */
    fun setTileTerrain(x: Int, y: Int, itemID: ItemID, bypassEvent: Boolean) {
        val (x, y) = coerceXY(x, y)
        val tilenum = tileNameToNumberMap[itemID]!!

        val oldTerrain = getTileFromTerrain(x, y)
        layerTerrain.unsafeSetTile(x, y, tilenum)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        terrainDamages.remove(blockAddr)

        if (BlockCodex[itemID].isSolid) {
            fluidFills.remove(blockAddr)
            fluidTypes.remove(blockAddr)
        }
        // fluid tiles-item should be modified so that they will also place fluid onto their respective map

        if (!bypassEvent)
            Terrarum.ingame?.queueTerrainChangedEvent(oldTerrain, itemID, x, y)
    }

    fun setTileWire(x: Int, y: Int, tile: ItemID, bypassEvent: Boolean) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        val wireNode = wirings[blockAddr]

        if (wireNode == null) {
            wirings[blockAddr] = WiringNode(blockAddr, SortedArrayList())
        }

        wirings[blockAddr]!!.wires.add(tile)

        if (!bypassEvent)
            Terrarum.ingame?.queueWireChangedEvent(tile, false, x, y)


        // figure out wiring graphs
        val matchingNeighbours = WireActor.WIRE_NEARBY.mapIndexed { index, (tx, ty) ->
            (getAllWiresFrom(x + tx, y + ty)?.contains(tile) == true).toInt() shl index
        }.sum()
        // setup graph of mine
        setWireGraphOfUnsafe(blockAddr, tile, matchingNeighbours)
        // setup graph for neighbours
        for (i in 0..3) {
            if (matchingNeighbours and WIRE_POS_MAP[i] > 0) {
                val (tx, ty) = WireActor.WIRE_NEARBY[i]
                val old = getWireGraphOf(x + tx, y + ty, tile) ?: 0
                setWireGraphOf(x + tx, y + ty, tile, old or WIRE_ANTIPOS_MAP[i])
            }
        }
    }

    fun getWireGraphOf(x: Int, y: Int, itemID: ItemID): Int? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireGraphUnsafe(blockAddr, itemID)
    }

    fun getWireGraphUnsafe(blockAddr: BlockAddress, itemID: ItemID): Int? {
        return wiringGraph[blockAddr]?.get(itemID)?.connections
    }

    fun getWireEmitStateOf(x: Int, y: Int, itemID: ItemID): Vector2? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireEmitStateUnsafe(blockAddr, itemID)
    }

    fun getWireEmitStateUnsafe(blockAddr: BlockAddress, itemID: ItemID): Vector2? {
        return wiringGraph[blockAddr]?.get(itemID)?.emitState
    }

    fun getWireRecvStateOf(x: Int, y: Int, itemID: ItemID): ArrayList<WireRecvState>? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireRecvStateUnsafe(blockAddr, itemID)
    }

    fun getWireRecvStateUnsafe(blockAddr: BlockAddress, itemID: ItemID): ArrayList<WireRecvState>? {
        return wiringGraph[blockAddr]?.get(itemID)?.recvStates
    }

    fun setWireGraphOf(x: Int, y: Int, itemID: ItemID, cnx: Int) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return setWireGraphOfUnsafe(blockAddr, itemID, cnx)
    }

    fun setWireGraphOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, cnx: Int) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = HashMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(cnx)

        wiringGraph[blockAddr]!![itemID]!!.connections = cnx
    }

    fun setWireEmitStateOf(x: Int, y: Int, itemID: ItemID, vector: Vector2) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return setWireEmitStateOfUnsafe(blockAddr, itemID, vector)
    }

    fun setWireEmitStateOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, vector: Vector2) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = HashMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(0, vector)

        wiringGraph[blockAddr]!![itemID]!!.emitState = vector
    }

    fun addWireRecvStateOf(x: Int, y: Int, itemID: ItemID, state: WireRecvState) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return addWireRecvStateOfUnsafe(blockAddr, itemID, state)
    }

    fun clearAllWireRecvState(x: Int, y: Int) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return clearAllWireRecvStateUnsafe(blockAddr)
    }

    fun addWireRecvStateOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, state: WireRecvState) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = HashMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(0)

        wiringGraph[blockAddr]!![itemID]!!.recvStates.add(state)
    }

    fun getAllWiringGraph(x: Int, y: Int): HashMap<ItemID, WiringSimCell>? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getAllWiringGraphUnsafe(blockAddr)
    }

    fun getAllWiringGraphUnsafe(blockAddr: BlockAddress): HashMap<ItemID, WiringSimCell>? {
        return wiringGraph[blockAddr]
    }

    fun clearAllWireRecvStateUnsafe(blockAddr: BlockAddress) {
        wiringGraph[blockAddr]?.forEach {
            it.value.recvStates.clear()
        }
    }

    fun getAllWiresFrom(x: Int, y: Int): SortedArrayList<ItemID>? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getAllWiresFrom(blockAddr)
    }

    fun getAllWiresFrom(blockAddr: BlockAddress): SortedArrayList<ItemID>? {
        return wirings[blockAddr]?.wires
    }

    fun getTileFrom(mode: Int, x: Int, y: Int): ItemID {
        if (mode == TERRAIN) {
            return getTileFromTerrain(x, y)
        }
        else if (mode == WALL) {
            return getTileFromWall(x, y)
        }
        else
            throw IllegalArgumentException("illegal mode input: " + mode.toString())
    }

    fun terrainIterator(): Iterator<ItemID> {
        return object : Iterator<ItemID> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): ItemID {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return getTileFromTerrain(x, y)
            }

        }
    }

    fun wallIterator(): Iterator<ItemID> {
        return object : Iterator<ItemID> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean =
                    iteratorCount < width * height

            override fun next(): ItemID {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return getTileFromWall(x, y)
            }

        }
    }

    /**
     * @return true if block is broken
     */
    fun inflictTerrainDamage(x: Int, y: Int, damage: Double): ItemID? {
        val damage = damage.toFloat()
        val addr = LandUtil.getBlockAddr(this, x, y)

        //println("[GameWorld] ($x, $y) Damage: $damage")

        if (terrainDamages[addr] == null) { // add new
            terrainDamages[addr] = damage
        }
        else if (terrainDamages[addr]!! + damage <= 0) { // tile is (somehow) fully healed
            terrainDamages.remove(addr)
        }
        else { // normal situation
            terrainDamages[addr] = terrainDamages[addr]!! + damage
        }

        //println("[GameWorld] accumulated damage: ${terrainDamages[addr]}")

        // remove tile from the world
        if (terrainDamages[addr] ?: 0f >= BlockCodex[getTileFromTerrain(x, y)].strength) {
            val tileBroke = getTileFromTerrain(x, y)
            setTileTerrain(x, y, Block.AIR, false)
            terrainDamages.remove(addr)
            return tileBroke
        }

        return null
    }
    fun getTerrainDamage(x: Int, y: Int): Float =
            terrainDamages[LandUtil.getBlockAddr(this, x, y)] ?: 0f

    /**
     * @return true if block is broken
     */
    fun inflictWallDamage(x: Int, y: Int, damage: Double): ItemID? {
        val damage = damage.toFloat()
        val addr = LandUtil.getBlockAddr(this, x, y)

        if (wallDamages[addr] == null) { // add new
            wallDamages[addr] = damage
        }
        else if (wallDamages[addr]!! + damage <= 0) { // tile is (somehow) fully healed
            wallDamages.remove(addr)
        }
        else { // normal situation
            wallDamages[addr] = wallDamages[addr]!! + damage
        }

        // remove tile from the world
        if (wallDamages[addr]!! >= BlockCodex[getTileFromWall(x, y)].strength) {
            val tileBroke = getTileFromWall(x, y)
            setTileWall(x, y, Block.AIR, false)
            wallDamages.remove(addr)
            return tileBroke
        }

        return null
    }
    fun getWallDamage(x: Int, y: Int): Float =
            wallDamages[LandUtil.getBlockAddr(this, x, y)] ?: 0f

    fun setFluid(x: Int, y: Int, fluidType: FluidType, fill: Float) {
        /*if (x == 60 && y == 256) {
            printdbg(this, "Setting fluid $fill at ($x,$y)")
        }*/


        if (fluidType == Fluid.NULL && fill != 0f) {
            throw Error("Illegal fluid at ($x,$y): ${FluidInfo(fluidType, fill)}")
        }


        val addr = LandUtil.getBlockAddr(this, x, y)

        if (fill > WorldSimulator.FLUID_MIN_MASS) {
            //setTileTerrain(x, y, fluidTypeToBlock(fluidType))
            fluidFills[addr] = fill
            fluidTypes[addr] = fluidType
        }
        else {
            fluidFills.remove(addr)
            fluidTypes.remove(addr)

        }


        /*if (x == 60 && y == 256) {
            printdbg(this, "TileTerrain: ${getTileFromTerrain(x, y)}")
            printdbg(this, "fluidTypes[$addr] = ${fluidTypes[addr]} (should be ${fluidType.value})")
            printdbg(this, "fluidFills[$addr] = ${fluidFills[addr]} (should be $fill)")
        }*/
    }

    fun getFluid(x: Int, y: Int): FluidInfo {
        val addr = LandUtil.getBlockAddr(this, x, y)
        val fill = fluidFills[addr]
        val type = fluidTypes[addr]
        return if (type == null) FluidInfo(Fluid.NULL, 0f) else FluidInfo(type, fill!!)
    }

    private fun fluidTypeToBlock(type: FluidType) = when (type.abs()) {
        Fluid.NULL.value -> Block.AIR
        in Fluid.fluidRange -> GameWorld.TILES_SUPPORTED - type.abs()
        else -> throw IllegalArgumentException("Unsupported fluid type: $type")
    }

    data class FluidInfo(val type: FluidType, val amount: Float) {
        /** test if this fluid should be considered as one */
        fun isFluid() = type != Fluid.NULL && amount >= WorldSimulator.FLUID_MIN_MASS
        fun getProp() = BlockCodex[type]
        override fun toString() = "Fluid type: ${type.value}, amount: $amount"
    }

    /**
     * Connection rules: connect to all nearby, except:
     *
     * If the wire allows 3- or 4-way connection, make such connection.
     * If the wire does not allow them (e.g. wire bridge, thicknet), connect top-bottom and left-right nodes.
     */
    data class WiringNode(
            val position: BlockAddress, // may seem redundant and it kinda is, but don't remove!
            val wires: SortedArrayList<ItemID> // what could possibly go wrong bloating up the RAM footprint when it's practically infinite these days?
    ) : Comparable<WiringNode> {
        override fun compareTo(other: WiringNode): Int {
            return (this.position - other.position).sign
        }
    }

    data class WireRecvState(
            var dist: Int, // how many tiles it took to traverse
            var src: Point2i // xy position
            // to get the state, use the src to get the state of the source emitter directly, then use dist to apply attenuation
    )

    /**
     * These values must be updated by none other than [WorldSimulator]()
     */
    data class WiringSimCell(
            var connections: Int = 0, // connections
            var emitState: Vector2 = Vector2(0.0, 0.0), // i'm emitting this much power
            var recvStates: ArrayList<WireRecvState> = ArrayList() // how far away are the power sources
    )

    fun getTemperature(worldTileX: Int, worldTileY: Int): Float? {
        return null
    }

    fun getAirPressure(worldTileX: Int, worldTileY: Int): Float? {
        return null
    }

    override fun dispose() {
        layerWall.dispose()
        layerTerrain.dispose()
        //nullWorldInstance?.dispose() // must be called ONLY ONCE; preferably when the app exits

        disposed = true
    }

    override fun equals(other: Any?) = layerTerrain.ptr == (other as GameWorld).layerTerrain.ptr

    companion object {
        @Transient const val WALL = 0
        @Transient const val TERRAIN = 1
        @Transient const val WIRE = 2

        @Transient val TILES_SUPPORTED = ReferencingRanges.TILES.last + 1
        //@Transient val SIZEOF: Byte = 2
        @Transient const val LAYERS: Byte = 4 // terrain, wall (layerTerrainLowBits + layerWallLowBits), wire

        @Transient private var nullWorldInstance: GameWorld? = null

        fun makeNullWorld(): GameWorld {
            if (nullWorldInstance == null)
                nullWorldInstance = GameWorld(1, 1, 1, 0, 0, 0)

            return nullWorldInstance!!
        }

        val DEFAULT_GRAVITATION = Vector2(0.0, 9.8)
    }

    open fun updateWorldTime(delta: Float) {
        worldTime.update(delta)
    }
}

infix fun Int.fmod(other: Int) = Math.floorMod(this, other)
infix fun Long.fmod(other: Long) = Math.floorMod(this, other)
infix fun Float.fmod(other: Float) = if (this >= 0f) this % other else (this % other) + other

inline class FluidType(val value: Int) {
    infix fun sameAs(other: FluidType) = this.value.absoluteValue == other.value.absoluteValue
    fun abs() = this.value.absoluteValue
}

/**
 * @param payloadName Payload name defined in Map Data Format.txt
 * * 4 Letters: regular payload
 * * 3 Letters: only valid for arrays with 16 elements, names are auto-generated by appending '0'..'9'+'a'..'f'. E.g.: 'CfL' turns into 'CfL0', 'CfL1' ... 'CfLe', 'CfLf'
 *
 * @param arg 0 for 8 MSBs of Terrain/Wall layer, 1 for 4 LSBs of Terrain/Wall layer, 2 for Int48-Float pair, 3 for Int48-Short pair, 4 for Int48-Int pair
 */
annotation class TEMzPayload(val payloadName: String, val arg: Int) {
    companion object {
        const val EXTERNAL_JAVAPROPERTIES = -3
        const val EXTERNAL_CSV = -2
        const val EXTERNAL_JSON = -1
        const val INT16_LITTLE = 1
        const val INT48_FLOAT_PAIR = 2
        const val INT48_SHORT_PAIR = 3
        const val INT48_INT_PAIR = 4
    }
}
