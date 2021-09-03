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

fun ByteArray.toLittleInt() =
        if (this.size != 4) throw Error("Array not in size of 4")
        else    this[0].toUint() or
                this[1].toUint().shl(8) or
                this[2].toUint().shl(16) or
                this[3].toUint().shl(24)
fun ByteArray.toULittleShort() =
        if (this.size != 4) throw Error("Array not in size of 2")
        else    this[0].toUint() or
                this[1].toUint().shl(8)
fun ByteArray.toLittleShort() =
        if (this.size != 4) throw Error("Array not in size of 2")
        else    this[0].toUint() or
                this[1].toInt().shl(8)
fun ByteArray.toLittleLong() =
        if (this.size != 8) throw Error("Array not in size of 8")
        else    this[0].toUlong() or
                this[1].toUlong().shl(8) or
                this[2].toUlong().shl(16) or
                this[3].toUlong().shl(24) or
                this[4].toUlong().shl(32) or
                this[5].toUlong().shl(40) or
                this[6].toUlong().shl(48) or
                this[7].toUlong().shl(56)
fun ByteArray.toLittleInt48() =
        if (this.size != 6) throw Error("Array not in size of 6")
        else    this[0].toUlong() or
                this[1].toUlong().shl(8) or
                this[2].toUlong().shl(16) or
                this[3].toUlong().shl(24) or
                this[4].toUlong().shl(32) or
                this[5].toUlong().shl(40)
fun ByteArray.toLittleFloat() = java.lang.Float.intBitsToFloat(this.toLittleInt())

fun Byte.toUlong() = java.lang.Byte.toUnsignedLong(this)
fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
