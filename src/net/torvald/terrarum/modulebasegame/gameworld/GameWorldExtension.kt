package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.serialise.ReadLayerDataZip

/**
 * Created by minjaesong on 2018-07-03.
 */
class GameWorldExtension : GameWorld {

    constructor(worldIndex: Int, width: Int, height: Int, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) : super(worldIndex, width, height, creationTIME_T, lastPlayTIME_T, totalPlayTime)
    internal constructor(worldIndex: Int, layerData: ReadLayerDataZip.LayerData, creationTIME_T: Long, lastPlayTIME_T: Long, totalPlayTime: Int) : super(worldIndex, layerData, creationTIME_T, lastPlayTIME_T, totalPlayTime)


    /** Extended world time */
    val worldTime: WorldTime
    val economy = GameEconomy()

    override var TIME_T: Long
        get() = worldTime.TIME_T
        set(value) { worldTime.TIME_T = value }
    override var dayLength: Int
        get() = WorldTime.DAY_LENGTH
        set(value) { throw UnsupportedOperationException() }


    // delegated properties //
    /*val layerWall: MapLayer; get() = baseworld.layerWall
    val layerTerrain: MapLayer; get() = baseworld.layerTerrain
    val layerWire: MapLayer; get() = baseworld.layerWire
    val layerWallLowBits: PairedMapLayer; get() = baseworld.layerWallLowBits
    val layerTerrainLowBits: PairedMapLayer; get() = baseworld.layerTerrainLowBits
    val layerHalfThermal: MapLayerHalfFloat; get() = baseworld.layerHalfThermal
    var spawnX: Int; get() = baseworld.spawnX; set(v) { baseworld.spawnX = v }
    var spawnY: Int; get() = baseworld.spawnY; set(v) { baseworld.spawnY = v }
    val wallDamages: HashMap<BlockAddress, BlockDamage>; get() = baseworld.wallDamages
    val terrainDamages: HashMap<BlockAddress, BlockDamage>; get() = baseworld.terrainDamages
    var globalLight: Color; get() = baseworld.globalLight; set(v) { baseworld.globalLight = v }
    var averageTemperature: Float; get() = baseworld.averageTemperature; set(v) { baseworld.averageTemperature = v }
    var generatorSeed: Long; get() = baseworld.generatorSeed; set(v) { baseworld.generatorSeed = v }
    val terrainArray: ByteArray; get() = baseworld.terrainArray
    val wallArray: ByteArray; get() = baseworld.wallArray
    val wireArray: ByteArray; get() = baseworld.wireArray
    val damageDataArray: ByteArray; get() = baseworld.damageDataArray*/

    init {
        worldTime = WorldTime( // Year EPOCH (125), Month 1, Day 1 is implied
                7 * WorldTime.HOUR_SEC +
                30L * WorldTime.MINUTE_SEC
        )
    }

    fun updateWorldTime(delta: Float) {
        worldTime.update(delta)
    }

}