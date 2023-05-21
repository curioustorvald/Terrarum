package net.torvald.terrarum.serialise

fun Int.toLittle() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte(),
        this.ushr(16).and(0xFF).toByte(),
        this.ushr(24).and(0xFF).toByte()
)
/** Converts int as 2-byte array, discarding the sign.*/
fun Int.toULittleShort() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte()
)
/** Converts int as 2-byte array, preserving the sign. In other words, it converts int to short. */
fun Int.toLittleShort() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.shr(8).and(0xFF).toByte()
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
/** Converts long as 6-byte array, discarding the sign. */
fun Long.toULittle48() = byteArrayOf(
        this.and(0xFF).toByte(),
        this.ushr(8).and(0xFF).toByte(),
        this.ushr(16).and(0xFF).toByte(),
        this.ushr(24).and(0xFF).toByte(),
        this.ushr(32).and(0xFF).toByte(),
        this.ushr(40).and(0xFF).toByte()
)
fun Double.toLittle() = java.lang.Double.doubleToRawLongBits(this).toLittle()
fun Boolean.toLittle() = byteArrayOf(if (this) 0xFF.toByte() else 0.toByte())

fun ByteArray.toLittleInt32(offset: Int = 0) =
        this[0 + offset].toUint() or
        this[1 + offset].toUint().shl(8) or
        this[2 + offset].toUint().shl(16) or
        this[3 + offset].toUint().shl(24)
fun ByteArray.toULittleShort(offset: Int = 0) =
        this[0 + offset].toUint() or
        this[1 + offset].toUint().shl(8)
fun ByteArray.toLittleShort(offset: Int = 0) =
        this[0 + offset].toUint() or
        this[1 + offset].toInt().shl(8)
fun ByteArray.toLittleInt64(offset: Int = 0) =
        this[0 + offset].toUlong() or
        this[1 + offset].toUlong().shl(8) or
        this[2 + offset].toUlong().shl(16) or
        this[3 + offset].toUlong().shl(24) or
        this[4 + offset].toUlong().shl(32) or
        this[5 + offset].toUlong().shl(40) or
        this[6 + offset].toUlong().shl(48) or
        this[7 + offset].toUlong().shl(56)
fun ByteArray.toLittleInt48(offset: Int = 0) =
        this[0 + offset].toUlong() or
        this[1 + offset].toUlong().shl(8) or
        this[2 + offset].toUlong().shl(16) or
        this[3 + offset].toUlong().shl(24) or
        this[4 + offset].toUlong().shl(32) or
        this[5 + offset].toUlong().shl(40)
fun ByteArray.toLittleFloat() = java.lang.Float.intBitsToFloat(this.toLittleInt32())

fun ByteArray.toBigInt16(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(8) or
                this[1 + offset].toUint()
}
fun ByteArray.toBigInt24(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(16) or
                this[1 + offset].toUint().shl(8) or
                this[2 + offset].toUint()
}
fun ByteArray.toBigInt32(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(24) or
                this[1 + offset].toUint().shl(16) or
                this[2 + offset].toUint().shl(8) or
                this[3 + offset].toUint()
}
fun ByteArray.toBigInt48(offset: Int = 0): Long {
        return  this[0 + offset].toUlong().shl(40) or
                this[1 + offset].toUlong().shl(32) or
                this[2 + offset].toUlong().shl(24) or
                this[3 + offset].toUlong().shl(16) or
                this[4 + offset].toUlong().shl(8) or
                this[5 + offset].toUlong()
}
fun ByteArray.toBigInt64(offset: Int = 0): Long {
        return  this[0 + offset].toUlong().shl(56) or
                this[1 + offset].toUlong().shl(48) or
                this[2 + offset].toUlong().shl(40) or
                this[3 + offset].toUlong().shl(32) or
                this[4 + offset].toUlong().shl(24) or
                this[5 + offset].toUlong().shl(16) or
                this[6 + offset].toUlong().shl(8) or
                this[7 + offset].toUlong()
}
fun Byte.toUlong() = java.lang.Byte.toUnsignedLong(this)
fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
