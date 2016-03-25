package com.torvald.serialise

import com.torvald.terrarum.mapgenerator.MapGenerator
import com.torvald.terrarum.mapgenerator.RoguelikeRandomiser
import com.torvald.terrarum.Terrarum
import com.torvald.terrarum.tileproperties.TilePropCodex
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileInputStream
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object WriteMeta {

    val META_FILENAME = "world"

    val MAGIC: ByteArray = byteArrayOf(
            'T'.toByte(),
            'E'.toByte(),
            'S'.toByte(),
            'V'.toByte()
    )

    val BYTE_NULL: Byte = 0

    val terraseed: Long = MapGenerator.getGeneratorSeed()
    val rogueseed: Long = RoguelikeRandomiser.getGeneratorSeed()

    /**
     * Write save meta to specified directory. Returns false if something went wrong.
     * @param saveDirectoryName
     * @param savegameName -- Nullable. If the value is not specified, saveDirectoryName will be used instead.
     */
    fun write(saveDirectoryName: String, savegameName: String?): Boolean {
        val hashArray: ArrayList<ByteArray> = ArrayList()
        val savenameAsByteArray: ByteArray =
                (savegameName ?: saveDirectoryName).toByteArray(Charsets.UTF_8)

        // define files to get hash
        val fileArray: Array<File> = arrayOf(
                File(TilePropCodex.CSV_PATH)
                //, File(ItemPropCodex.CSV_PATH)
                //, File(MaterialPropCodex.CSV_PATH)
                //,
        )

        // get and store hash from fileArray
        for (file in fileArray) {
            val inputStream = FileInputStream(file)
            val hash = DigestUtils.sha256(inputStream)

            hashArray.add(hash)
        }

        // open file and delete it
        val metaPath = Paths.get("${Terrarum.defaultSaveDir}" +
                                       "/$saveDirectoryName/$META_FILENAME")
        val metaTempPath = Files.createTempFile(metaPath.toString(), "_temp")

        // TODO gzip

        // write bytes in tempfile
        Files.write(metaTempPath, MAGIC)
        Files.write(metaTempPath, savenameAsByteArray)
        Files.write(metaTempPath, byteArrayOf(BYTE_NULL))
        Files.write(metaTempPath, toByteArray(terraseed))
        Files.write(metaTempPath, toByteArray(rogueseed))
        for (hash in hashArray)
            Files.write(metaTempPath, hash)

        // replace savemeta with tempfile
        try {
            Files.copy(metaTempPath, metaPath, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(metaTempPath)
            println("Saved metadata to $saveDirectoryName.")

            return true
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun toByteArray(long: Long): ByteArray {
        return byteArrayOf(
                ((long ushr 0x38) and 0xFF).toByte(),
                ((long ushr 0x30) and 0xFF).toByte(),
                ((long ushr 0x28) and 0xFF).toByte(),
                ((long ushr 0x20) and 0xFF).toByte(),
                ((long ushr 0x18) and 0xFF).toByte(),
                ((long ushr 0x10) and 0xFF).toByte(),
                ((long ushr 0x08) and 0xFF).toByte(),
                ((long          ) and 0xFF).toByte()
        )
    }
}