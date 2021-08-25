
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.compression.Lzma
import net.torvald.UnsafePtr
import net.torvald.UnsafePtrInputStream
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameactors.WireActor
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.Ascii85
import net.torvald.terrarum.serialise.bytesToLzmadStr
import net.torvald.terrarum.serialise.bytesToZipdStr
import net.torvald.util.SortedArrayList
import org.apache.commons.codec.digest.DigestUtils
import org.dyn4j.geometry.Vector2
import java.util.zip.GZIPOutputStream
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.absoluteValue
import kotlin.math.sign

typealias BlockAddress = Long

class GameWorld : Disposable {

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

    var creationTime: Long
        internal set
    var lastPlayTime: Long
        internal set // there's a case of save-and-continue-playing
    var totalPlayTime: Int
        internal set

    /** Used to calculate play time */
    @Transient open val loadTime: Long = System.currentTimeMillis() / 1000L

    //layers
    lateinit var layerWall: BlockLayer
    lateinit var layerTerrain: BlockLayer

    //val layerThermal: MapLayerHalfFloat // in Kelvins
    //val layerFluidPressure: MapLayerHalfFloat // (milibar - 1000)

    /** Tilewise spawn point */
    var spawnX: Int
    /** Tilewise spawn point */
    var spawnY: Int

    val wallDamages = HashMap<BlockAddress, Float>()
    val terrainDamages = HashMap<BlockAddress, Float>()
    val fluidTypes = HashMap<BlockAddress, FluidType>()
    val fluidFills = HashMap<BlockAddress, Float>()

    /**
     * Single block can have multiple conduits, different types of conduits are stored separately.
     */
    private val wirings = HashMap<BlockAddress, WiringNode>()

    private val wiringGraph = HashMap<BlockAddress, HashMap<ItemID, WiringSimCell>>()
    @Transient private val WIRE_POS_MAP = intArrayOf(1,2,4,8)
    @Transient private val WIRE_ANTIPOS_MAP = intArrayOf(4,8,1,2)

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

    @Transient var disposed = false
        private set

    val worldTime: WorldTime = WorldTime( // Year EPOCH (125), Month 1, Day 1 is implied
            7 * WorldTime.HOUR_SEC +
            30L * WorldTime.MINUTE_SEC
    )


    val tileNumberToNameMap = HashMap<Int, ItemID>()
    // does not go to the savefile
    @Transient val tileNameToNumberMap = HashMap<ItemID, Int>()

    val extraFields = HashMap<String, Any?>()

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

        // temperature layer: 2x2 is one cell
        //layerThermal = MapLayerHalfFloat(width, height, averageTemperature)

        // fluid pressure layer: 4 * 8 is one cell
        //layerFluidPressure = MapLayerHalfFloat(width, height, 13f) // 1013 mBar


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T
        this.totalPlayTime = totalPlayTime

