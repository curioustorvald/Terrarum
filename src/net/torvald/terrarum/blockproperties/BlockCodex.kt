package net.torvald.terrarum.blockproperties

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ReferencingRanges.PREFIX_VIRTUALTILE
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.util.SortedArrayList
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2016-02-16.
 */
class BlockCodex {

    @Transient val blockProps = HashMap<ItemID, BlockProp>()

    @Transient val dynamicLights = SortedArrayList<ItemID>() // does not include virtual ones

    /** 65536 */
    //val MAX_TERRAIN_TILES = GameWorld.TILES_SUPPORTED

    @Transient private val nullProp = BlockProp()

    @Transient var highestNumber = -1 // does not include virtual ones
        private set

    // fake props for "randomised" dynamic lights
    @Transient val DYNAMIC_RANDOM_CASES = 64
    @Transient private var virtualTileCursor = 1

    /**
     * One-to-Many
     */
    @Transient val tileToVirtual = HashMap<ItemID, List<ItemID>>()

    /**
     * Many-to-One
     */
    @Transient val virtualToTile = HashMap<ItemID, ItemID>()

    fun clear() {
        blockProps.clear()
        dynamicLights.clear()
        highestNumber = -1
        virtualTileCursor = 1
        tileToVirtual.clear()
        virtualToTile.clear()
    }

    internal constructor()

    /**
     * Later entry (possible from other modules) will replace older ones
     */
    fun fromModule(module: String, path: String, blockRegisterHook: (BlockProp) -> Unit) {
        printdbg(this, "Building block properties table")
        try {
            register(module, CSVFetcher.readFromModule(module, path), blockRegisterHook)
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    fun fromCSV(module: String, csvString: String, blockRegisterHook: (BlockProp) -> Unit) {
        printdbg(this, "Building block properties table for module $module")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
                csvString,
                CSVFetcher.terrarumCSVFormat
        )
        val csvRecordList = csvParser.records
        csvParser.close()

        register(module, csvRecordList, blockRegisterHook)
    }

    private fun register(module: String, records: List<CSVRecord>, blockRegisterHook: (BlockProp) -> Unit) {
        records.forEach {
            val id = it.intVal("id")
            /*if (it.intVal("id") == -1) {
                setProp(nullProp, it)
            }
            else {
                setProp(blockProps[it.intVal("id")], it)
            }*/

            setProp(module, id, it)
            val tileId = "$module:$id"

            // register tiles with dynamic light
            if ((blockProps[tileId]?.dynamicLuminosityFunction ?: 0) != 0) {
                dynamicLights.add(tileId)

                // add virtual props for dynamic lights
                val virtualChunk = ArrayList<ItemID>()
                repeat(DYNAMIC_RANDOM_CASES) { _ ->
                    val virtualID = "$PREFIX_VIRTUALTILE:$virtualTileCursor"

                    virtualToTile[virtualID] = tileId
                    virtualChunk.add(virtualID)

                    setProp(PREFIX_VIRTUALTILE, virtualTileCursor, it)

                    printdbg(this, "Block ID $tileId -> Virtual ID $virtualID, baseLum: ${blockProps[virtualID]?.baseLumCol}")
                    virtualTileCursor += 1
                }
                tileToVirtual[tileId] = virtualChunk.sorted().toList()
            }


            blockProps[tileId]?.let(blockRegisterHook)
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
    fun hasProp(blockID: ItemID?): Boolean = if (blockID == null) false else blockProps.containsKey(blockID)

    operator fun get(blockID: ItemID?): BlockProp {
        if (blockID == null || blockID == Block.NULL) {
            return nullProp
        }

        try {
            return if (blockID.isWall())
                blockProps[blockID.substring(5)]!!
            else
                blockProps[blockID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $blockID does not exist.")
        }
    }

    /*operator fun get(fluidType: FluidType?): BlockProp {
        // TODO fluid from other mods

        if (fluidType == null || fluidType == Fluid.NULL) {
            return blockProps[Block.AIR]!!
        }

        try {
            return blockProps["basegame:${fluidType.abs() + GameWorld.TILES_SUPPORTED - 1}"]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $fluidType does not exist.")
        }
    }*/

    fun getOrNull(blockID: ItemID?): BlockProp? {//<O>
        return blockProps[blockID]
    }

    fun filter(predicate: (BlockProp) -> Boolean) = blockProps.entries.filter { (_, prop) -> predicate(prop) }

    private fun setProp(modname: String, key: Int, record: CSVRecord) {
        val prop = BlockProp()
        prop.nameKey = record.get("name")
        prop.tags = record.get("tags").split(',').map { it.trim().toUpperCase() }.toHashSet()

        prop.id = "$modname:$key"
        prop.numericID = key
        // drop and world should be interpreted as:
        //   "N/A" -> empty string
        //   otherModname:id -> as-is
        //   id -> thisModname:id
        prop.drop = record.get("drop").let { if (it == null) "" else if (it.contains(':')) it else "$modname:$it" }
        prop.world = record.get("spawn").let { if (it == null) "" else if (it.contains(':')) it else "$modname:$it" }

        prop.shadeColR = record.floatVal("shdr")
        prop.shadeColG = record.floatVal("shdg")
        prop.shadeColB = record.floatVal("shdb")
        prop.shadeColA = record.floatVal("shduv")
        prop.opacity = Cvec(prop.shadeColR, prop.shadeColG, prop.shadeColB, prop.shadeColA)

        prop.strength = record.intVal("str")
        prop.density = record.intVal("dsty")

        prop.baseLumColR = record.floatVal("lumr")
        prop.baseLumColG = record.floatVal("lumg")
        prop.baseLumColB = record.floatVal("lumb")
        prop.baseLumColA = record.floatVal("lumuv")
        prop.baseLumCol.set(prop.baseLumColR, prop.baseLumColG, prop.baseLumColB, prop.baseLumColA)

        prop.friction = record.intVal("fr")

        //prop.isFluid = record.boolVal("fluid")
        prop.isSolid = record.boolVal("solid")
        //prop.isClear = record.boolVal("clear")

        prop.isPlatform = prop.hasTag("PLATFORM")
        prop.isActorBlock = prop.hasTag("ACTORBLOCK")

        prop.isWallable = record.boolVal("wall")
        prop.maxSupport = record.intVal("grav")
        prop.isVertFriction = record.boolVal("fv")

        prop.dynamicLuminosityFunction = record.intVal("dlfn")

        prop.reflectance = record.floatVal("refl")

        prop.material = record.get("mate")

        blockProps[prop.id] = prop

        printdbg(this, "Setting prop ${prop.id} ->>\t${prop.nameKey}\tsolid:${prop.isSolid}")
    }
}

fun CSVRecord.str16ToInt(s: String): Int {
    var ret = 0
    try {
        ret = this.get(s).toLong(16).toInt()
    }
    catch (e: NumberFormatException) { }
    catch (e1: IllegalStateException) { }
    catch (e2: NullPointerException) { }

    return ret
}

fun CSVRecord.intVal(s: String): Int {
    var ret = -1
    try {
        ret = this.get(s).toInt()
    }
    catch (e: NumberFormatException) { }
    catch (e1: IllegalStateException) { }
    catch (e2: NullPointerException) { }

    return ret
}

fun CSVRecord.floatVal(s: String): Float {
    var ret = -1f
    try {
        ret = this.get(s).toFloat()
    }
    catch (e: NumberFormatException) { }
    catch (e1: IllegalStateException) { }
    catch (e2: NullPointerException) { }

    return ret
}

fun CSVRecord.boolVal(s: String) = this.intVal(s) != 0