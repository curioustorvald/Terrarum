
package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Disposable
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isFluid
import net.torvald.terrarum.itemproperties.ItemRemapTable
import net.torvald.terrarum.itemproperties.ItemTable
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.utils.*
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.weather.Weatherbox
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.util.SortedArrayList
import org.dyn4j.geometry.Vector2
import java.util.*
import java.util.concurrent.Executors.callable

typealias BlockAddress = Long

class PhysicalStatus() {
    // bottom-center point
    var position = Point2d()
    // actorvalues are copied separately so don't worry about them here

    constructor(player: IngamePlayer) : this() {
        this.position = Point2d(player.hitbox.canonicalX, player.hitbox.canonicalY)
    }
}

/**
 * Special version of GameWorld where everything, including layer data, are saved in a single JSON file (i.e. not chunked)
 */
class SimpleGameWorld(width: Int, height: Int) : GameWorld(width, height) {
    override lateinit var layerWall: BlockLayerGenericI16
    override lateinit var layerTerrain: BlockLayerGenericI16
    constructor() : this(0, 0)
    override fun dispose() {
        layerWall.dispose()
        layerTerrain.dispose()
    }
}

open class GameWorld(
    val worldIndex: UUID // should not be immutable as JSON loader will want to overwrite it
) : Disposable {

    constructor() : this(UUID.randomUUID())
    constructor(width: Int, height: Int) : this(UUID.randomUUID()) {
        this.width = width
        this.height = height
    }

    var worldCreator: UUID = UUID(0L,0L) // TODO record a value to this
    var width: Int = 0; private set
    var height: Int = 0; private set

    var playersLastStatus = PlayersLastStatus() // only gets used when the game saves and loads

    /**
     * 0,1 - RoguelikeRandomiser
     * 2,3 - WeatherMixer
     */
    val randSeeds = LongArray(256) // stores 128 128-bit numbers

    /** Creation time for this world, NOT the entire savegame */
    internal var creationTime = -1L
        internal set
    /** Creation time for this world, NOT the entire savegame */
    internal var lastPlayTime = -1L
        internal set // there's a case of save-and-continue-playing
    /** Creation time for this world, NOT the entire savegame */
    internal var totalPlayTime = 0L // cumulative value for this very world

    val gameRules = KVHashMap() // spawn points, creation/lastplay/totalplaytimes are NOT stored to gameRules

    init {
        creationTime = App.getTIME_T()
    }

    //layers
    @Transient open lateinit var layerWall: BlockLayerGenericI16
    @Transient open lateinit var layerTerrain: BlockLayerGenericI16
    @Transient open lateinit var layerOres: BlockLayerOresI16I8 // damage to the block follows `terrainDamages`
    @Transient open lateinit var layerFluids: BlockLayerFluidI16F16
    val wallDamages = HashArray<Float>()
    val terrainDamages = HashArray<Float>()

    @Transient open lateinit var chunkFlags: Array<ByteArray>

    //val layerThermal: MapLayerHalfFloat // in Kelvins
    //val layerFluidPressure: MapLayerHalfFloat // (milibar - 1000)

    /** Tilewise spawn point */
    var spawnX: Int = 0
    /** Tilewise spawn point */
    var spawnY: Int = 0

    var spawnPoint: Point2i
        get() = Point2i(spawnX, spawnY)
        set(value) {
            spawnX = value.x
            spawnY = value.y
        }
    var portalPoint: Point2i? = null



    /**
     * Single block can have multiple conduits, different types of conduits are stored separately.
     */
    public val wirings = HashedWirings()
    private val wiringGraph = HashedWiringGraph()

    @Transient private val WIRE_POS_MAP = intArrayOf(1,2,4,8)
    @Transient private val WIRE_ANTIPOS_MAP = intArrayOf(4,8,1,2)

    /**
     * Used by the renderer. When wirings are updated, `wirings` and this properties must be synchronised.
     */
    //private val wiringBlocks: HashArray<ItemID>

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

    // Terrain, ores and fluids all use the same number space

    @Transient private val forcedTileNumberToNames = hashSetOf(
        Block.AIR, Block.UPDATE, Block.NOT_GENERATED
    )
    /*@Transient private val forcedFluidNumberToTiles = hashSetOf(
        Fluid.NULL
    )*/
    val tileNumberToNameMap = HashArray<ItemID>().also {
        it[0] = Block.AIR
        it[1] = Block.UPDATE
        it[65535] = Block.NOT_GENERATED // unlike Block.NULL, this one is solid
    }
    @Transient internal lateinit var oldTileNumberToNameMap: Map<Long, ItemID> // this is a caching variable, refreshed on every (initial) load
    /*val fluidNumberToNameMap = HashArray<ItemID>().also {
        it[0] = Fluid.NULL
        it[65535] = Fluid.NULL // 65535 denotes "not generated"
    }*/
    // does not go to the savefile
    @Transient val tileNameToNumberMap = HashMap<ItemID, Int>().also {
        it[Block.AIR] = 0
        it[Block.UPDATE] = 1
        it[Block.NOT_GENERATED] = 65535 // unlike Block.NULL, this one is solid
    }
    /*@Transient val fluidNameToNumberMap = HashMap<ItemID, Int>().also {
        it[Fluid.NULL] = 0
    }*/

    val extraFields = HashMap<String, Any?>()

    // NOTE: genver was here but removed: genver will be written by manually editing the serialising JSON. Reason: the 'genver' string must be found on a fixed offset on the file.
    internal var comp = -1 // only gets used when the game saves and loads

    internal val dynamicItemInventory = ItemTable()
    internal val dynamicToStaticTable = ItemRemapTable()

    @Deprecated("This value is only used for savegames; DO NOT USE THIS", ReplaceWith("INGAME.actorContainerActive", "net.torvald.terrarum.INGAME"))
    internal val actors = ArrayList<ActorID>() // only filled up on save and load; DO NOT USE THIS

    var weatherbox = Weatherbox()

    init {
        weatherbox.initWith(WeatherCodex.getById("generic01") ?: WeatherMixer.DEFAULT_WEATHER, 3600L)
        val currentWeather = weatherbox.currentWeather
        // TEST FILL WITH RANDOM VALUES
        (0..6).map { WeatherMixer.takeUniformRand(0f..1f) }.let {
            weatherbox.windDir.pM2 = it[1]
            weatherbox.windDir.pM1 = it[2]
            weatherbox.windDir.p0  = it[3]
            weatherbox.windDir.p1  = it[4]
            weatherbox.windDir.p2  = it[5]
            weatherbox.windDir.p3  = it[6]
        }
        (0..6).map { WeatherMixer.takeUniformRand(-1f..1f) }.let {
            val pM3 = currentWeather.getRandomWindSpeed(it[1])
            weatherbox.windSpeed.pM2 = currentWeather.getRandomWindSpeed(pM3, it[1])
            weatherbox.windSpeed.pM1 = currentWeather.getRandomWindSpeed(weatherbox.windSpeed.pM2, it[2])
            weatherbox.windSpeed.p0  = currentWeather.getRandomWindSpeed(weatherbox.windSpeed.pM1, it[3])
            weatherbox.windSpeed.p1  = currentWeather.getRandomWindSpeed(weatherbox.windSpeed.p0, it[4])
            weatherbox.windSpeed.p2  = currentWeather.getRandomWindSpeed(weatherbox.windSpeed.p1, it[5])
            weatherbox.windSpeed.p3  = currentWeather.getRandomWindSpeed(weatherbox.windSpeed.p2, it[6])
        }

        // the savegame loader will overwrite whatever the initial value we have here
    }


    /**
     * Create new world
     */
    constructor(width: Int, height: Int, creationTIME_T: Long, lastPlayTIME_T: Long): this() {
        if (width <= 0 || height <= 0) throw IllegalArgumentException("Non-positive width/height: ($width, $height)")

        this.width = width
        this.height = height

        // preliminary spawn points
        this.spawnX = width / 2
        this.spawnY = 150

        layerTerrain = BlockLayerGenericI16(width, height)
        layerWall = BlockLayerGenericI16(width, height)
        layerOres = BlockLayerOresI16I8(width, height)
        layerFluids = BlockLayerFluidI16F16(width, height)
        chunkFlags = Array(height / CHUNK_H) { ByteArray(width / CHUNK_W) }

        // temperature layer: 2x2 is one cell
        //layerThermal = MapLayerHalfFloat(width, height, averageTemperature)

        // fluid pressure layer: 4 * 8 is one cell
        //layerFluidPressure = MapLayerHalfFloat(width, height, 13f) // 1013 mBar


        creationTime = creationTIME_T
        lastPlayTime = lastPlayTIME_T


        if (App.tileMaker != null) {
            App.tileMaker.tags.forEach {
                if (!forcedTileNumberToNames.contains(it.key)) {
                    printdbg(this, "newworld tileNumber ${it.value.tileNumber} <-> tileName ${it.key}")

                    tileNumberToNameMap[it.value.tileNumber.toLong()] = it.key
                    tileNameToNumberMap[it.key] = it.value.tileNumber
                }
            }
            /*Terrarum.fluidCodex.fluidProps.entries.forEach {
                if (!forcedFluidNumberToTiles.contains(it.key)) {
                    fluidNumberToNameMap[it.value.numericID.toLong()] = it.key
                    fluidNameToNumberMap[it.key] = it.value.numericID
                }
            }*/
        }
    }

    fun coordInWorld(x: Int, y: Int) = y in 0 until height // ROUNDWORLD implementation
    fun coordInWorldStrict(x: Int, y: Int) = x in 0 until width && y in 0 until height // ROUNDWORLD implementation

    fun renumberTilesAfterLoad() {
        printdbg(this, "renumberTilesAfterLoad()")

        // patch the "old"map
        tileNumberToNameMap[0] = Block.AIR
        tileNumberToNameMap[1] = Block.UPDATE
        tileNumberToNameMap[65535] = Block.NOT_GENERATED
        // before the renaming, update the name maps
        oldTileNumberToNameMap = tileNumberToNameMap.toMap()

        tileNumberToNameMap.forEach { l, s ->
            printdbg(this, "  afterload oldMapping tileNumber $l <-> $s")
        }

        printdbg(this, "")

        tileNumberToNameMap.clear()
        tileNameToNumberMap.clear()
        App.tileMaker.tags.forEach {
            printdbg(this, "  afterload tileMaker tileNumber ${it.value.tileNumber} <-> ${it.key}")

            tileNumberToNameMap[it.value.tileNumber.toLong()] = it.key
            tileNameToNumberMap[it.key] = it.value.tileNumber
        }
        /*Terrarum.fluidCodex.fluidProps.entries.forEach {
            fluidNumberToNameMap[it.value.numericID.toLong()] = it.key
            fluidNameToNumberMap[it.key] = it.value.numericID
        }*/

        // force this rule to the old saves
        tileNumberToNameMap[0] = Block.AIR
        tileNumberToNameMap[1] = Block.UPDATE
        tileNumberToNameMap[65535] = Block.NOT_GENERATED
        tileNameToNumberMap[Block.AIR] = 0
        tileNameToNumberMap[Block.UPDATE] = 1
        tileNameToNumberMap[Block.NOT_GENERATED] = 65535
//        fluidNumberToNameMap[0] = Fluid.NULL
//        fluidNumberToNameMap[65535] = Fluid.NULL
//        fluidNameToNumberMap[Fluid.NULL] = 0


        BlocksDrawer.rebuildInternalPrecalculations()

        // perform renaming of tile layers
        /*for (y in 0 until layerTerrain.height) {
            for (x in 0 until layerTerrain.width) {
                // renumber terrain and wall
                layerTerrain.unsafeSetTile(x, y, tileNameToNumberMap[oldTileNumberToNameMap[layerTerrain.unsafeGetTile(x, y).toLong()]]!!)
                layerWall.unsafeSetTile(x, y, tileNameToNumberMap[oldTileNumberToNameMap[layerWall.unsafeGetTile(x, y).toLong()]]!!)

                // renumber ores
                val oldOreNum = layerOres.unsafeGetTile(x, y).toLong()
                val oldOreName = oldTileNumberToNameMap[oldOreNum]
                layerOres.unsafeSetTileKeepPlacement(x, y, oldOreName.let { tileNameToNumberMap[it] ?: throw NullPointerException("Unknown tile name: $oldOreName (<- $oldOreNum)") })

                // renumber fluids
                val (oldFluidNum, oldFluidFill) = layerFluids.unsafeGetTile1(x, y)
                val oldFluidName = oldTileNumberToNameMap[oldFluidNum.toLong()]
                layerFluids.unsafeSetTile(x, y, oldFluidName.let { tileNameToNumberMap[it] ?: throw NullPointerException("Unknown tile name: $oldFluidName (<- $oldFluidNum)") }, oldFluidFill)
            }
        }*/
        // will use as much threads you have on the system
        printdbg(this, "starting renumbering thread")
        try {
            val te = ThreadExecutor()
            te.renew()
            te.submitAll1(
                (0 until layerTerrain.width step CHUNK_W).map { xorigin ->
                    callable {
                        for (y in 0 until layerTerrain.height) {
                            for (x in xorigin until (xorigin + CHUNK_W).coerceAtMost(layerTerrain.width)) {
                                // renumber terrain and wall
                                layerTerrain.unsafeSetTile(
                                    x, y,
                                    tileNameToNumberMap[oldTileNumberToNameMap[layerTerrain.unsafeGetTile(x, y)
                                        .toLong()]]!!
                                )
                                layerWall.unsafeSetTile(
                                    x, y,
                                    tileNameToNumberMap[oldTileNumberToNameMap[layerWall.unsafeGetTile(x, y)
                                        .toLong()]]!!
                                )

                                // renumber ores
                                val oldOreNum = layerOres.unsafeGetTile(x, y).toLong()
                                val oldOreName = oldTileNumberToNameMap[oldOreNum]
                                layerOres.unsafeSetTileKeepPlacement(x, y,
                                    oldOreName.let {
                                        tileNameToNumberMap[it]
                                            ?: throw NullPointerException("Unknown tile name: $oldOreName (<- $oldOreNum)")
                                    })

                                // renumber fluids
                                val (oldFluidNum, oldFluidFill) = layerFluids.unsafeGetTile1(x, y)
                                val oldFluidName = oldTileNumberToNameMap[oldFluidNum.toLong()]
                                layerFluids.unsafeSetTile(
                                    x, y,
                                    oldFluidName.let {
                                        tileNameToNumberMap[it]
                                            ?: throw NullPointerException("Unknown tile name: $oldFluidName (<- $oldFluidNum)")
                                    },
                                    oldFluidFill
                                )
                            }
                        }
                    }
                }
            )
            te.join()
        }
        catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
        printdbg(this, "renumbering thread finished")

        printdbg(this, "renumberTilesAfterLoad done!")
    }
    
    /**
     * Get 2d array data of wire
     * @return byte[][] wire layer
     */
    //val wireArray: ByteArray
    //    get() = layerWire.data

    fun getLayer(index: Int) = when(index) {
        TERRAIN -> layerTerrain
        WALL -> layerWall
        ORES -> layerOres
        FLUID -> layerFluids
        else -> null//throw IllegalArgumentException("Unknown layer index: $index")
    }

    fun coerceXY(x: Int, y: Int) = (x fmod width) to (y.coerceIn(0, height - 1))
    fun coerceXY(xy: Pair<Int, Int>) = (xy.first fmod width) to (xy.second.coerceIn(0, height - 1))

    /**
     * @return ItemID, WITHOUT wall tag
     */
    fun getTileFromWall(rawX: Int, rawY: Int): ItemID {
        val (x, y) = coerceXY(rawX, rawY)
        return tileNumberToNameMap[layerWall.unsafeGetTile(x, y).toLong()] ?: Block.UPDATE//throw NoSuchElementException("No tile name mapping for wall ${layerWall.unsafeGetTile(x, y)} in ($x, $y) from $layerWall")
    }

    /**
     * @return ItemID
     */
    fun getTileFromTerrain(rawX: Int, rawY: Int): ItemID {
        val (x, y) = coerceXY(rawX, rawY)
        return tileNumberToNameMap[layerTerrain.unsafeGetTile(x, y).toLong()] ?: Block.UPDATE//throw NoSuchElementException("No tile name mapping for terrain ${layerTerrain.unsafeGetTile(x, y)} in ($x, $y) from $layerTerrain")
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

        if (!bypassEvent && oldWall != itemID) {
            Terrarum.ingame?.queueWallChangedEvent(oldWall, itemID, x, y)
            Terrarum.ingame?.modified(LandUtil.LAYER_WALL, x, y)
        }
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
        val tilenum = tileNameToNumberMap[itemID] ?: throw NullPointerException("Unknown tile name $itemID")

        val oldTerrain = getTileFromTerrain(x, y)
        layerTerrain.unsafeSetTile(x, y, tilenum)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        terrainDamages.remove(blockAddr)

        if (BlockCodex[itemID].isSolid) {
            layerFluids.unsafeSetTile(x, y, tileNameToNumberMap[Fluid.NULL]!!, 0f)
//            Terrarum.ingame?.modified(LandUtil.LAYER_FLUID, x, y)
        }
        // fluid tiles-item should be modified so that they will also place fluid onto their respective map

        val oldOre = layerOres.unsafeGetTile(x, y)
        val deleteOldOre = (oldOre != 0 && !BlockCodex[itemID].isSolid)

        if (deleteOldOre) {
            layerOres.unsafeSetTile(x, y, 0, 0)
        }

        if (!bypassEvent && oldTerrain != itemID) {
            Terrarum.ingame?.queueTerrainChangedEvent(oldTerrain, itemID, x, y)
            Terrarum.ingame?.modified(LandUtil.LAYER_TERR, x, y)

            if (deleteOldOre) {
                Terrarum.ingame?.modified(LandUtil.LAYER_ORES, x, y)
            }
        }
    }

    fun setTileWire(x: Int, y: Int, tile: ItemID, bypassEvent: Boolean, connection: Int) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        val wireNode = wirings[blockAddr]

        if (wireNode == null) {
            wirings[blockAddr] = WiringNode(SortedArrayList())
        }

        wirings[blockAddr]!!.ws.add(tile)

        if (!bypassEvent) {
            Terrarum.ingame?.queueWireChangedEvent(tile, false, x, y)
//            Terrarum.ingame?.modified(LandUtil.LAYER_WIRE, x, y)
        }

        /*
        // auto-connect-to-the-neighbour wire placement
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
        }*/

        // scratch-that-i'll-figure-it-out wire placement
        setWireGraphOfUnsafe(blockAddr, tile, connection)
    }

    fun setTileOnLayerUnsafe(layer: Int, x: Int, y: Int, tile: Int) {
        (getLayer(layer) ?: throw IllegalArgumentException("Unknown layer index: $layer")).let {
            if (it !is BlockLayerGenericI16) throw IllegalArgumentException("Block layers other than BlockLayer16 is not supported yet)")
            it.unsafeSetTile(x, y, tile)
        }
    }

    fun removeTileWire(x: Int, y: Int, tile: ItemID, bypassEvent: Boolean) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        val wireNode = wirings[blockAddr]

        if (wireNode != null) {
            if (!bypassEvent) {
                Terrarum.ingame?.queueWireChangedEvent(tile, true, x, y)
//                Terrarum.ingame?.modified(LandUtil.LAYER_WIRE, x, y)
            }

            // disconnect neighbouring nodes
            /* RIGHT */getWireGraphOf(x+1, y, tile)?.let { setWireGraphOf(x+1, y, tile, it and 0b1011) }
            /* BOTTOM */if (y+1 < height) getWireGraphOf(x, y+1, tile)?.let { setWireGraphOf(x, y+1, tile, it and 0b0111) }
            /* LEFT */getWireGraphOf(x-1, y, tile)?.let { setWireGraphOf(x-1, y, tile, it and 0b1110) }
            /* TOP */if (y-1 >= 0) getWireGraphOf(x, y-1, tile)?.let { setWireGraphOf(x, y-1, tile, it and 0b1101) }

            // remove wire from this tile
            wiringGraph[blockAddr]!!.remove(tile)
            wirings[blockAddr]!!.ws.remove(tile)
        }
    }

    fun removeTileWireNoReconnect(x: Int, y: Int, tile: ItemID, bypassEvent: Boolean) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        val wireNode = wirings[blockAddr]

        if (wireNode != null) {
            if (!bypassEvent) {
                Terrarum.ingame?.queueWireChangedEvent(tile, true, x, y)
//                Terrarum.ingame?.modified(LandUtil.LAYER_WIRE, x, y)
            }

            // remove wire from this tile
            wiringGraph[blockAddr]!!.remove(tile)
            wirings[blockAddr]!!.ws.remove(tile)
        }
    }

    fun getWireGraphOf(x: Int, y: Int, itemID: ItemID): Int? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireGraphUnsafe(blockAddr, itemID)
    }

    fun getWireGraphUnsafe(blockAddr: BlockAddress, itemID: ItemID): Int? {
        return wiringGraph[blockAddr]?.get(itemID)?.cnx
    }

    fun getWireEmitStateOf(x: Int, y: Int, itemID: ItemID): Vector2? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireEmitStateUnsafe(blockAddr, itemID)
    }

    fun getWireEmitStateUnsafe(blockAddr: BlockAddress, itemID: ItemID): Vector2? {
        return wiringGraph[blockAddr]?.get(itemID)?.emt
    }

    fun getWireReceptionStateOf(x: Int, y: Int, itemID: ItemID): ArrayList<WireReceptionState>? {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getWireReceptionStateUnsafe(blockAddr, itemID)
    }

    fun getWireReceptionStateUnsafe(blockAddr: BlockAddress, itemID: ItemID): ArrayList<WireReceptionState>? {
        return wiringGraph[blockAddr]?.get(itemID)?.rcp
    }

    fun setWireGraphOf(x: Int, y: Int, itemID: ItemID, cnx: Int) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return setWireGraphOfUnsafe(blockAddr, itemID, cnx)
    }

    fun setWireGraphOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, cnx: Int) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = WiringGraphMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(cnx)

        wiringGraph[blockAddr]!![itemID]!!.cnx = cnx
    }

    fun setWireEmitStateOf(x: Int, y: Int, itemID: ItemID, vector: Vector2) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return setWireEmitStateOfUnsafe(blockAddr, itemID, vector)
    }

    fun setWireEmitStateOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, vector: Vector2) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = WiringGraphMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(0, vector)

        // out = max(old, new)
        val old = wiringGraph[blockAddr]!![itemID]!!.emt

        wiringGraph[blockAddr]!![itemID]!!.emt.set(old max vector)
    }

    private infix fun Vector2.max(other: Vector2) = Vector2(
        maxOf(this.x, other.x),
        maxOf(this.y, other.y)
    )

    fun addWireRecvStateOf(x: Int, y: Int, itemID: ItemID, state: WireReceptionState) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return addWireRecvStateOfUnsafe(blockAddr, itemID, state)
    }

    fun clearAllWireRecvState(x: Int, y: Int) {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return clearAllWireRecvStateUnsafe(blockAddr)
    }

    fun addWireRecvStateOfUnsafe(blockAddr: BlockAddress, itemID: ItemID, state: WireReceptionState) {
        if (wiringGraph[blockAddr] == null)
            wiringGraph[blockAddr] = WiringGraphMap()
        if (wiringGraph[blockAddr]!![itemID] == null)
            wiringGraph[blockAddr]!![itemID] = WiringSimCell(0)

        wiringGraph[blockAddr]!![itemID]!!.rcp.add(state)
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
            it.value.rcp.clear()
        }
    }

    fun getAllWiresFrom(x: Int, y: Int): Pair<SortedArrayList<ItemID>?, WiringGraphMap?> {
        val (x, y) = coerceXY(x, y)
        val blockAddr = LandUtil.getBlockAddr(this, x, y)
        return getAllWiresFrom(blockAddr)
    }

    fun getAllWiresFrom(blockAddr: BlockAddress): Pair<SortedArrayList<ItemID>?, WiringGraphMap?> {
        return wirings[blockAddr]?.ws to wiringGraph[blockAddr]
    }

    fun getTileFrom(mode: Int, x: Int, y: Int): ItemID {
        if (mode == TERRAIN) {
            return getTileFromTerrain(x, y)
        }
        else if (mode == WALL) {
            return getTileFromWall(x, y)
        }
        else
            throw IllegalArgumentException("illegal mode input: $mode")
    }

    /**
     * Will return (Block.AIR, 0) if there is no ore
     */
    fun getTileFromOre(rawX: Int, rawY: Int): OrePlacement {
        val (x, y) = coerceXY(rawX, rawY)
        val (tileNum, placement) = layerOres.unsafeGetTile1(x, y)
        val tileName = tileNumberToNameMap[tileNum.toLong()]
        return OrePlacement(tileName ?: Block.UPDATE, placement)
    }

    fun setTileOre(rawX: Int, rawY: Int, ore: ItemID, placement: Int) {
        val (x, y) = coerceXY(rawX, rawY)
        layerOres.unsafeSetTile(x, y, tileNameToNumberMap[ore]!!, placement)
    }

    fun terrainIterator(): Iterator<List<ItemID>> {
        return object : Iterator<List<ItemID>> {

            private var iteratorCount = 0

            override fun hasNext(): Boolean {
                return iteratorCount < width * height
            }

            override fun next(): List<ItemID> {
                val y = iteratorCount / width
                val x = iteratorCount % width
                // advance counter
                iteratorCount += 1

                return listOf(getTileFromTerrain(x, y), getTileFromWall(x, y), getTileFromOre(x, y).item)
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
     * @return ItemID of the broken block AND ore if the block is broken, `null` otherwise
     */
    fun inflictTerrainDamage(x: Int, y: Int, damage: Double, bypassEvent: Boolean): Pair<ItemID?, ItemID?> {
        if (damage.isNaN()) throw IllegalArgumentException("Cannot inflict NaN amount of damage at($x, $y)")

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

        if (!bypassEvent) {
            Terrarum.ingame?.modified(LandUtil.LAYER_TERR, x, y)
        }

        //println("[GameWorld] accumulated damage: ${terrainDamages[addr]}")

        // remove tile from the world
        if ((terrainDamages[addr] ?: 0f) >= BlockCodex[getTileFromTerrain(x, y)].strength) {
            val tileBroke = getTileFromTerrain(x, y)
            val oreBroke = getTileFromOre(x, y)
            setTileTerrain(x, y, Block.AIR, false)
            terrainDamages.remove(addr)
            return tileBroke.let { if (it == Block.AIR) null else it } to oreBroke.item.let { if (it == Block.AIR) null else it }
        }

        return null to null
    }
    fun getTerrainDamage(x: Int, y: Int): Float =
            terrainDamages[LandUtil.getBlockAddr(this, x, y)] ?: 0f

    /**
     * @return true if block is broken
     */
    fun inflictWallDamage(x: Int, y: Int, damage: Double, bypassEvent: Boolean): ItemID? {
        if (damage.isNaN()) throw IllegalArgumentException("Cannot inflict NaN amount of damage at($x, $y)")

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

        if (!bypassEvent) {
            Terrarum.ingame?.modified(LandUtil.LAYER_TERR, x, y)
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

    fun setFluid(x: Int, y: Int, fluidType: ItemID, fill: Float) {
        val (x, y) = coerceXY(x, y)

        if (!fluidType.isFluid() && fluidType != Block.AIR) throw IllegalArgumentException("Fluid type is not actually fluid: $fluidType")

        /*if (x == 60 && y == 256) {
            printdbg(this, "Setting fluid $fill at ($x,$y)")
        }*/


        if (fluidType == Fluid.NULL && fill != 0f) {
            throw Error("Illegal fluid fill at ($x,$y): ${FluidInfo(fluidType, fill)}")
        }


//        val addr = LandUtil.getBlockAddr(this, x, y)

        val fluidNumber = tileNameToNumberMap[fluidType] ?: throw NullPointerException("No such fluid: $fluidType")

        if (fill > FLUID_MIN_MASS) {
            //setTileTerrain(x, y, fluidTypeToBlock(fluidType))
            layerFluids.unsafeSetTile(x, y, fluidNumber, fill)
        }
        else {
            layerFluids.unsafeSetTile(x, y, tileNameToNumberMap[Fluid.NULL]!!, 0f)
        }


        /*if (x == 60 && y == 256) {
            printdbg(this, "TileTerrain: ${getTileFromTerrain(x, y)}")
            printdbg(this, "fluidTypes[$addr] = ${fluidTypes[addr]} (should be ${fluidType.value})")
            printdbg(this, "fluidFills[$addr] = ${fluidFills[addr]} (should be $fill)")
        }*/
    }

    fun getFluid(x: Int, y: Int): FluidInfo {
        val (x, y) = coerceXY(x, y)
        val (type, fill) = layerFluids.unsafeGetTile1(x, y)
        var fluidID = tileNumberToNameMap[type.toLong()] ?: throw NullPointerException("No such fluid: $type")

        if (fluidID == Block.NULL || fluidID == Block.NOT_GENERATED)
            fluidID = Fluid.NULL

        return FluidInfo(fluidID, fill.ifNaN(0f)) // hex FFFFFFFF (magic number for ungenerated tiles) is interpreted as Float.NaN
    }

    /*private fun fluidTypeToBlock(type: FluidType) = when (type.abs()) {
        Fluid.NULL.value -> Block.AIR
        in Fluid.fluidRange -> GameWorld.TILES_SUPPORTED - type.abs()
        else -> throw IllegalArgumentException("Unsupported fluid type: $type")
    }*/

    data class FluidInfo(val type: ItemID = Fluid.NULL, val amount: Float = 0f) {
        /** test if this fluid should be considered as one */
        fun isFluid() = type != Fluid.NULL && amount >= FLUID_MIN_MASS
        fun getProp() = FluidCodex[type]
        override fun toString() = "Fluid type: ${type}, amount: $amount"
    }

    /**
     * Connection rules: connect to all nearby, except:
     *
     * If the wire allows 3- or 4-way connection, make such connection.
     * If the wire does not allow them (e.g. wire bridge, thicknet), connect top-bottom and left-right nodes.
     */
    data class WiringNode(
            val ws: SortedArrayList<ItemID> = SortedArrayList<ItemID>() // what could possibly go wrong bloating up the RAM footprint when it's practically infinite these days?
    )

    data class WireReceptionState(
            var dist: Int = -1, // how many tiles it took to traverse
            var src: Point2i = Point2i(0,0) // xy position
            // to get the state, use the src to get the state of the source emitter directly, then use dist to apply attenuation
    )

    /**
     * These values must be updated by none other than [WorldSimulator]()
     */
    data class WiringSimCell(
            var cnx: Int = 0, // connections. [1, 2, 4, 8] = [RIGHT, DOWN, LEFT, UP]
            val emt: Vector2 = Vector2(0.0, 0.0), // i'm emitting this much power
            val rcp: ArrayList<WireReceptionState> = ArrayList() // how far away are the power sources
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
        layerOres.dispose()
        layerFluids.dispose()
        //nullWorldInstance?.dispose() // must be called ONLY ONCE; preferably when the app exits

        disposed = true
    }

    override fun equals(other: Any?) = layerTerrain.ptr == (other as GameWorld).layerTerrain.ptr

    companion object {
        @Transient const val TERRAIN = 0
        @Transient const val WALL = 1
        @Transient const val ORES = 2
        @Transient const val FLUID = 3

        @Transient val TILES_SUPPORTED = ReferencingRanges.TILES.last + 1
        //@Transient val SIZEOF: Byte = 2
        @Transient const val LAYERS: Byte = 4 // terrain, wall (layerTerrainLowBits + layerWallLowBits), wire

        @Transient private var nullWorldInstance: GameWorld? = null

        fun makeNullWorld(): GameWorld {
            if (nullWorldInstance == null)
                nullWorldInstance = GameWorld(1, 1, 0, 0)

            return nullWorldInstance!!
        }

        val DEFAULT_GRAVITATION = Vector2(0.0, 9.8)

        @Transient const val CHUNK_NULL = 0x00.toByte()
        @Transient const val CHUNK_GENERATING = 0x01.toByte()
        @Transient const val CHUNK_LOADED = 0x02.toByte()
    }

    open fun updateWorldTime(delta: Float) {
        worldTime.update(delta)
    }
}

infix fun Int.fmod(other: Int) = Math.floorMod(this, other)
infix fun Long.fmod(other: Long) = Math.floorMod(this, other)
infix fun Float.fmod(other: Float) = if (this >= 0f) this % other else (this % other) + other
infix fun Double.fmod(other: Double) = if (this >= 0.0) this % other else (this % other) + other
