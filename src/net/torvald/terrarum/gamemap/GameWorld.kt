
package net.torvald.terrarum.gamemap

import org.dyn4j.geometry.Vector2
import org.newdawn.slick.SlickException

class GameWorld
/**
 * @param width
 * *
 * @param height
 * *
 * @throws SlickException
 */
@Throws(SlickException::class)
constructor(//properties
        val width: Int, val height: Int) {

    //layers
    val layerWall: MapLayer
    /**
     * Get MapLayer object of terrain

     * @return MapLayer terrain layer
     */
    val layerTerrain: MapLayer
    val layerWire: MapLayer
    val wallDamage: PairedMapLayer
    val terrainDamage: PairedMapLayer
    val spawnX: Int
    val spawnY: Int

    //public World physWorld = new World( new Vec2(0, -TerrarumMain.game.gravitationalAccel) );
    //physics
    /** Meter per second squared. Currently only the downward gravity is supported. No reverse gravity :p */
    var gravitation: Vector2 = Vector2(0.0, 9.8)
    /** RGB in Integer */
    var globalLight: Int = 0
    val time: WorldTime

    var generatorSeed: Long = 0

    init {
        this.spawnX = width / 2
        this.spawnY = 200

        layerTerrain = MapLayer(width, height)
        layerWall = MapLayer(width, height)
        layerWire = MapLayer(width, height)
        terrainDamage = PairedMapLayer(width, height)
        wallDamage = PairedMapLayer(width, height)

        time = WorldTime()
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
        get() = terrainDamage.dataPair

    fun getTileFromWall(x: Int, y: Int): Int? {
        val wall: Int? = layerWall.getTile(x, y)
        val wallDamage: Int? = getWallDamage(x, y)
        return if (wall == null || wallDamage == null)
            null
        else
            wall * PairedMapLayer.RANGE + wallDamage
    }

    fun getTileFromTerrain(x: Int, y: Int): Int? {
        val terrain: Int? = layerTerrain.getTile(x, y)
        val terrainDamage: Int? = getTerrainDamage(x, y)
        return if (terrain == null || terrainDamage == null)
            null
        else
            terrain * PairedMapLayer.RANGE + terrainDamage
    }

    fun getTileFromWire(x: Int, y: Int): Int? {
        return layerWire.getTile(x, y)
    }

    fun getWallDamage(x: Int, y: Int): Int? {
        return wallDamage.getData(x, y)
    }

    fun getTerrainDamage(x: Int, y: Int): Int? {
        return terrainDamage.getData(x, y)
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
        setTileTerrain(x, y, (combinedTilenum / PairedMapLayer.RANGE).toByte(), combinedTilenum % PairedMapLayer.RANGE)
    }

    fun setTileWall(x: Int, y: Int, tile: Byte, damage: Int) {
        layerWall.setTile(x, y, tile)
        wallDamage.setData(x, y, damage)
    }

    fun setTileTerrain(x: Int, y: Int, tile: Byte, damage: Int) {
        layerTerrain.setTile(x, y, tile)
        terrainDamage.setData(x, y, damage)
    }

    fun setTileWire(x: Int, y: Int, tile: Byte) {
        layerWire.data[y][x] = tile
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

    fun updateWorldTime(delta: Int) {
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

    companion object {

        @Transient val WALL = 0
        @Transient val TERRAIN = 1
        @Transient val WIRE = 2

        @Transient val TILES_SUPPORTED = MapLayer.RANGE * PairedMapLayer.RANGE
        @Transient val SIZEOF: Byte = MapLayer.SIZEOF
        @Transient val LAYERS: Byte = 4 // terrain, wall (terrainDamage + wallDamage), wire
    }
}