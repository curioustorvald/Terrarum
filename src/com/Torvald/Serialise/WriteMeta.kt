package com.Torvald.Serialise

import com.Torvald.Terrarum.MapGenerator.MapGenerator
import com.Torvald.Terrarum.MapGenerator.RoguelikeRandomiser
import com.Torvald.Terrarum.TileProperties.TilePropCodex
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileInputStream
import java.io.File
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object WriteMeta {

    val MAGIC: Array<Byte> = arrayOf(
              'T'.toByte()
            , 'E'.toByte()
            , 'S'.toByte()
            , 'V'.toByte()
    )

    val terraseed: Long = MapGenerator.getGeneratorSeed()
    val rogueseed: Long = RoguelikeRandomiser.getGeneratorSeed()

    fun write() {
        var hashArray: ArrayList<ByteArray> = ArrayList()

        val fileArray: Array<File> = arrayOf(
                File(TilePropCodex.CSV_PATH)
                //, File(ItemPropCodex.CSV_PATH)
                //, File(MaterialPropCodex.CSV_PATH)
                //,
        )

        for (file in fileArray) {
            val inputStream = FileInputStream(file)
            val hash = DigestUtils.sha256(inputStream)

            hashArray.add(hash)
        }
    }
}