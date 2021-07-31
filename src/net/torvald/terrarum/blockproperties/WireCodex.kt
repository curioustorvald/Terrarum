package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2016-02-16.
 */
object WireCodex {

    private var wireProps = HashMap<ItemID, WireProp>()

    private val nullProp = WireProp()

    /**
     * `wire.csv` and texture for all wires are expected to be found in the given path.
     *
     * @param module name of the module
     * @param path to the "wires" directory, not path to the CSV; must end with a slash!
     */
    operator fun invoke(module: String, path: String) {
        AppLoader.printmsg(this, "Building wire properties table for module $module")

        try {
            val records = CSVFetcher.readFromModule(module, path + "wires.csv")


            records.forEach {
                WireCodex.setProp(module, intVal(it, "id"), it)
            }

            AppLoader.printmsg(this, "Registering wire textures into the resource pool")
            wireProps.keys.forEach { id ->
                val wireid = id.split(':').last().toInt()

                CommonResourcePool.addToLoadingList(id) {
                    val t = TextureRegionPack(ModMgr.getPath(module, "$path$wireid.tga"), TILE_SIZE, TILE_SIZE)
                    /*return*/t
                }
            }

            CommonResourcePool.loadAll()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getAll() = WireCodex.wireProps.values

    /*fun get(index: Int): WireProp {
        try {
            return wireProps[index]
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with id $index does not exist.")
        }
        catch (e1: ArrayIndexOutOfBoundsException) {
            if (index == -1) return nullProp
            else throw e1
        }
    }*/

    /*operator fun get(rawIndex: Int?): WireProp {
        if (rawIndex == null || rawIndex == Block.NULL) {
            return nullProp
        }

        try {
            return wireProps[rawIndex]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Blockprop with raw id $rawIndex does not exist.")
        }
    }*/
    operator fun get(blockID: ItemID?): WireProp {
        if (blockID == null || blockID == "basegame:"+Block.NULL) {
            return WireCodex.nullProp
        }

        try {
            return WireCodex.wireProps[blockID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Wireprop with id $blockID does not exist.")
        }
    }

    fun getOrNull(blockID: ItemID?): WireProp? {//<O>
        return WireCodex.wireProps[blockID]
    }

    private fun setProp(modname: String, key: Int, record: CSVRecord) {
        val prop = WireProp()
        prop.nameKey = record.get("name")

        prop.id = "wire@$modname:$key"
        prop.renderClass = record.get("renderclass")
        prop.accepts = record.get("accept")
        prop.inputCount = intVal(record, "inputcount")
        prop.inputType = record.get("inputtype") ?: prop.accepts
        prop.outputType = record.get("outputtype") ?: prop.accepts

        WireCodex.wireProps[prop.id] = prop

        // load java class
        val invImgRef = record.get("inventoryimg").split(',')
        val invImgSheet = invImgRef[0]
        val invImgX = invImgRef[1].toInt()
        val invImgY = invImgRef[2].toInt()

        val className = record.get("javaclass")
        val loadedClass = Class.forName(className)
        val loadedClassConstructor = loadedClass.getConstructor(ItemID::class.java, String::class.java, Int::class.java, Int::class.java)
        val loadedClassInstance = loadedClassConstructor.newInstance(prop.id, invImgSheet, invImgX, invImgY)
        ItemCodex[prop.id] = loadedClassInstance as GameItem

        AppLoader.printmsg(this, "Setting prop ${prop.id} ->>\t${prop.nameKey}")
    }
}