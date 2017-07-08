
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.blockproperties.BlockCodex
import org.dyn4j.geometry.Vector2

typealias BlockAddress = Long
typealias BlockDamage = Float

class GameWorld(val width: Int, val height: Int) {


    //layers
    val layerWall: MapLayer
    val layerTerrain: MapLayer
    val layerWire: MapLayer

    val layerWallLowBits: PairedMapLayer
    val layerTerrainLowBits: PairedMapLayer

    val layerThermal: MapLayerFloat // in Kelvins

    val spawnX: Int
    val spawnY: Int

    val wallDamages = HashMap<BlockAddress, BlockDamage>()
    val terrainDamages = HashMap<BlockAddress, BlockDamage>()

    //public World physWorld = new World( new Vec2(0, -TerrarumMain.game.gravitationalAccel) );
    //physics
    /** Meter per second squared. Currently only the downward gravity is supported. No reverse gravity :p */
    var gravitation: Vector2 = Vector2(0.0, 9.8)
    /** 0.0..1.0+ */
    var globalLight = Color(0f,0f,0f,1f)



    val time: WorldTime
    val economy = GameEconomy()



    var generatorSeed: Long = 0



    init {
        this.spawnX = width / 2
        this.spawnY = 200

        layerTerrain = MapLayer(width, height)
        layerWall = MapLayer(width, height)
        layerWire = MapLayer(width, height)
        layerTerrainLowBits = PairedMapLayer(width, height)
        layerWallLowBits = PairedMapLayer(width, height)

        layerThermal = MapLayerFloat(width / 2, height / 2)


        time = WorldTime(
                71 * WorldTime.DAY_LENGTH +
                7 * WorldTime.HOUR_SEC +
                30L * WorldTime.MINUTE_SEC
        )
    }

    /**
     * Get 2d array data of terrain

     * @return byte[][] terrain layer
     */
    val terrainArray: Array<ByteArray>
        get() = layerTerrain.data

    /**
     * Get 2d array data of wall

     * @return byte[][] wall layer
     */
    val wallArray: Array<ByteArray>
        get() = layerWall.data

    /**
     * Get 2d array data of wire

     * @return byte[][] wire layer
     */
    val wireArray: Array<ByteArray>
        get() = layerWire.data

    /**
     * Get paired array data of damage codes.
     * Format: 0baaaabbbb, aaaa for x = 0, 2, 4, ..., bbbb for x = 1, 3, 5, ...
     * @return byte[][] damage code pair
     */
    val damageDataArray: Array<ByteArray>
        get() = layerTerrainLowBits.dataPair

    fun getTileFromWall(x: Int, y: Int): Int? {
        val wall: Int? = layerWall.getTile(x fmod width, y)
        val wallDamage: Int? = getWallLowBits(x fmod width, y)
        return if (wall == null || wallDamage == null)
            null
        else
            wall * PairedMapLayer.RANGE + wallDamage
    }

    fun getTileFromTerrain(x: Int, y: Int): Int? {
        val terrain: Int? = layerTerrain.getTile(x fmod width, y)
        val terrainDamage: Int? = getTerrainLowBits(x fmod width, y)
        return if (terrain == null || terrainDamage == null)
            null
        else
            terrain * PairedMapLayer.RANGE + terrainDamage
    }

    fun getTileFromWire(x: Int, y: Int): Int? {
        return layerWire.getTile(x fmod width, y)
    }

    fun getWallLowBits(x: Int, y: Int): Int? {
        return layerWallLowBits.getData(x fmod width, y)
    }

    fun getTerrainLowBits(x: Int, y: Int): Int? {
        return layerTerrainLowBits.getData(x fmod width, y)
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
        setTileWall(x fmod width, y, (combinedTilenum / PairedMapLayer.RANGE).toByte(), combinedTilenum % PairedMapLayer.RANGE)
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
        setTileTerrain(x fmod width, y, (combinedTilenum / PairedMapLayer.RANGE).toByte(), combinedTilenum % PairedMapLayer.RANGE)
    }

    fun setTileWall(x: Int, y: Int, tile: Byte, damage: Int) {
        layerWall.setTile(x fmod width, y, tile)
        layerWallLowBits.setData(x fmod width, y, damage)
        wallDamages.remove(LandUtil.getBlockAddr(x, y))
    }

    fun setTileTerrain(x: Int, y: Int, tile: Byte, damage: Int) {
        layerTerrain.setTile(x fmod width, y, tile)
        layerTerrainLowBits.setData(x fmod width, y, damage)
        terrainDamages.remove(LandUtil.getBlockAddr(x, y))
    }

    fun setTileWire(x: Int, y: Int, tile: Byte) {
        layerWire.data[y][x fmod width] = tile
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

    fun updateWorldTime(delta: Float) {
        time.update(delta)
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
        val addr = LandUtil.getBlockAddr(x, y)

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
            return true
        }

        return false
    }
    fun getTerrainDamage(x: Int, y: Int): Float =
            terrainDamages[LandUtil.getBlockAddr(x, y)] ?: 0f

    /**
     * @return true if block is broken
     */
    fun inflictWallDamage(x: Int, y: Int, damage: Double): Boolean {
        val damage = damage.toFloat()
        val addr = LandUtil.getBlockAddr(x, y)

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
            return true
        }

        return false
    }
    fun getWallDamage(x: Int, y: Int): Float =
            wallDamages[LandUtil.getBlockAddr(x, y)] ?: 0f

    companion object {

        @Transient val WALL = 0
        @Transient val TERRAIN = 1
        @Transient val WIRE = 2

        @Transient val TILES_SUPPORTED = MapLayer.RANGE * PairedMapLayer.RANGE
        @Transient val SIZEOF: Byte = MapLayer.SIZEOF
        @Transient val LAYERS: Byte = 4 // terrain, wall (layerTerrainLowBits + layerWallLowBits), wire
    }
}

infix fun Int.fmod(other: Int) = Math.floorMod(this, other)
