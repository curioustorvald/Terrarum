package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printmsg
import net.torvald.terrarum.Codex
import net.torvald.terrarum.blockproperties.floatVal
import net.torvald.terrarum.blockproperties.intVal
import net.torvald.terrarum.utils.CSVFetcher
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * To be used with items AND TILES (electricity resistance, thermal conductivity)
 *
 * Created by minjaesong on 2016-03-18.
 */
class Material {
    var strength: Int = 1 // actually tensile strength
    var density: Int = 1000 // grams per litre

    var thermalConductivity: Float = 10f // watts per metre-kelven

    var forceMod: Int = 1 // arbitrary unit. See Pickaxe_Power.xlsx
    var enduranceMod: Float = 1f // multiplier. Copper as 1.0
    //var armourMod: Float // multiplier. Copper as 1.0

    var durability: Int = 0 // tools only

    var identifier: String = "Name not set"

    /**
     * Mainly intended to be used by third-party modules
     */
    val extra = Codex()
}

class MaterialCodex {

    @Transient val materialProps = HashMap<String, Material>()
    @Transient private val nullMaterial = Material()

    internal constructor()

    fun fromModule(module: String, path: String) {
        App.printmsg(this, "Building material properties table")
        try {
            register(CSVFetcher.readFromModule(module, path))
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    fun fromCSV(module: String, csvString: String) {
        val csvParser = org.apache.commons.csv.CSVParser.parse(
                csvString,
                CSVFetcher.terrarumCSVFormat
        )
        val csvRecordList = csvParser.records
        csvParser.close()
        register(csvRecordList)
    }

    private fun register(records: List<CSVRecord>) {
        records.forEach {
            val prop = Material()
            prop.strength = it.intVal("tens")
            prop.density = it.intVal("dsty")
            prop.forceMod = it.intVal("fmod")
            prop.enduranceMod = it.floatVal("endurance")
            prop.thermalConductivity = it.floatVal("tcond")
            prop.identifier = it.get("idst").toUpperCase()

            materialProps[prop.identifier] = prop

            printmsg(this, "${prop.identifier}\t${prop.strength}\t${prop.density}\t${prop.forceMod}\t${prop.enduranceMod}")
        }
    }

    fun clear() = materialProps.clear()

    operator fun get(identifier: String) = try {
        materialProps[identifier.toUpperCase()]!!
    }
    catch (e: NullPointerException) {
        throw NullPointerException("Material with id $identifier does not exist.")
    }

    fun getOrDefault(identifier: String) = materialProps[identifier.toUpperCase()] ?: nullMaterial

}