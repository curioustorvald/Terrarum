package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.AppLoader.printmsg
import net.torvald.terrarum.blockproperties.floatVal
import net.torvald.terrarum.blockproperties.intVal
import net.torvald.terrarum.utils.CSVFetcher
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
}

object MaterialCodex {

    private var materialProps = HashMap<String, Material>()
    private val nullMaterial = Material()

    operator fun invoke(module: String, path: String) {
        try {
            val records = CSVFetcher.readFromModule(module, path)

            printmsg(this, "Building materials table")

            records.forEach {
                val prop = Material()
                prop.strength = intVal(it, "tens")
                prop.density = intVal(it, "dsty")
                prop.forceMod = intVal(it, "fmod")
                prop.enduranceMod = floatVal(it, "endurance")
                prop.thermalConductivity = floatVal(it, "tcond")
                prop.identifier = it.get("idst").toUpperCase()

                materialProps[prop.identifier] = prop

                printmsg(this, "${prop.identifier}\t${prop.strength}\t${prop.density}\t${prop.forceMod}\t${prop.enduranceMod}")
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    operator fun get(identifier: String) = try {
        materialProps[identifier.toUpperCase()]!!
    }
    catch (e: NullPointerException) {
        throw NullPointerException("Material with id $identifier does not exist.")
    }

    fun getOrDefault(identifier: String) = materialProps[identifier.toUpperCase()] ?: nullMaterial

}