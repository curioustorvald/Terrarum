package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.intVal
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.CSVFetcher
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2025-04-29.
 */
class CanistersCodex {

    @Transient val CanisterProps = HashMap<ItemID, CanisterProp>() // itemID is <modulename>_<index>

    @Transient private val nullProp = CanisterProp()


    operator fun get(canister: ItemID?): CanisterProp {
        if (canister == null || canister.substringAfter('_').substringBefore(':').substringBefore('@') == "0") {
            return nullProp
        }

        try {
            return CanisterProps[canister]!!
        }
        catch (e: NullPointerException) {
            throw NullPointerException("CanisterProp with id $canister does not exist.")
        }
    }

    fun getOrNull(blockID: ItemID?): CanisterProp? {
        return CanisterProps[blockID]
    }

    /**
     * Later entry (possible from other modules) will replace older ones
     */
    fun fromModule(module: String, path: String, registerHook: (CanisterProp) -> Unit = {}) {
        printdbg(this, "Building fluid properties table")
        try {
            register(module, CSVFetcher.readFromModule(module, path), registerHook)
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    private fun register(module: String, records: List<CSVRecord>, registerHook: (CanisterProp) -> Unit) {
        records.forEach {
            setProp(module, it.intVal("id"), it)
            val tileId = "${module}_${it.intVal("id")}"
            CanisterProps[tileId]?.let(registerHook)
        }
    }

    private fun setProp(module: String, key: Int, record: CSVRecord) {
        val prop = CanisterProp()
        prop.tags = record.get("tags").split(',').map { it.trim().toUpperCase() }.toHashSet()
        prop.id = "${module}_$key"
        prop.itemID = record.get("itemid")

        CanisterProps[prop.id] = prop

        printdbg(this, "Setting canister prop ${prop.id}")
    }
}

/**
 * Created by minjaesong on 2025-04-29.
 */
class CanisterProp {
    var id: ItemID = ""
    var itemID: ItemID = ""
    @Transient var tags = HashSet<String>()
}
