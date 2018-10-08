package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Only being used by the title screen and the demoworld. This object may get deleted at any update
 *
 * Created by minjaesong on 2016-08-24.
 */
// internal for everything: prevent malicious module from messing up the savedata
@Deprecated("TEMD is deprecated format; use TEMz which does compression")
internal object ReadLayerData {


    internal operator fun invoke(inputStream: InputStream, inWorld: GameWorldExtension? = null): GameWorldExtension {
        val magicBytes = ByteArray(4)
        val layerSizeBytes = ByteArray(1)
        val layerCountBytes = ByteArray(1)
        val worldWidthBytes = ByteArray(4)
        val worldHeightBytes = ByteArray(4)
        val spawnCoordXBytes = ByteArray(4)
        val spawnCoordYBytes = ByteArray(4)

        // read header first
        inputStream.read(magicBytes)
        if (!Arrays.equals(magicBytes, WriteLayerData.MAGIC)) {
            throw IllegalArgumentException("File not a Layer Data")
        }

        inputStream.read(layerSizeBytes)
        inputStream.read(layerCountBytes)
        inputStream.skip(2) // reserved bytes
        inputStream.read(worldWidthBytes)
        inputStream.read(worldHeightBytes)
        inputStream.read(spawnCoordXBytes)
        inputStream.read(spawnCoordYBytes)

        val worldWidth = worldWidthBytes.toLittleInt()
        val worldHeight = worldHeightBytes.toLittleInt()
        val bytesPerTile = layerSizeBytes[0].toUint()
        val layerCount = layerCountBytes[0].toUint()
        val layerSize = worldWidth * worldHeight * bytesPerTile

        val terrainLayerMSB = ByteArray(layerSize)
        val wallLayerMSB = ByteArray(layerSize)
        val terrainLayerLSB = ByteArray(layerSize / 2)
        val wallLayerLSB = ByteArray(layerSize / 2)
        var wireLayer: ByteArray? = null

        inputStream.read(terrainLayerMSB)
        inputStream.read(wallLayerMSB)
        inputStream.read(terrainLayerLSB)
        inputStream.read(wallLayerLSB)

        if (layerCount == 4) {
            wireLayer = ByteArray(layerSize)
            inputStream.read(wireLayer)
        }



        // create world out of tiles data

        val retWorld = inWorld ?: GameWorldExtension(1, worldWidth, worldHeight, 0, 0, 0) // FIXME null TIME_T for the (partial) test to pass

        retWorld.layerTerrain.data = terrainLayerMSB
        retWorld.layerWall.data = wallLayerMSB
        retWorld.layerTerrainLowBits.data = terrainLayerLSB
        retWorld.layerWallLowBits.data = wallLayerLSB

        if (wireLayer != null) {
            retWorld.layerWire.data = wireLayer
        }

        retWorld.spawnX = spawnCoordXBytes.toLittleInt()
        retWorld.spawnY = spawnCoordYBytes.toLittleInt()


        return retWorld
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
fun Long.toLittle48() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte(),
        this.ushr(16).and(0xFF).toByte(),
        this.ushr(24).and(0xFF).toByte(),
        this.ushr(32).and(0xFF).toByte(),
        this.ushr(40).and(0xFF).toByte()
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
fun ByteArray.toLittleInt48() =
        if (this.size != 6) throw Error("Array not in size of 6")
        else    this[0].toUlong() or
                this[1].toUlong().shl(8) or
                this[2].toUlong().shl(16) or
                this[3].toUlong().shl(24) or
                this[4].toUlong().shl(32) or
                this[5].toUlong().shl(40)
fun ByteArray.toLittleFloat() = java.lang.Float.intBitsToFloat(this.toLittleInt())

fun Byte.toUlong() = java.lang.Byte.toUnsignedLong(this)
fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)