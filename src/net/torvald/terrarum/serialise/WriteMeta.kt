package net.torvald.terrarum.serialise

import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.modulebasegame.worldgenerator.WorldGenerator
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import java.nio.charset.Charset

/**
 * Created by minjaesong on 2016-03-15.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object WriteMeta {

    val META_FILENAME = "worldinfo0"

    val MAGIC = "TESV".toByteArray(charset = Charset.forName("US-ASCII"))


    val BYTE_NULL: Byte = 0

    val terraseed: Long = WorldGenerator.SEED


    /**
     * Write save meta to specified directory. Returns false if something went wrong.
     * @param saveDirectoryName
     * @param savegameName -- Nullable. If the value is not specified, saveDirectoryName will be used instead.
     */
    internal fun write(saveDirectoryName: String, savegameName: String?): Boolean {
        /*val hashArray: ArrayList<ByteArray> = ArrayList()
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
        val metaPath = Paths.get("$AppLoader.defaultSaveDir" +
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
        }*/
        return false
    }
}