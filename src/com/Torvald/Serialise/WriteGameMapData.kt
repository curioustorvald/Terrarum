package com.torvald.serialise

import com.torvald.terrarum.gamemap.GameMap
import com.torvald.terrarum.Terrarum
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by minjaesong on 16-03-18.
 */
object WriteGameMapData {

    val META_FILENAME = "worldinfo1"

    val MAGIC: ByteArray = byteArrayOf(
            'T'.toByte(),
            'E'.toByte(),
            'M'.toByte(),
            'D'.toByte()
    )

    val BYTE_NULL: Byte = 0


    fun write(saveDirectoryName: String): Boolean {
        val path = Paths.get("${Terrarum.defaultSaveDir}" +
                                       "/$saveDirectoryName/${WriteMeta.META_FILENAME}")
        val tempPath = Files.createTempFile(path.toString(), "_temp")
        val map = Terrarum.game.map

        // TODO gzip

        // write binary
        Files.write(tempPath, MAGIC)
        Files.write(tempPath, byteArrayOf(GameMap.BITS))
        Files.write(tempPath, byteArrayOf(GameMap.LAYERS))
        Files.write(tempPath, byteArrayOf(BYTE_NULL))
        Files.write(tempPath, byteArrayOf(BYTE_NULL))
        Files.write(tempPath, toByteArray(map.width))
        Files.write(tempPath, toByteArray(map.height))
        Files.write(tempPath, toByteArray(map.spawnX))
        Files.write(tempPath, toByteArray(map.spawnY))
        map.layerTerrain.forEach(
                {b -> Files.write(tempPath, byteArrayOf(b))})
        map.layerWall.forEach(
                {b -> Files.write(tempPath, byteArrayOf(b))})
        map.terrainDamage.forEach(
                {b -> Files.write(tempPath, byteArrayOf(b))})
        map.wallDamage.forEach(
                {b -> Files.write(tempPath, byteArrayOf(b))})
        map.layerWire.forEach(
                {b -> Files.write(tempPath, byteArrayOf(b))})

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