        postLoad()
    }

    constructor() {
        worldIndex = 1234567890
        width = 999
        height = 999
        val time = AppLoader.getTIME_T()
        creationTime = time
        lastPlayTime = time
        totalPlayTime = 0
        spawnX = 0
        spawnY = 0
    }

    fun postLoad() {
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

        try {
            return tileNumberToNameMap[layerWall.unsafeGetTile(x, y)]!!
        }
        catch (e: NullPointerException) {
            System.err.println("NPE for wall ${layerWall.unsafeGetTile(x, y)} in ($x, $y)")
            throw e
        }
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
            System.err.println("NPE for terrain ${layerTerrain.unsafeGetTile(x, y)} in ($x, $y)")
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

    data class FluidInfo(val type: FluidType = Fluid.NULL, val amount: Float = 0f) {
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
            val position: BlockAddress = -1, // may seem redundant and it kinda is, but don't remove!
            val wires: SortedArrayList<ItemID> = SortedArrayList<ItemID>() // what could possibly go wrong bloating up the RAM footprint when it's practically infinite these days?
    ) : Comparable<WiringNode> {
        override fun compareTo(other: WiringNode): Int {
            return (this.position - other.position).sign
        }
    }

    data class WireRecvState(
            var dist: Int = -1, // how many tiles it took to traverse
            var src: Point2i = Point2i(0,0) // xy position
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

    /**
     * Returns lines that are part of the entire JSON
     *
     * To extend this function, you can code something like this:
     * ```
     * return super.getJsonFields() + arrayListOf(
     *     """"<myModuleName>.<myNewObject>": ${Json(JsonWriter.OutputType.json).toJson(<myNewObject>)}"""
     * )
     * ```
     */
    open fun getJsonFields(): List<String> {
        fun Byte.tostr() = this.toInt().and(255).toString(16).padStart(2,'0')

        val tdmgstr = Json(JsonWriter.OutputType.json).toJson(terrainDamages)
        val wdmgstr = Json(JsonWriter.OutputType.json).toJson(wallDamages)
        val flutstr = Json(JsonWriter.OutputType.json).toJson(fluidTypes)
        val flufstr = Json(JsonWriter.OutputType.json).toJson(fluidFills)
        val wirestr = Json(JsonWriter.OutputType.json).toJson(wirings)
        val wirgstr = Json(JsonWriter.OutputType.json).toJson(wiringGraph)

        val digester = DigestUtils.getSha256Digest()

        layerTerrain.bytesIterator().forEachRemaining { digester.update(it) }
        val terrhash = StringBuilder().let { sb -> digester.digest().forEach { sb.append(it.tostr()) }; sb.toString() }
        layerWall.bytesIterator().forEachRemaining { digester.update(it) }
        val wallhash = StringBuilder().let { sb -> digester.digest().forEach { sb.append(it.tostr()) }; sb.toString() }

        // use gzip; lzma's slower and larger for some reason
        return arrayListOf(
                """"worldname": "$worldName"""",
                """"comp": "gzip"""",
                """"width": $width""",
                """"height": $height""",
                """"spawnx": $spawnX""",
                """"spawny": $spawnY""",
                """"genver": 4""",
                """"time_t": ${worldTime.TIME_T}""",
                """"terr": {
                    |"h": "$terrhash",
                    |"b": "${blockLayerToStr(layerTerrain)}"}""".trimMargin(),
                """"wall": {
                    |"h": "$wallhash",
                    |"b": "${blockLayerToStr(layerWall)}"}""".trimMargin(),
                """"tdmg": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(tdmgstr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(tdmgstr.toByteArray())}"}""".trimMargin(),
                """"wdmg": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(wdmgstr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(wdmgstr.toByteArray())}"}""".trimMargin(),
                """"flut": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(flutstr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(flutstr.toByteArray())}"}""".trimMargin(),
                """"fluf": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(flufstr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(flufstr.toByteArray())}"}""".trimMargin(),
                """"wire": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(wirestr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(wirestr.toByteArray())}"}""".trimMargin(),
                """"wirg": {
                    |"h": "${StringBuilder().let { sb -> digester.digest(wirgstr.toByteArray()).forEach { sb.append(it.tostr()) }; sb.toString() }}",
                    |"b": "${bytesToZipdStr(wirgstr.toByteArray())}"}""".trimMargin()
        )
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
 * @param b a BlockLayer
 * @return Bytes in [b] which are GZip'd then Ascii85-encoded
 */
fun blockLayerToStr(b: BlockLayer): String {
    val sb = StringBuilder()
    val bo = ByteArray64GrowableOutputStream()
    val zo = GZIPOutputStream(bo)

    b.bytesIterator().forEachRemaining {
        zo.write(it.toInt())
    }
    zo.flush(); zo.close()

    val ba = bo.toByteArray64()
    var bai = 0
    val buf = IntArray(4) { Ascii85.PAD_BYTE }
    ba.forEach {
        if (bai > 0 && bai % 4 == 0) {
            sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
            buf.fill(Ascii85.PAD_BYTE)
        }

        buf[bai % 4] = it.toInt() and 255

        bai += 1
    }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

    return sb.toString()
}

/**
 * @param b a BlockLayer
 * @return Bytes in [b] which are LZMA'd then Ascii85-encoded
 */
fun blockLayerToStr2(b: BlockLayer): String {
    val sb = StringBuilder()
    val bi = UnsafePtrInputStream(b.ptr)
    val bo = ByteArray64GrowableOutputStream()

    Lzma.compress(bi, bo); bo.flush(); bo.close()

    val ba = bo.toByteArray64()
    var bai = 0
    val buf = IntArray(4) { Ascii85.PAD_BYTE }
    ba.forEach {
        if (bai > 0 && bai % 4 == 0) {
            sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
            buf.fill(Ascii85.PAD_BYTE)
        }

        buf[bai % 4] = it.toInt() and 255

        bai += 1
    }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

    return sb.toString()
}