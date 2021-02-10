package net.torvald.terrarum.blockproperties

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printmsg
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.util.SortedArrayList
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2016-02-16.
 */
object BlockCodex {

    private var blockProps = HashMap<ItemID, BlockProp>()

    val dynamicLights = SortedArrayList<ItemID>() // does not include virtual ones

    /** 65536 */
    //val MAX_TERRAIN_TILES = GameWorld.TILES_SUPPORTED

    private val nullProp = BlockProp()

    var highestNumber = -1 // does not include virtual ones
        private set

    // fake props for "randomised" dynamic lights
    const val DYNAMIC_RANDOM_CASES = 64
    private var virtualTileCursor = 0
    val tileToVirtual = HashMap<ItemID, List<ItemID>>()
    val virtualToTile = HashMap<ItemID, ItemID>()

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

                val numericID = intVal(it, "id")
                setProp(module, numericID, it)
                val tileId = "tile@$module:$numericID"

                // register tiles with dynamic light
                if ((blockProps[tileId]?.dynamicLuminosityFunction ?: 0) != 0) {
                    dynamicLights.add(tileId)

                    // add virtual props for dynamic lights
                    val virtualChunk = ArrayList<ItemID>()
                    repeat(DYNAMIC_RANDOM_CASES) { _ ->
                        val virtualID = "virt:$virtualTileCursor"

                        virtualToTile[virtualID] = tileId
                        virtualChunk.add(virtualID)

                        setProp("virt", virtualTileCursor, it)

                        printdbg(this, "Block ID $tileId -> Virtual ID $virtualID, baseLum: ${blockProps[virtualID]?.baseLumCol}")
                        virtualTileCursor += 1
                    }
                    tileToVirtual[tileId] = virtualChunk.sorted().toList()
                }
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getAll() = blockProps.values

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

    /*operator fun get(rawIndex: Int?): BlockProp {
        if (rawIndex == null || rawIndex == Block.NULL) {
            return nullProp
        }

        try {
            return blockProps[rawIndex]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $rawIndex does not exist.")
        }
    }*/
    operator fun get(blockID: ItemID?): BlockProp {
        if (blockID == null || blockID == "basegame:"+Block.NULL) {
            return nullProp
        }

        try {
            return blockProps[blockID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $blockID does not exist.")
        }
    }

    operator fun get(fluidType: FluidType?): BlockProp {
        // TODO fluid from other mods

        if (fluidType == null || fluidType.value == 0) {
            return blockProps["basegame:"+Block.AIR]!!
        }

        try {
            return blockProps["basegame:${fluidType.abs() + GameWorld.TILES_SUPPORTED - 1}"]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $fluidType does not exist.")
        }
    }

    fun getOrNull(blockID: ItemID?): BlockProp? {//<O>
        return blockProps[blockID]
    }

    private fun setProp(modname: String, key: Int, record: CSVRecord) {
        val prop = BlockProp()
        prop.nameKey = record.get("name")

        prop.id = "$modname:${(if (key == -1) 0 else intVal(record, "id"))}"
        prop.drop = "$modname:${intVal(record, "drop")}"

        prop.shadeColR = floatVal(record, "shdr")
        prop.shadeColG = floatVal(record, "shdg")
        prop.shadeColB = floatVal(record, "shdb")
        prop.shadeColA = floatVal(record, "shduv")
        prop.opacity = Cvec(prop.shadeColR, prop.shadeColG, prop.shadeColB, prop.shadeColA)

        prop.strength = intVal(record, "str")
        prop.density = intVal(record, "dsty")

        prop.baseLumColR = floatVal(record, "lumr")
        prop.baseLumColG = floatVal(record, "lumg")
        prop.baseLumColB = floatVal(record, "lumb")
        prop.baseLumColA = floatVal(record, "lumuv")
        prop.baseLumCol.set(prop.baseLumColR, prop.baseLumColG, prop.baseLumColB, prop.baseLumColA)

        prop.friction = intVal(record, "fr")
        prop.viscosity = intVal(record, "vscs")
        prop.colour = str16ToInt(record, "colour")

        //prop.isFluid = boolVal(record, "fluid")
        prop.isSolid = boolVal(record, "solid")
        //prop.isClear = boolVal(record, "clear")
        prop.isPlatform = boolVal(record, "plat")
        prop.isWallable = boolVal(record, "wall")
        prop.maxSupport = intVal(record, "grav")
        prop.isVertFriction = boolVal(record, "fv")

        prop.dynamicLuminosityFunction = intVal(record, "dlfn")

        blockProps["$modname:$key"] = prop

        printmsg(this, "$modname:${intVal(record, "id")}\t" + prop.nameKey)
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
    catch (e2: NullPointerException) {
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
    catch (e2: NullPointerException) {
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
    catch (e2: NullPointerException) {
    }

    return ret
}

fun boolVal(rec: CSVRecord, s: String) = intVal(rec, s) != 0