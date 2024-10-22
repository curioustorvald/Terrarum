package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockLayerGenericI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.GameWorld.Companion.TERRAIN
import net.torvald.terrarum.gameworld.GameWorld.Companion.WALL
import net.torvald.terrarum.linearSearchBy
import net.torvald.terrarum.utils.HashArray
import java.io.StringReader

/**
 * Created by minjaesong on 2023-10-17.
 */
class PointOfInterest(
    identifier: String,
    width: Int,
    height: Int,
    tileNumberToNameMap0: HashArray<ItemID>,
    tileNameToNumberMap0: HashMap<ItemID, Int>
) : Json.Serializable {

    constructor() : this("undefined", 0,0, HashArray(), HashMap())

    constructor(numberToNameMap: HashArray<ItemID>, nameToNumberMap: HashMap<ItemID, Int>) : this("undefined", 0, 0, numberToNameMap, nameToNumberMap)

    @Transient var w = width; private set
    @Transient var h = height; private set
    @Transient var layers = ArrayList<POILayer>(); private set
    @Transient var id = identifier; private set
    @Transient var tileNumberToNameMap: HashArray<ItemID> = tileNumberToNameMap0; private set
    @Transient var tileNameToNumberMap: HashMap<ItemID, Int> = tileNameToNumberMap0; private set
    @Transient lateinit var lutFromJson: HashArray<ItemID>
    @Transient var wlenFromJson = 0

    override fun write(json: Json) {
        val tileSymbolToItemId = HashArray<ItemID>() // exported

        val uniqueTiles = ArrayList(layers.flatMap { it.getUniqueTiles() }.toSet().toList().sorted()) // contains TileNumber


        // build tileSymbolToItemId
        uniqueTiles.forEachIndexed { index, tilenum ->
            tileSymbolToItemId[index.toLong()] = if (tilenum == 65535) // largest value on BlockLayerI16
                Block.NULL
            else
                tileNumberToNameMap[tilenum.toLong()] ?: throw NullPointerException("No tileNumber->tileName mapping for $tilenum")
        }

        val itemIDtoTileSym = tileSymbolToItemId.map { it.value to it.key }.toMap()

        val wordSize = if (tileSymbolToItemId.size >= 255) 16 else 8



//        printdbg(this, "unique tiles: ${tileSymbolToItemId.size}")
//        printdbg(this, "tileSymbolToItemId=$tileSymbolToItemId")
//        printdbg(this, "itemIDtoTileSym=$itemIDtoTileSym")



        layers.forEach { it.getReadyForSerialisation(tileNumberToNameMap, itemIDtoTileSym, wordSize / 8) }

        json.setTypeName(null)
        json.writeValue("genver", TerrarumAppConfiguration.VERSION_RAW)
        json.writeValue("id", id)
        json.writeValue("wlen", wordSize)
        json.writeValue("w", w)
        json.writeValue("h", h)
        json.writeValue("lut", tileSymbolToItemId)
        json.writeValue("layers", layers)
    }

    override fun read(json: Json, jsonData: JsonValue) {
        this.id = jsonData["id"].asString()
        this.w = jsonData["w"].asInt()
        this.h = jsonData["h"].asInt()
        this.lutFromJson = json.readValue(HashArray<ItemID>().javaClass, jsonData["lut"])
        this.wlenFromJson = jsonData["wlen"].asInt()


        println("read test")
        println("id: $id, w: $w, h: $h, wlen: $wlenFromJson")
        println("lut: $lutFromJson")

        println("==== layers (bytes) ====")

        // decompress layers
        jsonData["layers"].forEachIndexed { index, jsonData ->
            print("#${index+1}: ")
            POILayer().also {
                it.w = this.w; it.h = this.h; it.id = this.id
                it.read(json, jsonData)
                layers.add(it)
            }
        }
    }

    fun getReadyToBeUsed(itemIDtoTileNum: Map<ItemID, Int>) {
        layers.forEach {
            it.getReadyToBeUsed(lutFromJson, itemIDtoTileNum, w, h, wlenFromJson / 8)
        }
    }

    /**
     * Place the specified layers onto the world. The name of the layers are case-insensitive.
     */
    fun placeOnWorld(layerNames: List<String>, world: GameWorld, bottomCentreX: Int, bottomCenterY: Int) {
        val layers = layerNames.map { name ->
            (layers.linearSearchBy { it.name.equals(name, true) } ?: throw IllegalArgumentException("Layer with name '$name' not found"))
        }
        layers.forEach {
            it.placeOnWorld(world, bottomCentreX, bottomCenterY)
        }
    }
}

/**
 * @param name name of the layer, case-insensitive.
 */
