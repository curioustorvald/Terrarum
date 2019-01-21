
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.ReadLayerDataLzma
import org.dyn4j.geometry.Vector2
import kotlin.math.absoluteValue

typealias BlockAddress = Long

open class GameWorld {

    var worldName: String = "New World"
    var worldIndex: Int
    val width: Int
    val height: Int

    val creationTime: Long
    var lastPlayTime: Long
        internal set // there's a case of save-and-continue-playing
    var totalPlayTime: Int
        internal set

    /** Used to calculate play time */
    val loadTime: Long = System.currentTimeMillis() / 1000L

    //layers
    val layerWall: MapLayer
    val layerTerrain: MapLayer
    val layerWire: MapLayer

    val layerWallLowBits: PairedMapLayer
    val layerTerrainLowBits: PairedMapLayer

    //val layerThermal: MapLayerHalfFloat // in Kelvins
    //val layerFluidPressure: MapLayerHalfFloat // (milibar - 1000)

    /** Tilewise spawn point */
    var spawnX: Int
    /** Tilewise spawn point */
    var spawnY: Int

    val wallDamages: HashMap<BlockAddress, Float>
    val terrainDamages: HashMap<BlockAddress, Float>
    val fluidTypes: HashMap<BlockAddress, FluidType>
    val fluidFills: HashMap<BlockAddress, Float>

    //public World physWorld = new World( new Vec2(0, -Terrarum.game.gravitationalAccel) );
    //physics
    /** Some arbitrary and empirical value */
    var gravitation: Vector2 = Vector2(0.0, 0.31)
    /** 0.0..1.0+ */
    var globalLight = Color(0f,0f,0f,0f)
    var averageTemperature = 288f // 15 deg celsius; simulates global warming


    var generatorSeed: Long = 0
        internal set


    constructor(worldIndex: Int, width: Int, height: Int, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) {
        if (width <= 0 || height <= 0) throw IllegalArgumentException("Non-positive width/height: ($width, $height)")

        this.worldIndex = worldIndex
        this.width = width
        this.height = height

        this.spawnX = width / 2
        this.spawnY = 200

        layerTerrain = MapLayer(width, height)
        layerWall = MapLayer(width, height)
        layerWire = MapLayer(width, height)
        layerTerrainLowBits = PairedMapLayer(width, height)
        layerWallLowBits = PairedMapLayer(width, height)

        wallDamages = HashMap<BlockAddress, Float>()
        terrainDamages = HashMap<BlockAddress, Float>()
        fluidTypes = HashMap<BlockAddress, FluidType>()
        fluidFills = HashMap<BlockAddress, Float>()

        // temperature layer: 2x2 is one cell
        //layerThermal = MapLayerHalfFloat(width, height, averageTemperature)

        // fluid pressure layer: 4 * 8 is one cell
        //layerFluidPressure = MapLayerHalfFloat(width, height, 13f) // 1013 mBar


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T
        this.totalPlayTime = totalPlayTime
    }

    internal constructor(worldIndex: Int, layerData: ReadLayerDataLzma.LayerData, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) {
        this.worldIndex = worldIndex

        layerTerrain = layerData.layerTerrain
        layerWall = layerData.layerWall
        layerWire = layerData.layerWire
        layerTerrainLowBits = layerData.layerTerrainLowBits
        layerWallLowBits = layerData.layerWallLowBits

        wallDamages = layerData.wallDamages
        terrainDamages = layerData.terrainDamages
        fluidTypes = layerData.fluidTypes
        fluidFills = layerData.fluidFills

        spawnX = layerData.spawnX
        spawnY = layerData.spawnY

        width = layerTerrain.width
        height = layerTerrain.height


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T
        this.totalPlayTime = totalPlayTime
    }


    /**
     * Get 2d array data of terrain

     * @return byte[][] terrain layer
     */
    val terrainArray: ByteArray
        get() = layerTerrain.data

    /**
     * Get 2d array data of wall

     * @return byte[][] wall layer
     */
    val wallArray: ByteArray
        get() = layerWall.data

    /**
     * Get 2d array data of wire

     * @return byte[][] wire layer
     */
    val wireArray: ByteArray
        get() = layerWire.data

    /**
     * Get paired array data of damage codes.
     * Format: 0baaaabbbb, aaaa for x = 0, 2, 4, ..., bbbb for x = 1, 3, 5, ...
     * @return byte[][] damage code pair
     */
    val damageDataArray: ByteArray
        get() = layerTerrainLowBits.data

    private fun coerceXY(x: Int, y: Int) = (x fmod width) to (y.coerceWorld())

    fun getTileFromWall(x: Int, y: Int): Int? {
        val (x, y) = coerceXY(x, y)
        val wall: Int? = layerWall.getTile(x, y)
        val wallDamage: Int? = getWallLowBits(x, y)
        return if (wall == null || wallDamage == null)
            null
        else
            wall * PairedMapLayer.RANGE + wallDamage
    }

