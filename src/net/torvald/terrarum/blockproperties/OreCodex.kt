package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.App
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.CSVFetcher
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2023-10-11.
 */
class OreCodex {

    @Transient val oreProps = HashMap<ItemID, OreProp>()
    @Transient private val nullProp = OreProp()

    internal constructor()

    /**
     * Later entry (possible from other modules) will replace older ones
     */
    fun fromModule(module: String, path: String) {
        App.printdbg(this, "Building ore properties table")
        try {
            register(module, CSVFetcher.readFromModule(module, path))
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    fun fromCSV(module: String, csvString: String) {
        App.printdbg(this, "Building ore properties table for module $module")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
            csvString,
            CSVFetcher.terrarumCSVFormat
        )
        val csvRecordList = csvParser.records
        csvParser.close()

        register(module, csvRecordList)
    }

    private fun register(module: String, records: List<CSVRecord>) {
        records.forEach {
            setProp(module, it.intVal("id"), it)
        }
    }

    fun getOrNull(blockID: ItemID?): OreProp? {//<O>
        return oreProps[blockID]
    }

    private fun setProp(modname: String, key: Int, record: CSVRecord) {
        val prop = OreProp()
        prop.id = "ores@$modname:$key"
        prop.tags = record.get("tags").split(',').map { it.trim().toUpperCase() }.toHashSet()
        prop.item = record.get("item").let { if (it == null) "" else if (it.contains(':')) it else "$modname:$it" }

        oreProps[prop.id] = prop

        App.printdbg(this, "Setting prop ${prop.id}")
    }

    fun getAll() = oreProps.values

    operator fun get(oreID: ItemID?): OreProp {
        if (oreID == null || oreID == Block.NULL) {
            return nullProp
        }

        try {
            return oreProps[oreID]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Oreprop with id $oreID does not exist.")
        }
    }
}


class OreProp {
    var id: String = ""
    var item: ItemID = ""
    var tags = HashSet<String>()

    fun hasTag(s: String) = tags.contains(s)

}