class POILayer(
    name: String
) : Json.Serializable {
    constructor() : this("undefined")

    @Transient var name = name
    @Transient internal lateinit var blockLayer: ArrayList<BlockLayerGenericI16>
    @Transient internal lateinit var dat: Array<ByteArray>

    @Deprecated("Used for debug print", ReplaceWith("name")) @Transient internal var id = ""
    @Deprecated("Used for debug print") @Transient internal var w = 0
    @Deprecated("Used for debug print") @Transient internal var h = 0

    /**
     * @return list of unique tiles, in the form of TileNums
     */
    fun getUniqueTiles(): List<Int> {
        return blockLayer.flatMap { layer ->
            (0 until layer.height * layer.width).map { layer.unsafeGetTile(it % layer.width, it / layer.width) }
        }.toSet().toList().sorted()
    }

    /**
     * Converts `blockLayer` into `dat` internally, the hidden property used for the serialisation
     *
     * Tilenum: tile number in the block layer, identical to the any other block layers
     * TileSymbol: condensed version of the Tilenum, of which the higheset number is equal to the number of unique tiles used in the layer
     *
     * `tilenumToItemID[-1]` should return `Block.NULL`
     */
    fun getReadyForSerialisation(tilenumToItemID: HashArray<ItemID>, itemIDtoTileSym: Map<ItemID, Long>, byteLength: Int) {
        dat = blockLayer.map { layer ->
            ByteArray(layer.width * layer.height * byteLength) { i ->
                if (byteLength == 1) {
                    val tilenum = layer.unsafeGetTile(i % layer.width, i / layer.width).toLong()
                    val itemID = if (tilenum == 65535L) Block.NULL else tilenumToItemID[tilenum] ?: throw NullPointerException("No tileNumber->tileName mapping for $tilenum")
                    val tileSym = itemIDtoTileSym[itemID]!!
                    tileSym.toByte()
                }
                else if (byteLength == 2) {
                    val tilenum = layer.unsafeGetTile((i/2) % layer.width, (i/2) / layer.width).toLong()
                    val itemID = if (tilenum == 65535L) Block.NULL else tilenumToItemID[tilenum]!!
                    val tileSym = itemIDtoTileSym[itemID]!!
                    if (i % 2 == 0) tileSym.and(255).toByte() else tileSym.ushr(8).and(255).toByte()
                }
                else throw IllegalArgumentException()
            }
        }.toTypedArray()
    }

    /**
     * Converts `dat` into `blockLayer` so the Layer can be actually utilised.
     */
    fun getReadyToBeUsed(tileSymbolToItemId: HashArray<ItemID>, itemIDtoTileNum: Map<ItemID, Int>, width: Int, height: Int, byteLength: Int) {
        if (::blockLayer.isInitialized) {
            blockLayer.forEach { it.dispose() }
        }
        blockLayer = ArrayList<BlockLayerGenericI16>()

        dat.forEachIndexed { layerIndex, layer ->
            val currentBlockLayer = BlockLayerGenericI16(width, height).also {
                blockLayer.add(it)
            }
            for (w in 0 until layer.size / byteLength) {
                val word = if (byteLength == 1) layer[w].toUint() else if (byteLength == 2) layer.toULittleShort(2*w) else throw IllegalArgumentException("Illegal byteLength $byteLength")
                val x = w % width
                val y = w / width
                val itemID = tileSymbolToItemId[word.toLong()]!!
                val tile = if (itemID == Block.NULL)
                    -1
                else
                    itemIDtoTileNum[itemID]!!
                currentBlockLayer.unsafeSetTile(x, y, tile)
            }
        }
    }

    fun placeOnWorld(world: GameWorld, bottomCentreX: Int, bottomCenterY: Int) {
        val topLeftX = bottomCentreX - (blockLayer[0].width - 1) / 2
        val topLeftY = bottomCenterY - (blockLayer[0].height - 1)

        blockLayer.forEachIndexed { layerIndex, layer ->
            for (x in 0 until layer.width) { for (y in 0 until layer.height) {
                val tile = layer.unsafeGetTile(x, y)
                if (tile != -1 && tile != 65535) {
                    val (wx, wy) = world.coerceXY(x + topLeftX, y + topLeftY)
                    world.setTileOnLayerUnsafe(layerIndex, wx, wy, tile)
                }
            } }
        }
    }

    override fun write(json: Json) {
        if (!::dat.isInitialized) throw IllegalStateException("Internal data is not prepared! please run getReadyForSerialisation() before writing!")
        json.setTypeName(null)
        json.writeValue("name", name)
        json.writeValue("dat", dat)
    }

    override fun read(json: Json, jsonData: JsonValue) {
        name = jsonData["name"].asString()

        println(name)


        dat = jsonData["dat"].mapIndexed { index, value ->
            val zipdStr = value.asString()
            val ba = Common.strToBytes(StringReader(zipdStr)).toByteArray()
            val lname = "L${if (index == TERRAIN) "terr" else if (index == WALL) "wall" else "unk$index"}"
            if (ba.size != w * h) throw IllegalStateException("Layer size mismatch: expected ${w*h} but got ${ba.size} on POI $id Layer $name $lname")

            print("  $lname: ")
            print("(${ba.size})")
            println("[${ba.joinToString()}]")

            ba
        }.toTypedArray()
    }

}
