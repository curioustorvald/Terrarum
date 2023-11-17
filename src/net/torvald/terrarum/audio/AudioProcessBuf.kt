package net.torvald.terrarum.audio

import net.torvald.terrarum.serialise.toUint

/**
 * Audio is assumed to be 16 bits
 *
 * Created by minjaesong on 2023-11-17.
 */
class AudioProcessBuf(val size: Int) {

    var buf0 = ByteArray(size); private set
    var buf1 = ByteArray(size); private set

    var fbuf0 = FloatArray(size / 2); private set
    var fbuf1 = FloatArray(size / 2); private set

    fun shift(): ByteArray {
        buf0 = buf1
        buf1 = ByteArray(size)

        fbuf0 = fbuf1
        fbuf1 = FloatArray(size / 2) {
            val i16 = (buf1[4*it].toUint() or buf1[4*it+1].toUint().shl(8)).toShort()
            i16 / 32767f
        }

        return buf1
    }

    fun getL0() = FloatArray(size / 4) { fbuf0[2*it] }
    fun getR0() = FloatArray(size / 4) { fbuf0[2*it+1] }
    fun getL1() = FloatArray(size / 4) { fbuf1[2*it] }
    fun getR1() = FloatArray(size / 4) { fbuf1[2*it+1] }

}