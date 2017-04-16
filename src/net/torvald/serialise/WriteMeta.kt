package net.torvald.serialise

import net.torvald.terrarum.mapgenerator.WorldGenerator
import net.torvald.terrarum.mapgenerator.RoguelikeRandomiser
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.tileproperties.TilePropCSV
import net.torvald.terrarum.tileproperties.TileCodex
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.*
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object WriteMeta {

    val META_FILENAME = "world"

    val MAGIC = "TESV".toByteArray(charset = Charset.forName("US-ASCII"))


    val BYTE_NULL: Byte = 0

    val terraseed: Long = WorldGenerator.SEED
    val rogueseed: Long = RoguelikeRandomiser.seed

    /**
     * Write save meta to specified directory. Returns false if something went wrong.
     * @param saveDirectoryName
     * @param savegameName -- Nullable. If the value is not specified, saveDirectoryName will be used instead.
     */
    fun write(saveDirectoryName: String, savegameName: String?): Boolean {
        val hashArray: ArrayList<ByteArray> = ArrayList()
        val savenameAsByteArray: ByteArray =
                (savegameName ?: saveDirectoryName).toByteArray(Charsets.UTF_8)

        // define Strings to be hashed
        val props = arrayOf(
                TilePropCSV()
                //, (item, mat, ...)
        )

        // get and store hash from the list
        props.map { hashArray.add(DigestUtils.sha256(it)) }

        // open file and delete it
        val metaPath = Paths.get("$Terrarum.defaultSaveDir" +
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
                (long.ushr(0x38) and 0xFF).toByte(),
                (long.ushr(0x30) and 0xFF).toByte(),
                (long.ushr(0x28) and 0xFF).toByte(),
                (long.ushr(0x20) and 0xFF).toByte(),
                (long.ushr(0x18) and 0xFF).toByte(),
                (long.ushr(0x10) and 0xFF).toByte(),
                (long.ushr(0x08) and 0xFF).toByte(),
                (long            and 0xFF).toByte()
        )
    }
}