package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.gameworld.MapLayer
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import org.apache.commons.csv.CSVRecord

import java.io.IOException

/**
 * Created by minjaesong on 2016-02-16.
 */
object BlockCodex {

    private var blockProps: Array<BlockProp>

    const val TILE_UNIQUE_MAX = MapLayer.RANGE * PairedMapLayer.RANGE

    private val nullProp = BlockProp()

    init {
        blockProps = Array<BlockProp>(TILE_UNIQUE_MAX * 2, { BlockProp() })
    }

    /**
     * Later entry (possible from other modules) will replace older ones
     */
    operator fun invoke(module: String, path: String) {
        try {
            val records = CSVFetcher.readFromModule(module, path)

            AppLoader.printdbg(this, "Building block properties table")

            records.forEach {
                if (intVal(it, "id") == -1) {
                    setProp(nullProp, it)
                }
                else {
                    setProp(blockProps[intVal(it, "id")], it)
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

        prop.id = intVal(record, "id")
        prop.drop = intVal(record, "drop")

        prop.shadeColR = floatVal(record, "shdr") / LightmapRenderer.MUL_FLOAT
        prop.shadeColG = floatVal(record, "shdg") / LightmapRenderer.MUL_FLOAT
        prop.shadeColB = floatVal(record, "shdb") / LightmapRenderer.MUL_FLOAT
        prop.shadeColA = floatVal(record, "shduv") / LightmapRenderer.MUL_FLOAT

        prop.strength = intVal(record, "strength")
        prop.density = intVal(record, "dsty")

        prop.lumColR = floatVal(record, "lumr") / LightmapRenderer.MUL_FLOAT
        prop.lumColG = floatVal(record, "lumg") / LightmapRenderer.MUL_FLOAT
        prop.lumColB = floatVal(record, "lumb") / LightmapRenderer.MUL_FLOAT
        prop.lumColA = floatVal(record, "lumuv") / LightmapRenderer.MUL_FLOAT

        prop.friction = intVal(record, "friction")
        prop.viscosity = intVal(record, "vscs")

        prop.isFluid = boolVal(record, "fluid")
        prop.isSolid = boolVal(record, "solid")
        prop.isClear = boolVal(record, "clear")
        prop.isWallable = boolVal(record, "wall")
        prop.isFallable = boolVal(record, "fall")
        prop.isVertFriction = boolVal(record, "fv")

        prop.dynamicLuminosityFunction = intVal(record, "dlfn")

        print("${intVal(record, "id")}")
        println("\t" + prop.nameKey)
    }

    private fun intVal(rec: CSVRecord, s: String): Int {
        var ret = -1
        try {
            ret = rec.get(s).toInt()
        }
        catch (e: NumberFormatException) {
        }
        catch (e1: IllegalStateException) {
        }

        return ret
    }

    private fun floatVal(rec: CSVRecord, s: String): Float {
        var ret = -1f
        try {
            ret = rec.get(s).toFloat()
        }
        catch (e: NumberFormatException) {
        }
        catch (e1: IllegalStateException) {
        }

        return ret
    }

    private fun boolVal(rec: CSVRecord, s: String) = intVal(rec, s) != 0
}
