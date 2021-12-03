package net.torvald.terrarum.modulecomputers.tsvmperipheral

import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.tsvm.VM
import net.torvald.tsvm.peripheral.BlockTransferInterface
import net.torvald.tsvm.peripheral.TestDiskDrive
import net.torvald.tsvm.peripheral.trimNull
import java.io.ByteArrayOutputStream

/**
 * Created by minjaesong on 2021-12-02.
 */
class WorldRadar : BlockTransferInterface(false, true) {

    private val W = 162
    private val H = 142

    private val AIR_OUT = 0.toByte()
    private val GRASS_OUT = 2.toByte()
    private val DIRT_OUT = 4.toByte()
    private val STONE_OUT = 7.toByte()

    init {
        statusCode = TestDiskDrive.STATE_CODE_STANDBY
    }

    private val messageComposeBuffer = ByteArrayOutputStream(BLOCK_SIZE) // always use this and don't alter blockSendBuffer please
    private var blockSendBuffer = ByteArray(1)
    private var blockSendCount = 0

    private fun resetBuf() {
        blockSendCount = 0
        messageComposeBuffer.reset()
    }


    override fun hasNext(): Boolean {
        return (blockSendCount * BLOCK_SIZE < blockSendBuffer.size)
    }

    override fun startSendImpl(recipient: BlockTransferInterface): Int {
        if (blockSendCount == 0) {
            blockSendBuffer = messageComposeBuffer.toByteArray()
        }

        val sendSize = if (blockSendBuffer.size - (blockSendCount * BLOCK_SIZE) < BLOCK_SIZE)
            blockSendBuffer.size % BLOCK_SIZE
        else BLOCK_SIZE

        recipient.writeout(ByteArray(sendSize) {
            blockSendBuffer[blockSendCount * BLOCK_SIZE + it]
        })

        blockSendCount += 1

        return sendSize
    }

    private var oldCmdbuf = HashMap<Int,Byte>(1024)

    private fun getNearbyTilesPos(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
                Point2i(x + 1, y),
                Point2i(x, y + 1),
                Point2i(x - 1, y),
                Point2i(x, y - 1),
        )
    }
    override fun writeoutImpl(inputData: ByteArray) {
        val inputString = inputData.trimNull().toString(VM.CHARSET)

        // prepare draw commands
        /*
         * draw command format:
         *
         * <Y> <X> <COL>
         *
         * marking rules:
         *
         * : exposed = has at least 1 nonsolid on 4 sides
         *
         * 1. exposed grass -> 2
         * 2. exposed dirt -> 4
         * 3. exposed stone -> 7
         * 4. stone exposed to dirt/grass -> 7
         */
        if (inputString.startsWith("POLL")) {
            resetBuf()
            val cmdbuf = HashMap<Int,Byte>(1024)

            Terrarum.ingame?.let { ingame -> ingame.actorNowPlaying?.let {

                val px = it.intTilewiseHitbox.canonicalX.toInt()
                val py = it.intTilewiseHitbox.canonicalY.toInt()

                for (y in 1..H - 2) {
                    for (x in 1..W - 2) {
                        val yx = (y - 1).shl(8) or x
                        val nearby = getNearbyTilesPos(px, py).map { ingame.world.getTileFromTerrain(it.x, it.y) } // up, left, right, down
                        val block = ingame.world.getTileFromTerrain(px, py)
                        val blockprop = Terrarum.blockCodex[block]

                        if (blockprop.isSolid) {
                            // TODO create extension function nearby.contains { predicate :: ItemID -> Boolean }
                            if (blockprop.material == "GRSS" && nearby.contains(Block.AIR)) {
                                cmdbuf[yx] = GRASS_OUT
                            }
                            else if (blockprop.material == "DIRT" && nearby.contains(Block.AIR)) {
                                cmdbuf[yx] = DIRT_OUT
                            }
                            else if (blockprop.material == "ROCK" && (nearby.contains(Block.AIR) || nearby.contains(Block.GRASS) || nearby.contains(Block.DIRT))) {
                                cmdbuf[yx] = STONE_OUT
                            }
                        }
                    }
                }

                (oldCmdbuf.keys union cmdbuf.keys).sorted().forEach { key ->
                    val value = (cmdbuf[key] ?: AIR_OUT).toInt()
                    val x = key % 256
                    val y = key / 256
                    messageComposeBuffer.write(y)
                    messageComposeBuffer.write(x)
                    messageComposeBuffer.write(value)
                }

                oldCmdbuf = cmdbuf
            }}
        }
    }
}