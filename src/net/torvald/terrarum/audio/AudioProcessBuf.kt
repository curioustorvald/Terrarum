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

    private fun shift(): ByteArray {
        buf0 = buf1
        buf1 = ByteArray(size)
        return buf1
    }

    private fun updateFloats() {
        fbuf0 = fbuf1
        fbuf1 = FloatArray(size / 2) {
            val i16 = (buf1[2*it].toUint() or buf1[2*it+1].toUint().shl(8)).toShort()
            i16 / 32767f
        }
    }

    fun fetchBytes(action: (ByteArray) -> Unit) {
        action(shift())
        updateFloats()
    }

    // reusing a buffer causes tons of blips in the sound? how??
    /*private val L0buf = FloatArray(size / 4)
    private val R0buf = FloatArray(size / 4)
    private val L1buf = FloatArray(size / 4)
    private val R1buf = FloatArray(size / 4)

    fun getL0(volume: Double): FloatArray {
        for (i in L0buf.indices) { L0buf[i] = (volume * fbuf0[2*i]).toFloat() }
        return L0buf
    }
    fun getR0(volume: Double): FloatArray {
        for (i in R0buf.indices) { R0buf[i] = (volume * fbuf0[2*i+1]).toFloat() }
        return R0buf
    }
    fun getL1(volume: Double): FloatArray {
        for (i in L1buf.indices) { L1buf[i] = (volume * fbuf1[2*i]).toFloat() }
        return L1buf
    }
    fun getR1(volume: Double): FloatArray {
        for (i in R1buf.indices) { R1buf[i] = (volume * fbuf1[2*i+1]).toFloat() }
        return R1buf
    }*/
    fun getL0(volume: Double) = FloatArray(size / 4) { (volume * fbuf0[2*it]).toFloat() }
    fun getR0(volume: Double) = FloatArray(size / 4) { (volume * fbuf0[2*it+1]).toFloat() }
    fun getL1(volume: Double) = FloatArray(size / 4) { (volume * fbuf1[2*it]).toFloat() }
    fun getR1(volume: Double) = FloatArray(size / 4) { (volume * fbuf1[2*it+1]).toFloat() }

}