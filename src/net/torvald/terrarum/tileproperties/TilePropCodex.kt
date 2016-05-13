package net.torvald.terrarum.tileproperties

import net.torvald.CSVFetcher
import net.torvald.terrarum.gamemap.MapLayer
import net.torvald.terrarum.gamemap.PairedMapLayer
import org.apache.commons.csv.CSVRecord

import java.io.IOException

/**
 * Created by minjaesong on 16-02-16.
 */
class TilePropCodex {

    init {
        tileProps = Array<TileProp>(TILE_UNIQUE_MAX + 1,
                {i -> TileProp() }
        )

        for (i in tileProps.indices) {
            tileProps[i] = TileProp()
        }

        try {
            // todo verify CSV using pre-calculated SHA256 hash
            val records = CSVFetcher.readCSV(CSV_PATH)

            println("[TilePropCodex] Building tile properties table")

            records.forEach { record -> setProp(
                    tileProps[idDamageToIndex(intVal(record, "id"), intVal(record, "dmg"))]
                    , record)
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {

        private lateinit var tileProps: Array<TileProp>

        val CSV_PATH = "./src/net/torvald/terrarum/tileproperties/tileprop.csv"

        const val TILE_UNIQUE_MAX = MapLayer.RANGE * PairedMapLayer.RANGE

        fun getProp(index: Int, damage: Int): TileProp {
            try {
                tileProps[idDamageToIndex(index, damage)].id
            }
            catch (e: NullPointerException) {
                throw NullPointerException("Tile prop with id $index and damage $damage does not exist.")
            }

            return tileProps[idDamageToIndex(index, damage)]
        }

        fun getProp(rawIndex: Int?): TileProp {
            try {
                tileProps[rawIndex ?: TileNameCode.NULL].id
            }
            catch (e: NullPointerException) {
                throw NullPointerException("Tile prop with raw id $rawIndex does not exist.")
            }

            return tileProps[rawIndex ?: TileNameCode.NULL]
        }

        private fun setProp(prop: TileProp, record: CSVRecord) {
            prop.nameKey = record.get("name")

            prop.id = idDamageToIndex(intVal(record, "id"), intVal(record, "dmg"))

            prop.opacity = intVal(record, "opacity")
            prop.strength = intVal(record, "strength")
            prop.density = intVal(record, "dsty")
            prop.luminosity = intVal(record, "lumcolor")
            prop.drop = intVal(record, "drop")
            prop.dropDamage = intVal(record, "ddmg")
            prop.friction = intVal(record, "friction")

            prop.isFluid = boolVal(record, "fluid")
            prop.isSolid = boolVal(record, "solid")
            prop.isWallable = boolVal(record, "wall")
            prop.isFallable = boolVal(record, "fall")

            print(formatNum3(intVal(record, "id")) + ":" + formatNum2(intVal(record, "dmg")))
            println("\t" + prop.nameKey)
        }

        private fun intVal(rec: CSVRecord, s: String): Int {
            var ret = -1
            try {
                ret = Integer.decode(rec.get(s))!!
            }
            catch (e: NullPointerException) {
            }

            return ret
        }

        private fun boolVal(rec: CSVRecord, s: String) = intVal(rec, s) != 0

        fun idDamageToIndex(index: Int, damage: Int) = index * PairedMapLayer.RANGE + damage

        private fun formatNum3(i: Int): String {
            if (i < 10)
                return "00" + i
            else if (i < 100)
                return "0" + i
            else
                return i.toString()
        }

        private fun formatNum2(i: Int) = if (i < 10) "0" + i else i.toString()
    }
}
