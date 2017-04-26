package net.torvald.terrarum.blockproperties

import net.torvald.CSVFetcher
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
                if (intVal(it, "sid") == -1) {
                    setProp(nullProp, it)
                }
                else {
                    setProp(
                            blockProps[idDamageToIndex(intVal(it, "id"), intVal(it, "sid"))], it
                    )
                }
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun get(index: Int, subID: Int): BlockProp {
        try {
            blockProps[idDamageToIndex(index, subID)].id
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $index and subID $subID does not exist.")
        }

        return blockProps[idDamageToIndex(index, subID)]
    }

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

    private fun setProp(prop: BlockProp, record: CSVRecord) {
        prop.nameKey = record.get("name")

        prop.id = idDamageToIndex(intVal(record, "id"), intVal(record, "sid"))

        prop.opacity = intVal(record, "opacity")
        prop.strength = intVal(record, "strength")
        prop.density = intVal(record, "dsty")
        prop.luminosity = intVal(record, "lumcolor")
        prop.drop = intVal(record, "drop")
        prop.dropDamage = intVal(record, "ddmg")
        prop.friction = intVal(record, "friction")
        prop.viscosity = intVal(record, "vscs")

        prop.isFluid = boolVal(record, "fluid")
        prop.isSolid = boolVal(record, "solid")
        prop.isWallable = boolVal(record, "wall")
        prop.isFallable = boolVal(record, "fall")
        prop.isVertFriction = boolVal(record, "fv")

        prop.dynamicLuminosityFunction = intVal(record, "dlfn")

        print(formatNum3(intVal(record, "id")) + ":" + formatNum2(intVal(record, "sid")))
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

    fun idDamageToIndex(index: Int, damage: Int) = index * PairedMapLayer.RANGE + damage

    private fun formatNum3(i: Int): String {
        if (i < 10)
            return "00" + i
        else if (i < 100)
            return "0" + i
        else
            return i.toString()
    }

    private fun formatNum2(i: Int) = if (i < 10) "0" + i else i.toString()
}
