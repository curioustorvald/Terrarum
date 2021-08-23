package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-23.
 */
class WriteWorld {

    open fun invoke(): String {

        
        return ""
    }

    /**
     * @param b a BlockLayer
     * @return Bytes in [b] which are GZip'd then Ascii85-encoded
     */
    private fun blockLayerToStr(b: BlockLayer): String {
        val sb = StringBuilder()
        val bo = ByteArray64GrowableOutputStream()
        val zo = GZIPOutputStream(bo)

        b.bytesIterator().forEachRemaining {
            zo.write(it.toInt())
        }
        zo.flush(); zo.close()

        val ba = bo.toByteArray64()
        var bai = 0
        val buf = IntArray(4) { Ascii85.PAD_BYTE }
        ba.forEach {
            if (bai > 0 && bai % 4 == 0) {
                sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
                buf.fill(Ascii85.PAD_BYTE)
            }

            buf[bai % 4] = it.toInt() and 255

            bai += 1
        }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

        return sb.toString()
    }

}