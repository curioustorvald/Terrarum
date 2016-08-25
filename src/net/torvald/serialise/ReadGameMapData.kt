package net.torvald.serialise

/**
 * Created by minjaesong on 16-08-24.
 */
object ReadGameMapData {
	
	fun InputStream.readRelative(b: ByteArray, off: Int, len: Int): Int {
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