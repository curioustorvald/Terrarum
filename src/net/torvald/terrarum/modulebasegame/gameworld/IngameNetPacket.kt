package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.terrarum.serialise.*
import java.util.zip.CRC32

/**
 * # Packet data structure
 *
 *  Endianness: big
 *
 * ## The Header
 *
 * - (Byte1) Frame Type
 *      - 00 : invalid
 *      - FF : token (an "empty" packet for a Token Ring)
 *      - AA : data
 *      - EE : abort
 *      - 99 : ballot (an initialiser packet for electing the Active Monitor for a Token Ring)
 * - (Byte1) Frame number. Always 0 for a Token Ring
 * - (Byte4) Sender MAC address
 *
 * ## The Body
 *
 *  The following specification differs by the Frame Type
 *
 * ### Token and Abort
 *
 *  The Token and Abort frame has no further bytes
 *
 * ### Ballot
 *
 * - (Byte4) Currently elected Monitor candidate. A NIC examines this number, and if its MAC is larger than
 * this value, the NIC writes its own MAC to this area and passes the packet to the next NIC; otherwise it
 * just passes the packet as-is
 *
 * ### Data
 *
 * - (Byte4) Receiver MAC address
 * - (Byte4) Length of data in bytes
 * - (Bytes) The actual data
 * - (Byte4) CRC-32 of the actual data
 *
 * ## Acknowledgement
 *
 * - (Byte4) Receiver MAC address
 * - (Byte2) Optional status, should be set to 0 if not used; value of 65535 (all bits set) is reserved for negative acknowledgement
 *
 * Created by minjaesong on 2025-02-27.
 */
data class IngameNetPacket(val byteArray: ByteArray) {

    fun getFrameType(): String {
        return when (byteArray.first().toUint()) {
            0xff -> "token"
            0xaa -> "data"
            0x55 -> "ack"//nowledgement
            0xee -> "abort"
            0x99 -> "ballot"
            0x00 -> "invalid"
            else -> "unknown"
        }
    }

    private fun checkIsToken() { if (getFrameType() != "token") throw Error() }
    private fun checkIsData() { if (getFrameType() != "data") throw Error() }
    private fun checkIsAck() { if (getFrameType() != "ack") throw Error() }
    private fun checkIsDataOrAck() { if (getFrameType() != "data" && getFrameType() != "ack") throw Error() }
    private fun checkIsAbort() { if (getFrameType() != "abort") throw Error() }
    private fun checkIsBallot() { if (getFrameType() != "ballot") throw Error() }

    fun getBallot(): Int {
        checkIsBallot()
        return byteArray.toBigInt32(6)
    }

    fun setBallot(mac: Int) {
        checkIsBallot()
        byteArray.writeBigInt32(mac, 6)
    }

    fun shouldIintercept(mac: Int) = when (getFrameType()) {
        "ballot" -> (getBallot() < mac)
        "data", "ack" -> (getDataRecipient() == mac)
        else -> false
    }

    /**
     * returns null if CRC check fails
     */
    fun getDataContents(): ByteArray? {
        checkIsData()
        val len = byteArray.toBigInt32(10)
        val ret = ByteArray(len)
        System.arraycopy(byteArray, 14, ret, 0, len)
        val crc0 = byteArray.toBigInt32(14 + len)
        val crc = CRC32().also { it.update(ret) }.value.toInt()
        return if (crc != crc0) null else ret
    }

    fun getAckStatus(): Int {
        checkIsAck()
        return byteArray.toBigInt16(10)
    }

    fun getDataRecipient(): Int {
        checkIsDataOrAck()
        return byteArray.toBigInt32(6)
    }

    companion object {
        private fun ByteArray.makeHeader(frameType: Int, mac: Int): ByteArray {
            this[0] = frameType.toByte()
            this.writeBigInt32(mac, 2)
            return this
        }

        fun makeToken(mac: Int) = ByteArray(5).makeHeader(0xff, mac)

        fun makeAbort(mac: Int) = ByteArray(5).makeHeader(0xee, mac)

        fun makeBallot(mac: Int) = ByteArray(9).makeHeader(0x99, mac)

        fun makeData(sender: Int, recipient: Int, data: ByteArray) = ByteArray(18 + data.size).also {
            it.makeHeader(0xaa, sender)
            it.writeBigInt32(recipient, 6)
            it.writeBigInt32(data.size, 10)
            System.arraycopy(data, 0, it, 14, data.size)
            val crc = CRC32().also { it.update(data) }.value.toInt()
            it.writeBigInt32(crc, 14 + data.size)
        }

        fun makeAck(sender: Int, recipient: Int, status: Int = 0) = ByteArray(12).also {
            it.makeHeader(0x55, sender)
            it.writeBigInt32(recipient, 6)
            it.writeBigInt16(status, 10)
        }
    }

}