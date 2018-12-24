package net.torvald.terrarum.serialise

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.realestate.LandUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

/**
 * This object only writes a file named 'worldinfo1'.
 *
 * The intended operation is as follows:
 *  1. This and others write
 *
 *  TODO temporarily dump on the disk THEN pack? Or put all the files (in ByteArray64) in the RAM THEN pack?
 *
 * Created by minjaesong on 2016-03-18.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object WriteLayerDataZip {

    // FIXME TERRAIN DAMAGE UNTESTED


    // 2400x800  world size, default comp level: about  90 kB
    // 8192x2048 world size, default comp level: about 670 kB

    // 2400x800  world size, best comp level: about  75 kB
    // 8192x2048 world size, best comp level: about 555 kB

    val LAYERS_FILENAME = "world"

    val MAGIC = byteArrayOf(0x54, 0x45, 0x4D, 0x7A)
    val VERSION_NUMBER = 3.toByte()
    val NUMBER_OF_LAYERS = 3.toByte()
    val NUMBER_OF_PAYLOADS = 5.toByte()
    val COMPRESSION_ALGORITHM = 1.toByte()
    val PAYLOAD_HEADER = byteArrayOf(0, 0x70, 0x4C, 0x64)
    val PAYLOAD_FOOTER = byteArrayOf(0x45, 0x6E, 0x64, 0x50, 0x59, 0x4C, 0x64, -1)
    val FILE_FOOTER = byteArrayOf(0x45, 0x6E, 0x64, 0x54, 0x45, 0x4D, -1, -2)

    //val NULL: Byte = 0


    /**
     * TODO currently it'll dump the temporary file (tmp_worldinfo1) onto the disk and will return the temp file.
     *
     * @return File on success; `null` on failure
     */
    internal operator fun invoke(): File? {
        val world = (Terrarum.ingame!!.world)

        val path = "${AppLoader.defaultSaveDir}/tmp_$LAYERS_FILENAME${world.worldIndex}"

        // TODO let's try dump-on-the-disk-then-pack method...

        /*val parentDir = File("${AppLoader.defaultSaveDir}/$saveDirectoryName")
        if (!parentDir.exists()) {
            parentDir.mkdir()
        }
        else if (!parentDir.isDirectory) {
            EchoError("Savegame directory is not actually a directory, aborting...")
            return false
        }*/


        val outFile = File(path)
        if (outFile.exists()) outFile.delete()
        outFile.createNewFile()

        val outputStream = BufferedOutputStream(FileOutputStream(outFile), 8192)
        var deflater: DeflaterOutputStream // couldn't really use one outputstream for all the files.

        fun wb(byteArray: ByteArray) { outputStream.write(byteArray) }
        fun wb(byte: Byte) { outputStream.write(byte.toInt()) }
        //fun wb(byte: Int) { outputStream.write(byte) }
        fun wi32(int: Int) { wb(int.toLittle()) }
        fun wi48(long: Long) { wb(long.toLittle48()) }
        fun wi64(long: Long) { wb(long.toLittle()) }
        fun wf32(float: Float) { wi32(float.toRawBits()) }


        ////////////////////
        // WRITE BINARIES //
        ////////////////////


        // all the necessary headers
        wb(MAGIC); wb(VERSION_NUMBER); wb(NUMBER_OF_LAYERS); wb(NUMBER_OF_PAYLOADS); wb(COMPRESSION_ALGORITHM)

        // world width, height, and spawn point
        wi32(world.width); wi32(world.height)
        wi48(LandUtil.getBlockAddr(world, world.spawnX, world.spawnY))

        // write payloads //
        outputStream.flush()

        // TERR payload
        // PRO Debug tip: every deflated bytes must begin with 0x789C or 0x78DA
        // Thus, \0pLd + [10] must be either of these.

        wb(PAYLOAD_HEADER); wb("TERR".toByteArray())
        wi48(world.width * world.height * 3L / 2)
        deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)
        deflater.write(world.terrainArray)
        deflater.write(world.layerTerrainLowBits.data)
        deflater.finish()
        wb(PAYLOAD_FOOTER)

        // WALL payload
        wb(PAYLOAD_HEADER); wb("WALL".toByteArray())
        wi48(world.width * world.height * 3L / 2)
        deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)
        deflater.write(world.wallArray)
        deflater.write(world.layerWallLowBits.data)
        deflater.finish()
        wb(PAYLOAD_FOOTER)

        // WIRE payload
        wb(PAYLOAD_HEADER); wb("WIRE".toByteArray())
        wi48(world.width * world.height.toLong())
        deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)
        deflater.write(world.wireArray)
        deflater.finish()
        wb(PAYLOAD_FOOTER)

        // TdMG payload
        wb(PAYLOAD_HEADER); wb("TdMG".toByteArray())
        wi48(world.terrainDamages.size.toLong())

        deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)

        world.terrainDamages.forEach { t, u ->
            deflater.write(t.toLittle48())
            deflater.write(u.toRawBits().toLittle())
        }

        deflater.finish()
        wb(PAYLOAD_FOOTER)

        // WdMG payload
        wb(PAYLOAD_HEADER); wb("WdMG".toByteArray())
        wi48(world.wallDamages.size.toLong())

        deflater = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION), true)

        world.wallDamages.forEach { t, u ->
            deflater.write(t.toLittle48())
            deflater.write(u.toRawBits().toLittle())
        }

        deflater.finish()
        wb(PAYLOAD_FOOTER)

        // write footer
        wb(FILE_FOOTER)


        //////////////////
        // END OF WRITE //
        //////////////////



        // replace savemeta with tempfile
        try {
            outputStream.flush()
            outputStream.close()


            return outFile
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            outputStream.close()
        }

        return null
    }


}
