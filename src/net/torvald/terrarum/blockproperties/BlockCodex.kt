package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.gameworld.MapLayer
import net.torvald.terrarum.gameworld.PairedMapLayer
import org.apache.commons.csv.CSVRecord

import java.io.IOException

/**
 * Created by minjaesong on 16-02-16.
 */
object BlockCodex {

    private var blockProps: Array<BlockProp>

    const val TILE_UNIQUE_MAX = MapLayer.RANGE * PairedMapLayer.RANGE

    private val nullProp = BlockProp()

    init {
        blockProps = Array<BlockProp>(TILE_UNIQUE_MAX * 2, { BlockProp() })
    }

    operator fun invoke(module: String, path: String) {
        try {
            val records = CSVFetcher.readFromModule(module, path)

            println("[BlockCodex] Building block properties table")

            records.forEach {
                if (intVal(it, "blid") == -1) {
                    setProp(nullProp, it)
                }
                else {
                    setProp(blockProps[intVal(it, "blid")], it)
                }
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*fun get(index: Int): BlockProp {
        try {
            return blockProps[index]
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $index does not exist.")
        }
        catch (e1: ArrayIndexOutOfBoundsException) {
            if (index == -1) return nullProp
            else throw e1
        }
    }*/

    operator fun get(rawIndex: Int?): BlockProp {
        if (rawIndex == null || rawIndex == Block.NULL) {
            return nullProp
        }

        try {
            return blockProps[rawIndex]
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $rawIndex does not exist.")
        }
    }

    fun getOrNull(rawIndex: Int?): BlockProp? {
        if (rawIndex == null || rawIndex == Block.NULL) {
            return null
        }

        try {
            return blockProps[rawIndex]
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $rawIndex does not exist.")
        }
    }

    private fun setProp(prop: BlockProp, record: CSVRecord) {
        prop.nameKey = record.get("name")

        prop.id = intVal(record, "blid")
        prop.drop = intVal(record, "drid")

        prop.opacity = intVal(record, "opacity")
        prop.strength = intVal(record, "strength")
        prop.density = intVal(record, "dsty")
        prop.luminosity = intVal(record, "lumcolor")
        prop.friction = intVal(record, "friction")
        prop.viscosity = intVal(record, "vscs")

        prop.isFluid = boolVal(record, "fluid")
        prop.isSolid = boolVal(record, "solid")
        prop.isWallable = boolVal(record, "wall")
        prop.isFallable = boolVal(record, "fall")
        prop.isVertFriction = boolVal(record, "fv")

        prop.dynamicLuminosityFunction = intVal(record, "dlfn")

        print("${intVal(record, "blid")}")
        println("\t" + prop.nameKey)
    }

    private fun intVal(rec: CSVRecord, s: String): Int {
        var ret = -1
        try {
            ret = Integer.decode(rec.get(s))!!
        }
        catch (e: NullPointerException) {
        }

        return ret
    }

    private fun boolVal(rec: CSVRecord, s: String) = intVal(rec, s) != 0

    private fun formatNum2(i: Int) = if (i < 10) "0" + i else i.toString()
}