    fun getTileFromTerrain(x: Int, y: Int): Int? {
        val (x, y) = coerceXY(x, y)
        val terrain: Int? = layerTerrain.getTile(x, y)
        val terrainDamage: Int? = getTerrainLowBits(x, y)
        return if (terrain == null || terrainDamage == null)
            null
        else
            terrain * PairedMapLayer.RANGE + terrainDamage
    }

    fun getTileFromWire(x: Int, y: Int): Int? {
        val (x, y) = coerceXY(x, y)
        return layerWire.getTile(x, y)
    }

    fun getWallLowBits(x: Int, y: Int): Int? {
        val (x, y) = coerceXY(x, y)
        return layerWallLowBits.getData(x, y)
    }

    fun getTerrainLowBits(x: Int, y: Int): Int? {
        val (x, y) = coerceXY(x, y)
        return layerTerrainLowBits.getData(x, y)
    }

    /**
     * Set the tile of wall as specified, with damage value of zero.
     * @param x
     * *
     * @param y
     * *
     * @param combinedTilenum (tilenum * 16) + damage
     */
    fun setTileWall(x: Int, y: Int, combinedTilenum: Int) {
        val (x, y) = coerceXY(x, y)
        setTileWall(x, y, (combinedTilenum / PairedMapLayer.RANGE).toByte(), combinedTilenum % PairedMapLayer.RANGE)
    }

    /**
     * Set the tile of wall as specified, with damage value of zero.
     * @param x
     * *
     * @param y
     * *
     * @param combinedTilenum (tilenum * 16) + damage
     */
    fun setTileTerrain(x: Int, y: Int, combinedTilenum: Int) {
        val (x, y) = coerceXY(x, y)
        setTileTerrain(x, y, (combinedTilenum / PairedMapLayer.RANGE).toByte(), combinedTilenum % PairedMapLayer.RANGE)
    }

    fun setTileWall(x: Int, y: Int, tile: Byte, damage: Int) {
        val (x, y) = coerceXY(x, y)
        layerWall.setTile(x, y, tile)
        layerWallLowBits.setData(x, y, damage)
        wallDamages.remove(LandUtil.getBlockAddr(this, x, y))
    }

    /**
     * Warning: this function alters fluid lists: be wary of call order!
     */
    fun setTileTerrain(x: Int, y: Int, tile: Byte, damage: Int) {
        val (x, y) = coerceXY(x, y)
        layerTerrain.setTile(x, y, tile)
        layerTerrainLowBits.setData(x, y, damage)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        terrainDamages.remove(blockAddr)

        if (BlockCodex[tile * PairedMapLayer.RANGE + damage].isSolid) {
            fluidFills.remove(blockAddr)
            fluidTypes.remove(blockAddr)
        }
        // fluid tiles-item should be modified so that they will also place fluid onto their respective map
    }

    fun setTileWire(x: Int, y: Int, tile: Byte) {
        val (x, y) = coerceXY(x, y)
        layerWire.setTile(x, y, tile)
    }

    fun getTileFrom(mode: Int, x: Int, y: Int): Int? {
        if (mode == TERRAIN) {
            return getTileFromTerrain(x, y)
        }
        else if (mode == WALL) {
            return getTileFromWall(x, y)
        }
        else if (mode == WIRE) {
            return getTileFromWire(x, y)
        }
        else
            throw IllegalArgumentException("illegal mode input: " + mode.toString())
    }

    fun terrainIterator(): Iterator<Int> {
        return object : Iterator<Int> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): Int {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return getTileFromTerrain(x, y)!!
            }

        }
    }

    fun wallIterator(): Iterator<Int> {
        return object : Iterator<Int> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean =
                    iteratorCount < width * height

            override fun next(): Int {
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
            setTileTerrain(x, y, 0)
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
            setTileWall(x, y, 0)
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
        fun getProp() = FluidCodex[type]
        override fun toString() = "Fluid type: ${type.value}, amount: $amount"
    }


    fun getTemperature(worldTileX: Int, worldTileY: Int): Float? {
        return null
    }

    fun getAirPressure(worldTileX: Int, worldTileY: Int): Float? {
        return null
    }


    private fun Int.coerceWorld() = this.coerceIn(0, height - 1)
    
    companion object {
        @Transient val WALL = 0
        @Transient val TERRAIN = 1
        @Transient val WIRE = 2

        /** 4096 */
        @Transient val TILES_SUPPORTED = MapLayer.RANGE * PairedMapLayer.RANGE
        @Transient val SIZEOF: Byte = MapLayer.SIZEOF
        @Transient val LAYERS: Byte = 4 // terrain, wall (layerTerrainLowBits + layerWallLowBits), wire

        fun makeNullWorld() = GameWorld(-1, 1, 1, 0, 0, 0)
    }
}

infix fun Int.fmod(other: Int) = Math.floorMod(this, other)
infix fun Float.fmod(other: Float) = if (this >= 0f) this % other else (this % other) + other

inline class FluidType(val value: Int) {
    infix fun sameAs(other: FluidType) = this.value.absoluteValue == other.value.absoluteValue
    fun abs() = this.value.absoluteValue
}