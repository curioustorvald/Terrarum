package net.torvald.terrarum.serialise

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameworld.GameWorld
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

/**
 * TODO this one does not use TerranVirtualDisk
 *
 * Created by minjaesong on 2016-03-18.
 */
// internal for everything: prevent malicious module from messing up the savedata
@Deprecated("TEMD is deprecated format; use TEMz which does compression")
internal object WriteLayerData {

    val LAYERS_FILENAME = "worldinfo1"

    val MAGIC = "TEMD".toByteArray(charset = Charset.forName("US-ASCII"))

    val BYTE_NULL: Byte = 0


    internal operator fun invoke(saveDirectoryName: String): Boolean {
        val path = "${AppLoader.defaultSaveDir}/$saveDirectoryName/${LAYERS_FILENAME}"
        val tempPath = "${path}_bak"
        val map = (Terrarum.ingame!!.world)

        val parentDir = File("${AppLoader.defaultSaveDir}/$saveDirectoryName")
        if (!parentDir.exists()) {
            parentDir.mkdir()
        }
        else if (!parentDir.isDirectory) {
            EchoError("Savegame directory is not actually a directory, aborting...")
            return false
        }

        val tempFile = File(tempPath)
        val outFile = File(path)
        tempFile.createNewFile()

        val outputStream = GZIPOutputStream(FileOutputStream(tempFile))


        // write binary
        outputStream.write(MAGIC)
        outputStream.write(byteArrayOf(GameWorld.SIZEOF))
        outputStream.write(byteArrayOf(GameWorld.LAYERS))
        outputStream.write(byteArrayOf(BYTE_NULL))
        outputStream.write(byteArrayOf(BYTE_NULL))
        outputStream.write(map.width.toLittle())
        outputStream.write(map.height.toLittle())
        outputStream.write(map.spawnX.toLittle())
        outputStream.write(map.spawnY.toLittle())
        // write one row (byteArray) at a time
        outputStream.write(map.layerTerrain.data)       
        outputStream.write(map.layerWall.data)          
        outputStream.write(map.layerTerrainLowBits.data)
        outputStream.write(map.layerWallLowBits.data)   
        outputStream.write(map.layerWire.data)          

        // replace savemeta with tempfile
        try {
            outputStream.flush()
            outputStream.close()

            outFile.delete()
            tempFile.copyTo(outFile, overwrite = true)
            tempFile.delete()
            println("Saved map data '$LAYERS_FILENAME' to $saveDirectoryName.")

            return true
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            outputStream.close()
        }

        return false
    }


}

const val WORLD_WRITER_FORMAT_VERSION = 3