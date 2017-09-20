package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.EchoError
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2016-03-18.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object WriteLayerData {

    val META_FILENAME = "worldinfo1"

    val MAGIC = "TEMD".toByteArray(charset = Charset.forName("US-ASCII"))

    val BYTE_NULL: Byte = 0


    internal operator fun invoke(saveDirectoryName: String): Boolean {
        val path = "${Terrarum.defaultSaveDir}/$saveDirectoryName/${META_FILENAME}"
        val tempPath = "${path}_bak"
        val map = Terrarum.ingame!!.world

        val parentDir = File("${Terrarum.defaultSaveDir}/$saveDirectoryName")
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
            println("Saved map data '$META_FILENAME' to $saveDirectoryName.")

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

fun Int.toLittle() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte(),
        this.ushr(16).and(0xFF).toByte(),
        this.ushr(24).and(0xFF).toByte()
)
fun Long.toLittle() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte(),
        this.ushr(16).and(0xFF).toByte(),
        this.ushr(24).and(0xFF).toByte(),
        this.ushr(32).and(0xFF).toByte(),
        this.ushr(40).and(0xFF).toByte(),
        this.ushr(48).and(0xFF).toByte(),
        this.ushr(56).and(0xFF).toByte()
)
fun Double.toLittle() = java.lang.Double.doubleToRawLongBits(this).toLittle()
fun Boolean.toLittle() = byteArrayOf(if (this) 0xFF.toByte() else 0.toByte())

fun ByteArray.toLittleInt() =
        if (this.size != 4) throw Error("Array not in size of 4")
        else    this[0].toUint() or
                this[1].toUint().shl(8) or
                this[2].toUint().shl(16) or
                this[3].toUint().shl(24)
fun ByteArray.toLittleLong() =
        if (this.size != 8) throw Error("Array not in size of 8")
        else    this[0].toUlong() or
                this[1].toUlong().shl(8) or
                this[2].toUlong().shl(16) or
                this[3].toUlong().shl(24) or
                this[4].toUlong().shl(32) or
                this[5].toUlong().shl(40) or
                this[6].toUlong().shl(48) or
                this[7].toUlong().shl(56)
fun ByteArray.toLittleDouble() = java.lang.Double.longBitsToDouble(this.toLittleLong())

fun Byte.toUlong() = java.lang.Byte.toUnsignedLong(this)
fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)