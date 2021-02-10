
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.printStackTrace
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.ReadLayerDataZip
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.util.SortedArrayList
import org.dyn4j.geometry.Vector2
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
    private val wirings: HashMap<BlockAddress, SortedArrayList<WiringNode>>

    /**
     * Used by the renderer. When wirings are updated, `wirings` and this properties must be synchronised.
     */
    private val wiringBlocks: HashMap<BlockAddress, ItemID>

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

    /** time in (preferably) seconds */
    open var TIME_T: Long = 0L
    open var dayLength: Int = 86400

    @TEMzPayload("TMaP", TEMzPayload.EXTERNAL_JSON)
    val tileNumberToNameMap: HashMap<Int, ItemID>
    // does not go to the savefile
    val tileNameToNumberMap: HashMap<ItemID, Int>


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

        wiringBlocks = HashMap()
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
        CreateTileAtlas.tags.forEach {
            tileNumberToNameMap[it.value.tileNumber] = it.key
            tileNameToNumberMap[it.key] = it.value.tileNumber
        }
    }

    internal constructor(worldIndex: Int, layerData: ReadLayerDataZip.LayerData, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) {
        this.worldIndex = worldIndex

        layerTerrain = layerData.layerTerrain
        layerWall = layerData.layerWall
        //layerWire = layerData.layerWire

        wallDamages = layerData.wallDamages
        terrainDamages = layerData.terrainDamages
        fluidTypes = layerData.fluidTypes
        fluidFills = layerData.fluidFills

        wiringBlocks = HashMap()
        wirings = HashMap()

        spawnX = layerData.spawnX
        spawnY = layerData.spawnY

        width = layerTerrain.width
        height = layerTerrain.height


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T
        this.totalPlayTime = totalPlayTime

        tileNumberToNameMap = layerData.tileNumberToNameMap

        // TODO perform renaming of tile layers

        // after the renaming, update the name maps
        tileNameToNumberMap = HashMap<ItemID, Int>()
        CreateTileAtlas.tags.forEach {
            tileNumberToNameMap[it.value.tileNumber] = it.key
            tileNameToNumberMap[it.key] = it.value.tileNumber
        }
    }

    /**
     * Get 2d array data of wire

     * @return byte[][] wire layer
     */
    //val wireArray: ByteArray
    //    get() = layerWire.data

    fun coerceXY(x: Int, y: Int) = (x fmod width) to (y.coerceIn(0, height - 1))

    /**
     * @return ItemID
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
        return tileNumberToNameMap[layerTerrain.unsafeGetTile(x, y)]!!
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
     * @param itemID Tile as in ItemID
     */
    fun setTileWall(x: Int, y: Int, itemID: ItemID) {
        val (x, y) = coerceXY(x, y)
        val tilenum = tileNameToNumberMap[itemID]!!

        val oldWall = getTileFromWall(x, y)
        layerWall.unsafeSetTile(x, y, tilenum)
        wallDamages.remove(LandUtil.getBlockAddr(this, x, y))

        Terrarum.ingame?.queueWallChangedEvent(oldWall, itemID, LandUtil.getBlockAddr(this, x, y))
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
     * @param itemID Tile as in ItemID
     */
    fun setTileTerrain(x: Int, y: Int, itemID: ItemID) {
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

        Terrarum.ingame?.queueTerrainChangedEvent(oldTerrain, itemID, LandUtil.getBlockAddr(this, x, y))
    }

    /*fun setTileWire(x: Int, y: Int, tile: Byte) {
        val (x, y) = coerceXY(x, y)
        val oldWire = getTileFromWire(x, y)
        layerWire.setTile(x, y, tile)

        if (oldWire != null)
            Terrarum.ingame?.queueWireChangedEvent(oldWire, tile.toUint(), LandUtil.getBlockAddr(this, x, y))
    }*/

    fun getWiringBlocks(x: Int, y: Int): ItemID {
        return Block.AIR // TODO
        //return wiringBlocks.getOrDefault(LandUtil.getBlockAddr(this, x, y), Block.AIR)
    }

    fun getAllConduitsFrom(x: Int, y: Int): SortedArrayList<WiringNode>? {
        return wirings.get(LandUtil.getBlockAddr(this, x, y))
    }

    /**
     * @param conduitTypeBit defined in net.torvald.terrarum.blockproperties.Wire, always power-of-two
     */
    fun getConduitByTypeFrom(x: Int, y: Int, conduitTypeBit: Int): WiringNode? {
        val conduits = getAllConduitsFrom(x, y)
        return conduits?.searchFor(conduitTypeBit) { it.typeBitMask }
    }

    fun addNewConduitTo(x: Int, y: Int, node: WiringNode) {
        // TODO needs new conduit storage scheme

        /*val blockAddr = LandUtil.getBlockAddr(this, x, y)

        // check for existing type of conduit
        // if there's no duplicate...
        if (getWiringBlocks(x, y) and node.typeBitMask == 0) {
            // store as-is
            wirings.getOrPut(blockAddr) { SortedArrayList() }.add(node)
            // synchronise wiringBlocks
            wiringBlocks[blockAddr] = (wiringBlocks[blockAddr] ?: 0) or node.typeBitMask
        }
        else {
            TODO("need overwriting policy for existing conduit node")
        }*/
    }

    fun getTileFrom(mode: Int, x: Int, y: Int): ItemID {
        if (mode == TERRAIN) {
            return getTileFromTerrain(x, y)
        }
        else if (mode == WALL) {
            return getTileFromWall(x, y)
        }
        else if (mode == WIRE) {
            return getWiringBlocks(x, y)
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

                return getTileFromTerrain(x, y)!!
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

                return getTileFromWall(x, y)!!
            }

        }
    }

    /**
     * @return true if block is broken
     */
    fun inflictTerrainDamage(x: Int, y: Int, damage: Double): Boolean {
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
            setTileTerrain(x, y, Block.AIR)
            terrainDamages.remove(addr)
            return true
        }

        return false
    }
    fun getTerrainDamage(x: Int, y: Int): Float =
            terrainDamages[LandUtil.getBlockAddr(this, x, y)] ?: 0f

    /**
     * @return true if block is broken
     */
    fun inflictWallDamage(x: Int, y: Int, damage: Double): Boolean {
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
            setTileWall(x, y, Block.AIR)
            wallDamages.remove(addr)
            return true
        }

        return false
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
            val position: BlockAddress,
            /** One defined in WireCodex, always power of two */
            val typeBitMask: Int,
            var fills: Float = 0f
    ) : Comparable<WiringNode> {
        override fun compareTo(other: WiringNode): Int {
            return (this.position - other.position).sign
        }
    }

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
