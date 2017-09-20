package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.GameWorld
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Created by minjaesong on 2016-08-24.
 */
// internal for everything: prevent malicious module from messing up the savedata
internal object ReadLayerData {


    internal operator fun invoke(inputStream: InputStream, inWorld: GameWorld? = null): GameWorld {
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

        val retWorld = inWorld ?: GameWorld(worldWidth, worldHeight)

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