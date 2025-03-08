package net.torvald.terrarum.modulecomputers.gameactors

import net.torvald.terrarum.serialise.*


open class RingBusFirmware(val devtype: Int) {

    fun workWithDataFrame(hostMAC: Int, datagramme: ByteArray): Pair<Int, ByteArray?> {
        if (datagramme.size < 12) return -1 to null

        val protocol = datagramme[0].toUint()
        val mode = datagramme[1].toUint()
        val arg1 = datagramme.toBigInt16(2)
        val recipient = datagramme.toBigInt32(4)
        val sender = datagramme.toBigInt32(8)
        val body = datagramme.sliceArray(12 until datagramme.size)

        return invoke(hostMAC, protocol, mode, arg1, recipient, sender, body, datagramme)
    }

    open fun invoke(hostMAC: Int, protocol: Int, mode: Int, arg1: Int, recipient: Int, sender: Int, body: ByteArray, datagramme: ByteArray): Pair<Int, ByteArray?> {
        return when (protocol) {
            1 -> 0 to echo(recipient, sender, body)
            2 -> 0 to blockTransfer(arg1, recipient, sender, body)
            4 -> 0 to deviceDiscovery(hostMAC, datagramme)
            else -> -1 to null
        }
    }

    open fun echo(recipient: Int, sender: Int, body: ByteArray): ByteArray {
        return ByteArray(8 + body.size).makeEmptyPacket(1, 1, 0, recipient, sender).also {
            it.writeBigInt48(System.currentTimeMillis(), 12)
            System.arraycopy(body, 6, this, 6, body.size - 6)
        }
    }

    open fun blockTransfer(sequence: Int, recipient: Int, sender: Int, body: ByteArray): ByteArray {
        return ByteArray(12).makeEmptyPacket(2, 1, sequence, recipient, sender) // always ACK
    }

    open fun deviceDiscovery(host: Int, wholeMessage: ByteArray): ByteArray {
        return ByteArray(wholeMessage.size + 6).also {
            System.arraycopy(wholeMessage, 0, it, 0, wholeMessage.size)
            it[it.size - 5] = devtype.toByte()
            it.writeBigInt32(host, it.size - 4)
        }
    }



    private fun ByteArray.makeEmptyPacket(protocol: Int, mode: Int, arg1: Int, recipient: Int, sender: Int): ByteArray {
        this[0] = protocol.toByte()
        this[1] = mode.toByte()
        this.writeBigInt16(arg1, 2)
        this.writeBigInt32(recipient, 4)
        this.writeBigInt32(sender, 8)

        return this
    }
}
