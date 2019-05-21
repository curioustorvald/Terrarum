package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printmsg
import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.MapLayer
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.worlddrawer.Cvec
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2016-02-16.
 */
object BlockCodex {

    private var blockProps = HashMap<Int, BlockProp>()

    /** 4096 */
    const val MAX_TERRAIN_TILES = MapLayer.RANGE * PairedMapLayer.RANGE

    private val nullProp = BlockProp()

    var highestNumber = -1
        private set

    /**
     * Later entry (possible from other modules) will replace older ones
     */
    operator fun invoke(module: String, path: String) {
        try {
            val records = CSVFetcher.readFromModule(module, path)

            AppLoader.printmsg(this, "Building block properties table")

            records.forEach {
                /*if (intVal(it, "id") == -1) {
                    setProp(nullProp, it)
                }
                else {
                    setProp(blockProps[intVal(it, "id")], it)
                }*/

                val id = intVal(it, "id")
                setProp(id, it)

                if (id > highestNumber)
                    highestNumber = id
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
            return blockProps[rawIndex]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $rawIndex does not exist.")
        }
    }

    operator fun get(fluidType: FluidType?): BlockProp {
        if (fluidType == null || fluidType.value == 0) {
            return blockProps[Block.AIR]!!
        }

        try {
            return blockProps[fluidType.abs() + GameWorld.TILES_SUPPORTED - 1]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $fluidType does not exist.")
        }
    }

    fun getOrNull(rawIndex: Int?): BlockProp? {
        return blockProps[rawIndex]
    }

    private fun setProp(key: Int, record: CSVRecord) {
        val prop = BlockProp()
        prop.nameKey = record.get("name")

        prop.id = if (key == -1) 0 else intVal(record, "id")
        prop.drop = intVal(record, "drop")

        prop.shadeColR = floatVal(record, "shdr") / LightmapRenderer.MUL_FLOAT
        prop.shadeColG = floatVal(record, "shdg") / LightmapRenderer.MUL_FLOAT
        prop.shadeColB = floatVal(record, "shdb") / LightmapRenderer.MUL_FLOAT
        prop.shadeColA = floatVal(record, "shduv") / LightmapRenderer.MUL_FLOAT
        prop.opacity = Cvec(prop.shadeColR, prop.shadeColG, prop.shadeColB, prop.shadeColA)

        prop.strength = intVal(record, "str")
        prop.density = intVal(record, "dsty")

        prop.lumColR = floatVal(record, "lumr") / LightmapRenderer.MUL_FLOAT
        prop.lumColG = floatVal(record, "lumg") / LightmapRenderer.MUL_FLOAT
        prop.lumColB = floatVal(record, "lumb") / LightmapRenderer.MUL_FLOAT
        prop.lumColA = floatVal(record, "lumuv") / LightmapRenderer.MUL_FLOAT
        prop.internalLumCol = Cvec(prop.lumColR, prop.lumColG, prop.lumColB, prop.lumColA)

        prop.friction = intVal(record, "fr")
        prop.viscosity = intVal(record, "vscs")
        prop.colour = str16ToInt(record, "colour")

        //prop.isFluid = boolVal(record, "fluid")
        prop.isSolid = boolVal(record, "solid")
        //prop.isClear = boolVal(record, "clear")
        prop.isPlatform = boolVal(record, "plat")
        prop.isWallable = boolVal(record, "wall")
        prop.isFallable = boolVal(record, "fall")
        prop.isVertFriction = boolVal(record, "fv")

        prop.dynamicLuminosityFunction = intVal(record, "dlfn")

        blockProps[key] = prop

        printmsg(this, "${intVal(record, "id")}\t" + prop.nameKey)
    }
}

fun str16ToInt(rec: CSVRecord, s: String): Int {
    var ret = 0
    try {
        ret = rec.get(s).toLong(16).toInt()
    }
    catch (e: NumberFormatException) {
    }
    catch (e1: IllegalStateException) {
    }

    return ret
}

fun intVal(rec: CSVRecord, s: String): Int {
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

fun floatVal(rec: CSVRecord, s: String): Float {
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

fun boolVal(rec: CSVRecord, s: String) = intVal(rec, s) != 0