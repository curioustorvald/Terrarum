package net.torvald.terrarum.blockproperties

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.CSVFetcher
import org.apache.commons.csv.CSVRecord
import java.io.IOException

/**
 * Created by minjaesong on 2023-10-09.
 */
class FluidCodex {

    @Transient val fluidProps = HashMap<ItemID, FluidProp>()

    @Transient private val nullProp = FluidProp()


    operator fun get(fluidID: ItemID?): FluidProp {
        if (fluidID == null || fluidID == Fluid.NULL) {
            return nullProp
        }

        try {
            return if (fluidID.startsWith("fluid@"))
                fluidProps[fluidID]!!
            else
                throw NullPointerException("Missing prefix 'fluid@' for the ID '$fluidID'")
        }
        catch (e: NullPointerException) {
            throw NullPointerException("Fluidprop with id $fluidID does not exist.")
        }
    }


    /**
     * Later entry (possible from other modules) will replace older ones
     */
    fun fromModule(module: String, path: String, registerHook: (FluidProp) -> Unit = {}) {
        printdbg(this, "Building fluid properties table")
        try {
            register(module, CSVFetcher.readFromModule(module, path), registerHook)
        }
        catch (e: IOException) { e.printStackTrace() }
    }

    private fun register(module: String, records: List<CSVRecord>, registerHook: (FluidProp) -> Unit) {
        records.forEach {
            setProp(module, it.intVal("id"), it)
            val tileId = "fluid@$module:${it.intVal("id")}"
            fluidProps[tileId]?.let(registerHook)
        }
    }

    private fun setProp(module: String, key: Int, record: CSVRecord) {
        val prop = FluidProp()
        prop.nameKey = record.get("name")
        prop.tags = record.get("tags").split(',').map { it.trim().toUpperCase() }.toHashSet()

        prop.id = "fluid@$module:$key"
        prop.numericID = key

        prop.shadeColR = record.floatVal("shdr")
        prop.shadeColG = record.floatVal("shdg")
        prop.shadeColB = record.floatVal("shdb")
        prop.shadeColA = record.floatVal("shduv")
        prop.opacity = Cvec(prop.shadeColR, prop.shadeColG, prop.shadeColB, prop.shadeColA)

        prop.strength = record.intVal("str")
        prop.density = record.intVal("dsty")

        prop.lumColR = record.floatVal("lumr")
        prop.lumColG = record.floatVal("lumg")
        prop.lumColB = record.floatVal("lumb")
        prop.lumColA = record.floatVal("lumuv")
        prop.lumCol.set(prop.lumColR, prop.lumColG, prop.lumColB, prop.lumColA)

        prop.viscosity = record.intVal("vscs")
        prop.colour = record.str16ToInt("colour")

        prop.reflectance = record.floatVal("refl")

        prop.material = record.get("mate")

        fluidProps[prop.id] = prop

        printdbg(this, "Setting fluid prop ${prop.id} ->>\t${prop.nameKey}")
    }

}



class FluidProp {
    var id: ItemID = ""
    var numericID: Int = -1
    var nameKey: String = ""

    /** 1.0f for 1023, 0.25f for 255 */
    var shadeColR = 0f
    var shadeColG = 0f
    var shadeColB = 0f
    var shadeColA = 0f
    var opacity = Cvec()

    var strength: Int = 0
    var density: Int = 0

    var material: String = ""

    /** 1.0f for 1023, 0.25f for 255 */
    var lumColR = 0f
    var lumColG = 0f
    var lumColB = 0f
    var lumColA = 0f
    var lumCol = Cvec()

    /** Fluid colour */
    var colour: Int = 0

    var viscosity: Int = 0

    var reflectance = 0f // the exact colour of the reflected light depends on the texture

    @Transient var tags = HashSet<String>()

}