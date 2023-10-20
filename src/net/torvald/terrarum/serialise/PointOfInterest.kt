package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockLayerI16
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
}

class POILayer(
    name: String
) : Json.Serializable {
    constructor() : this("undefined")

    @Transient val name = name
    @Transient val blockLayer = ArrayList<BlockLayerI16>()
    @Transient private lateinit var dat: Array<ByteArray>

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
     */
    fun getReadyToBeUsed(tileSymbolToItemId: HashArray<ItemID>, itemIDtoTileNum: Map<ItemID, Int>, width: Int, height: Int, byteLength: Int) {
        blockLayer.forEach { it.dispose() }
        blockLayer.clear()

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

    override fun write(json: Json) {
        json.setTypeName(null)
        json.writeValue("name", name)
        json.writeValue("dat", dat)
    }

    override fun read(json: Json?, jsonData: JsonValue?) {
        TODO("Not yet implemented")
    }

}
