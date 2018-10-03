package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.BlockDamage
import net.torvald.terrarum.gameworld.MapLayer
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.realestate.LandUtil
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream
import kotlin.IllegalArgumentException
import kotlin.collections.HashMap

/**
 * Created by minjaesong on 2016-08-24.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object ReadLayerDataZip {

    // FIXME UNTESTED !!

    internal operator fun invoke(file: File): LayerData {
        val inputStream = MarkableFileInputStream(FileInputStream(file))


        val magicBytes = ByteArray(4)
        val versionNumber = ByteArray(1)
        val layerCount = ByteArray(1)
        val payloadCountByte = ByteArray(1)
        val compression = ByteArray(1)
        val worldWidth = ByteArray(4)
        val worldHeight = ByteArray(4)
        val spawnAddress = ByteArray(6)


        //////////////////
        // FILE READING //
        //////////////////


        // read header first
        inputStream.read(magicBytes)
        if (!Arrays.equals(magicBytes, WriteLayerDataZip.MAGIC)) {
            throw IllegalArgumentException("File not a Layer Data")
        }


        inputStream.read(versionNumber)
        inputStream.read(layerCount)
        inputStream.read(payloadCountByte)
        inputStream.read(compression)
        inputStream.read(worldWidth)
        inputStream.read(worldHeight)
        inputStream.read(spawnAddress)

        // read payloads

        val payloadCount = payloadCountByte[0].toUint()
        val pldBuffer4 = ByteArray(4)
        val pldBuffer6 = ByteArray(6)
        val pldBuffer8 = ByteArray(8)

        val payloads = HashMap<String, TEMzPayload>()

        for (pldCnt in 0 until payloadCount) {
            inputStream.read(pldBuffer4)

            // check payload header
            if (!pldBuffer4.contentEquals(WriteLayerDataZip.PAYLOAD_HEADER))
                throw InternalError("Payload not found")

            // get payload's name
            inputStream.read(pldBuffer4)
            val payloadName = pldBuffer4.toString(Charset.forName("US-ASCII"))

            // get uncompressed size
            inputStream.read(pldBuffer6)
            val uncompressedSize = pldBuffer6.toLittleInt48()

            // get deflated size
            inputStream.mark(2147483647) // FIXME deflated stream cannot be larger than 2 GB
            // creep forward until we hit the PAYLOAD_FOOTER
            var deflatedSize: Int = 0 // FIXME deflated stream cannot be larger than 2 GB
            // loop init
            inputStream.read(pldBuffer8)
            // loop main
            while (!pldBuffer8.contentEquals(WriteLayerDataZip.PAYLOAD_FOOTER)) {
                val aByte = inputStream.read(); deflatedSize += 1
                if (aByte == -1) throw InternalError("Unexpected end-of-file")
                pldBuffer8.shiftLeftBy(1, aByte.toByte())
            }
            // at this point, we should have correct size of deflated bytestream
            val deflatedBytes = ByteArray(deflatedSize) // FIXME deflated stream cannot be larger than 2 GB
            inputStream.reset() // go back to marked spot
            inputStream.read(deflatedBytes)
            // put constructed payload into a container
            payloads.put(payloadName, TEMzPayload(uncompressedSize, deflatedBytes))

            // skip over to be aligned with the next payload
            inputStream.skip(18L + deflatedSize)
        }


        // test for EOF
        inputStream.read(pldBuffer8)
        if (!pldBuffer8.contentEquals(WriteLayerDataZip.FILE_FOOTER))
            throw InternalError("Expected end-of-file, got not-so-end-of-file")


        //////////////////////
        // END OF FILE READ //
        //////////////////////

        val width = worldWidth.toLittleInt()
        val height = worldHeight.toLittleInt()
        val worldSize = width.toLong() * height

        val payloadBytes = HashMap<String, ByteArray>()

        payloads.forEach { t, u ->
            val inflatedFile = ByteArray(u.uncompressedSize.toInt()) // FIXME deflated stream cannot be larger than 2 GB
            val inflater = Inflater()
            inflater.setInput(u.bytes, 0, u.bytes.size)
            val uncompLen = inflater.inflate(inflatedFile)

            // just in case
            if (uncompLen.toLong() != u.uncompressedSize)
                throw InternalError("DEFLATE size mismatch -- expected ${u.uncompressedSize}, got $uncompLen")

            // deal with (MSB ++ LSB)
            if (t == "TERR" || t == "WALL") {
                payloadBytes["${t}_MSB"] = inflatedFile.sliceArray(0 until worldSize.toInt()) // FIXME deflated stream cannot be larger than 2 GB
                payloadBytes["${t}_LSb"] = inflatedFile.sliceArray(worldSize.toInt() until inflatedFile.size) // FIXME deflated stream cannot be larger than 2 GB
            }
            else {
                payloadBytes[t] = inflatedFile
            }
        }

        val spawnPoint = LandUtil.resolveBlockAddr(width, spawnAddress.toLittleInt48())

        val terrainDamages = HashMap<BlockAddress, BlockDamage>()
        val wallDamages = HashMap<BlockAddress, BlockDamage>()

        // parse terrain damages
        for (c in 0 until payloadBytes["TdMG"]!!.size step 10) {
            val bytes = payloadBytes["TdMG"]!!

            val tileAddr = bytes.sliceArray(c..c+5)
            val value = bytes.sliceArray(c+6..c+9)

            terrainDamages[tileAddr.toLittleInt48()] = value.toLittleFloat()
        }


        // parse wall damages
        for (c in 0 until payloadBytes["WdMG"]!!.size step 10) {
            val bytes = payloadBytes["WdMG"]!!

            val tileAddr = bytes.sliceArray(c..c+5)
            val value = bytes.sliceArray(c+6..c+9)

            wallDamages[tileAddr.toLittleInt48()] = value.toLittleFloat()
        }

        
        return LayerData(
                MapLayer(width, height, payloadBytes["WALL_MSB"]!!),
                MapLayer(width, height, payloadBytes["TERR_MSB"]!!),
                MapLayer(width, height, payloadBytes["WIRE"]!!),
                PairedMapLayer(width, height, payloadBytes["WALL_LSB"]!!),
                PairedMapLayer(width, height, payloadBytes["TERR_LSB"]!!),

                spawnPoint.first, spawnPoint.second,

                wallDamages, terrainDamages
        )
    }

    private data class TEMzPayload(val uncompressedSize: Long, val bytes: ByteArray) // FIXME deflated stream cannot be larger than 2 GB

    /**
     * Immediately deployable, a part of the gameworld
     */
    internal data class LayerData(
            val layerWall: MapLayer,
            val layerTerrain: MapLayer,
            val layerWire: MapLayer,
            val layerWallLowBits: PairedMapLayer,
            val layerTerrainLowBits: PairedMapLayer,
            //val layerThermal: MapLayerHalfFloat, // in Kelvins
            //val layerAirPressure: MapLayerHalfFloat, // (milibar - 1000)

            val spawnX: Int,
            val spawnY: Int,
            val wallDamages: HashMap<BlockAddress, BlockDamage>,
            val terrainDamages: HashMap<BlockAddress, BlockDamage>
    )

    private fun ByteArray.shiftLeftBy(size: Int, fill: Byte = 0.toByte()) {
        if (size == 0) {
            return
        }
        else if (size < 0) {
            throw IllegalArgumentException("This won't shift to right (size = $size)")
        }
        else if (size >= this.size) {
            Arrays.fill(this, 0.toByte())
        }
        else {
            for (c in size..this.lastIndex) {
                this[c - size] = this[c]
            }
            for (c in (this.size - size)..this.lastIndex) {
                this[c] = fill
            }
        }
    }


	internal fun InputStream.readRelative(b: ByteArray, off: Int, len: Int): Int {
        if (b == null) {
            throw NullPointerException()
        } else if (off < 0 || len < 0 || len > b.size) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        var c = read()
        if (c == -1) {
            return -1
        }
        b[0] = c.toByte()

        var i = 1
        try {
            while (i < len) {
                c = read()
                if (c == -1) {
                    break
                }
                b[i] = c.toByte()
                i++
            }
        } catch (ee: IOException) {
        }

        return i
    }
}