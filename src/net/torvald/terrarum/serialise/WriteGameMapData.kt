package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.TerrarumGDX
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by minjaesong on 16-03-18.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object WriteGameMapData {

    val META_FILENAME = "worldinfo1"

    val MAGIC = "TEMD".toByteArray(charset = Charset.forName("US-ASCII"))

    val BYTE_NULL: Byte = 0


    internal fun write(saveDirectoryName: String): Boolean {
        val path = Paths.get("${TerrarumGDX.defaultSaveDir}" +
                                       "/$saveDirectoryName/${WriteMeta.META_FILENAME}")
        val tempPath = Files.createTempFile(path.toString(), "_temp")
        val map = TerrarumGDX.ingame!!.world

        // TODO gzip

        // write binary
        Files.write(tempPath, MAGIC)
        Files.write(tempPath, byteArrayOf(GameWorld.SIZEOF))
        Files.write(tempPath, byteArrayOf(GameWorld.LAYERS))
        Files.write(tempPath, byteArrayOf(BYTE_NULL))
        Files.write(tempPath, byteArrayOf(BYTE_NULL))
        Files.write(tempPath, toByteArray(map.width))
        Files.write(tempPath, toByteArray(map.height))
        Files.write(tempPath, toByteArray(map.spawnX))
        Files.write(tempPath, toByteArray(map.spawnY))
        map.layerTerrain.forEach(
                { b -> Files.write(tempPath, byteArrayOf(b)) })
        map.layerWall.forEach(
                { b -> Files.write(tempPath, byteArrayOf(b)) })
        map.layerTerrainLowBits.forEach(
                { b -> Files.write(tempPath, byteArrayOf(b)) })
        map.layerWallLowBits.forEach(
                { b -> Files.write(tempPath, byteArrayOf(b)) })
        map.layerWire.forEach(
                { b -> Files.write(tempPath, byteArrayOf(b)) })

        // replace savemeta with tempfile
        try {
            Files.copy(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(tempPath)
            println("Saved map data '$META_FILENAME' to $saveDirectoryName.")

            return true
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun toByteArray(int: Int): ByteArray {
        return byteArrayOf(
                ((int ushr 0x18) and 0xFF).toByte(),
                ((int ushr 0x10) and 0xFF).toByte(),
                ((int ushr 0x08) and 0xFF).toByte(),
                ((int          ) and 0xFF).toByte()
        )
    }
}