package net.torvald.terrarum.blockproperties

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.WireEmissionType
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.apache.commons.csv.CSVRecord
import java.io.IOException
/**
 * ItemCodex must be initialised first!
 *
 * Created by minjaesong on 2016-02-16.
 */
class WireCodex {

    @Transient val wireProps = HashMap<ItemID, WireProp>()
    @Transient private val nullProp = WireProp()

    @Transient val wirePorts = HashMap<WireEmissionType, Triple<TextureRegionPack, Int, Int>>()

    fun clear() {
        wireProps.clear()
    }

    internal constructor() {
        try {
            Terrarum.itemCodex["testtesttest"]
        }
        catch (e: UninitializedPropertyAccessException) {
            throw UninitializedPropertyAccessException("ItemCodex not initialised!")
        }
    }

    /**
     * `wire.csv` and texture for all wires are expected to be found in the given path.
     *
     * @param module name of the module
     * @param path to the "wires" directory, not path to the CSV; must end with a slash!
     */
    fun fromModule(module: String, path: String, blockRegisterHook: (WireProp) -> Unit) {
        printdbg(this, "Building wire properties table for module $module")
        try {
            register(module, path, CSVFetcher.readFromModule(module, path + "wires.csv"), blockRegisterHook)
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    fun fromCSV(module: String, path: String, csvString: String, blockRegisterHook: (WireProp) -> Unit) {
        printdbg(this, "Building wire properties table for module $module")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
                csvString,
                CSVFetcher.terrarumCSVFormat
        )
        val csvRecordList = csvParser.records
        csvParser.close()

        register(module, path, csvRecordList, blockRegisterHook)
    }

    private fun register(module: String, path: String, records: List<CSVRecord>, blockRegisterHook: (WireProp) -> Unit) {
        records.forEach {
            setProp(module, it.intVal("id"), it)
        }

        printdbg(this, "Registering wire textures into the resource pool")
        wireProps.keys.forEach { id ->
            val wireid = id.split(':').last().toInt()

            CommonResourcePool.addToLoadingList(id) {
                val t = TextureRegionPack(ModMgr.getGdxFile(module, "$path$wireid.tga"), TILE_SIZE, TILE_SIZE)
                /*return*/t
            }

            wireProps[id]?.let(blockRegisterHook)
        }

        CommonResourcePool.loadAll()

    }

    fun portsFromModule(module: String, path: String) {
        printdbg(this, "Building wire ports table for module $module")
        try {
            registerPorts(module, path, CSVFetcher.readFromModule(module, path + "wireports.csv"))
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    private fun registerPorts(module: String, path: String, records: List<CSVRecord>) {
        val spriteSheetFiles = ArrayList<Pair<FileHandle, String>>()
        val tempRecords = HashMap<WireEmissionType, Triple<String, Int, Int>>()

        records.forEach {
            val type = it.get("accepts")
            val fileModule = it.get("fileModule")
            val filePath = it.get("file")
            val x = it.get("xpos").toInt()
            val y = it.get("ypos").toInt()

            val file = ModMgr.getGdxFile(fileModule, filePath)
            val fileID = "wireport:$fileModule.${filePath.replace('\\','/')}"

            spriteSheetFiles.add(file to fileID)

            tempRecords[type] = Triple(fileID, x, y)
        }

        spriteSheetFiles.forEach { (file, id) ->
            CommonResourcePool.addToLoadingList(id) {
                TextureRegionPack(file, TILE_SIZE, TILE_SIZE)
            }
        }
        CommonResourcePool.loadAll()

        tempRecords.forEach { type, (fileID, x, y) ->
            wirePorts[type] = Triple(CommonResourcePool.getAsTextureRegionPack(fileID), x, y)
        }
    }

    fun getAll() = wireProps.values

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
            return nullProp
        }

        try {
            return wireProps[blockID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Wireprop with id $blockID does not exist.")
        }
    }

    fun getOrNull(blockID: ItemID?): WireProp? {//<O>
        return wireProps[blockID]
    }

    private fun setProp(modname: String, key: Int, record: CSVRecord) {
        val prop = WireProp()
        prop.nameKey = record.get("name")

        prop.id = "wire@$modname:$key"
        prop.numericID = key
        prop.renderClass = record.get("renderclass")
        prop.accepts = record.get("accept")
        prop.inputCount = record.intVal("inputcount")
        prop.inputType = record.get("inputtype") ?: prop.accepts
        prop.outputType = record.get("outputtype") ?: prop.accepts
        prop.canBranch = record.boolVal("branching")
        prop.tags = record.get("tags").split(',').map { it.trim().toUpperCase() }.toHashSet()

        wireProps[prop.id] = prop

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

        printdbg(this, "Setting prop ${prop.id} ->>\t${prop.nameKey}")
    }

    fun getAllWiresThatAccepts(accept: String): List<Pair<ItemID, WireProp>> {
        return wireProps.filter { it.value.accepts == accept }.toList()
    }

    fun getWirePortSpritesheet(emissionType: WireEmissionType): Triple<TextureRegionPack, Int, Int>? {
        return wirePorts[emissionType]
    }
}