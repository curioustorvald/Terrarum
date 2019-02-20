package net.torvald.terrarum.serialise

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.serialise.WriteLayerDataZip.FILE_FOOTER
import net.torvald.terrarum.serialise.WriteLayerDataZip.PAYLOAD_FOOTER
import net.torvald.terrarum.toHex
import java.nio.charset.Charset
import java.util.*

/**
 * Created by minjaesong on 2019-02-20.
 */

object PayloadUtil {
    /**
     * InputStream must be located manually at the payload begin
     *
     * For the actual use case, take a look at the source of the [ReadLayerDataZip].
     */
    fun readAll(inputStream: MarkableFileInputStream, footer: ByteArray = FILE_FOOTER): HashMap<String, TEMzPayload> {
        val pldBuffer4 = ByteArray(4)
        val pldBuffer6 = ByteArray(6)
        val pldBuffer8 = ByteArray(8)

        val payloads = HashMap<String, TEMzPayload>()

        var pldCnt = 1

        while (true) {
            // read header and get payload's name
            inputStream.read(pldBuffer8)

            // check if end of payload reached
            if (pldBuffer8.contentEquals(footer)) {
                break
            }

            val payloadName = pldBuffer8.copyOfRange(4, 8).toString(Charset.forName("US-ASCII"))

            AppLoader.printdbg(this, "Payload $pldCnt name: $payloadName") // maybe maybe related with buffer things?

            // get uncompressed size
            inputStream.read(pldBuffer6)
            val uncompressedSize = pldBuffer6.toLittleInt48()

            // get deflated size
            inputStream.mark(2147483647) // FIXME deflated stream cannot be larger than 2 GB
            // creep forward until we hit the PAYLOAD_FOOTER
            var compressedSize: Int = 0 // FIXME deflated stream cannot be larger than 2 GB
            // loop init
            inputStream.read(pldBuffer8)
            // loop main
            while (!pldBuffer8.contentEquals(PAYLOAD_FOOTER)) {
                val aByte = inputStream.read(); compressedSize += 1
                if (aByte == -1) throw InternalError("Unexpected end-of-file at payload $pldCnt")
                pldBuffer8.shiftLeftBy(1, aByte.toByte())
            }

            // at this point, we should have correct size of deflated bytestream

            AppLoader.printdbg(this, "Payload $pldCnt compressed size: $compressedSize")

            val compressedBytes = ByteArray(compressedSize) // FIXME deflated stream cannot be larger than 2 GB
            inputStream.reset() // go back to marked spot
            inputStream.read(compressedBytes)

            // PRO Debug tip: every deflated bytes must begin with 0x789C or 0x78DA
            // Thus, \0pLd + [10] must be either of these.

            // put constructed payload into a container
            payloads.put(payloadName, TEMzPayload(uncompressedSize, compressedBytes))

            // skip over to be aligned with the next payload
            inputStream.skip(8)

            pldCnt += 1
        }

        return payloads
    }

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

    private fun ByteArray.toByteString(): String {
        val sb = StringBuilder()
        this.forEach {
            sb.append(it.toUint().toHex().takeLast(2))
            sb.append(' ')
        }
        sb.deleteCharAt(sb.lastIndex)
        return sb.toString()
    }

    data class TEMzPayload(val uncompressedSize: Long, val bytes: ByteArray) // FIXME deflated stream cannot be larger than 2 GB

}