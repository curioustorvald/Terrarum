package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.linearSearchBy
import net.torvald.terrarum.utils.HashArray

/**
 * Created by minjaesong on 2023-10-17.
 */
class PointOfInterest(
    identifier: String,
    width: Int,
    height: Int
) : Json.Serializable {

    constructor() : this("undefined", 0,0)

    @Transient val w = width
    @Transient val h = height
    @Transient val layers = ArrayList<POILayer>()
    @Transient val id = identifier

    override fun write(json: Json) {
        val tileSymbolToItemId = HashArray<ItemID>() // exported
        val tilenumToItemID = HashMap<Int, ItemID>() // not exported

        // TODO populate above vars. Block.NULL is always integer -1

        val itemIDtoTileSym = tileSymbolToItemId.map { it.value to it.key }.toMap()

        val wordSize = if (tileSymbolToItemId.size >= 255) 16 else 8
        layers.forEach { it.getReadyForSerialisation(tilenumToItemID, itemIDtoTileSym, wordSize / 8) }

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
        TODO("Not yet implemented")
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

    @Transient val name = name
    @Transient internal lateinit var blockLayer: ArrayList<BlockLayerI16>
    @Transient internal lateinit var dat: Array<ByteArray>

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
     * `itemIDtoTileSym[Block.NULL]` should return -1, so that it would return 0xFF on any length of the word
     */
    fun getReadyForSerialisation(tilenumToItemID: Map<Int, ItemID>, itemIDtoTileSym: Map<ItemID, Long>, byteLength: Int) {
        dat = blockLayer.map { layer ->
            ByteArray(layer.width * layer.height * byteLength) { i ->
                if (byteLength == 1) {
                    itemIDtoTileSym[tilenumToItemID[layer.unsafeGetTile(i % layer.width, i / layer.width)]!!]!!.toByte()
                }
                else if (byteLength == 2) {
                    val tileSym = itemIDtoTileSym[tilenumToItemID[layer.unsafeGetTile((i/2) % layer.width, (i/2) / layer.width)]!!]!!
                    if (i % 2 == 0) tileSym.and(255).toByte() else tileSym.ushr(8).and(255).toByte()
                }
                else throw IllegalArgumentException()
            }
        }.toTypedArray()
    }

    /**
     * Converts `dat` into `blockLayer` so the Layer can be actually utilised.
     *
     * `itemIDtoTileNum[Block.NULL]` should return `-1`
     * `tileSymbolToItemId[255]` and `tileSymbolToItemId[65535]` should return `Block.NULL`
     */
    fun getReadyToBeUsed(tileSymbolToItemId: HashArray<ItemID>, itemIDtoTileNum: Map<ItemID, Int>, width: Int, height: Int, byteLength: Int) {
        if (::blockLayer.isInitialized) {
            blockLayer.forEach { it.dispose() }
        }
        blockLayer = ArrayList<BlockLayerI16>()

        dat.forEachIndexed { layerIndex, layer ->
            val currentBlockLayer = BlockLayerI16(width, height).also {
                blockLayer[layerIndex] = it
            }
            for (w in 0 until layer.size / byteLength) {
                val word = if (byteLength == 1) layer[w].toUint() else if (byteLength == 2) layer.toULittleShort(2*w) else throw IllegalArgumentException()
                val x = w % width
                val y = w / width
                val tile = itemIDtoTileNum[tileSymbolToItemId[word.toLong()]!!]!!
                currentBlockLayer.unsafeSetTile(x, y, tile)
            }
        }
    }

    fun placeOnWorld(world: GameWorld, bottomCentreX: Int, bottomCenterY: Int) {
        val topLeftX = bottomCentreX - blockLayer[0].width / 2
        val topLeftY = bottomCenterY - blockLayer[0].height

        blockLayer.forEachIndexed { layerIndex, layer ->
            for (x in 0 until layer.width) { for (y in 0 until layer.height) {
                val tile = layer.unsafeGetTile(x, y)
                if (tile != -1) {
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

    override fun read(json: Json?, jsonData: JsonValue?) {
        TODO("Not yet implemented")
    }

}
