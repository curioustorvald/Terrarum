package com.torvald.terrarum.tileproperties

import com.torvald.CSVFetcher
import com.torvald.terrarum.gamemap.MapLayer
import com.torvald.terrarum.gamemap.PairedMapLayer
import org.apache.commons.csv.CSVRecord

import java.io.IOException

/**
 * Created by minjaesong on 16-02-16.
 */
class TilePropCodex {

    init {
        tileProps = Array<TileProp>(MapLayer.RANGE * PairedMapLayer.RANGE,
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
                    tileProps[indexDamageToArrayAddr(intVal(record, "id"), intVal(record, "dmg"))]
                    , record)
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {

        private lateinit var tileProps: Array<TileProp>

        val CSV_PATH = "./src/com/torvald/terrarum/tileproperties/tileprop.csv"

        fun getProp(index: Int, damage: Int): TileProp {
            try {
                tileProps[indexDamageToArrayAddr(index, damage)].id
            }
            catch (e: NullPointerException) {
                throw NullPointerException("Tile prop with id $index and damage $damage does not exist.")
            }

            return tileProps[indexDamageToArrayAddr(index, damage)]
        }

        fun getProp(rawIndex: Int?): TileProp {
            try {
                tileProps[rawIndex ?: TileNameCode.STONE].id
            }
            catch (e: NullPointerException) {
                throw NullPointerException("Tile prop with raw id $rawIndex does not exist.")
            }

            return tileProps[rawIndex ?: TileNameCode.STONE]
        }

        private fun setProp(prop: TileProp, record: CSVRecord) {
            prop.name = record.get("name")

            prop.id = intVal(record, "id")
            prop.damage = intVal(record, "dmg")

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

            if (prop.isFluid) prop.movementResistance = intVal(record, "movr")

            print(formatNum3(prop.id) + ":" + formatNum2(prop.damage))
            println("\t" + prop.name)
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

        private fun boolVal(rec: CSVRecord, s: String): Boolean {
            return intVal(rec, s) != 0
        }

        fun indexDamageToArrayAddr(index: Int, damage: Int): Int {
            return index * PairedMapLayer.RANGE + damage
        }

        private fun formatNum3(i: Int): String {
            if (i < 10)
                return "00" + i
            else if (i < 100)
                return "0" + i
            else
                return i.toString()
        }

        private fun formatNum2(i: Int): String {
            if (i < 10)
                return "0" + i
            else
                return i.toString()
        }
    }
}
