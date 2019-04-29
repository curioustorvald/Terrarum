package net.torvald.terrarum.blockproperties

/**
 * Created by minjaesong on 2019-03-12.
 */
object Wire {

    /* A mapping for World's conduitTypes bits */
    const val BIT_NONE = 0
    const val BIT_SIGNAL_RED = 1
    const val BIT_UTILITY_PROTOTYPE = 2 // logic gates/PCLs/Diodes/Caps/etc.
    const val BIT_POWER_LOW = 4
    const val BIT_POWER_HIGHT = 8
    const val BIT_PARALLEL_8B = 16 // uses bit-to-mantissa encoding
    const val BIT_PARALLEL_16B = 32 // uses bit-to-mantissa encoding. 16 bit half duplex OR 8 bit full duplex
    const val BIT_ETHERNET = 64 // the actual datagramme should be represented by another means than the ConduitFills

    /* A mapping for World's WiringNode.fills[] index */
    const val FILL_ID_SIGNAL_RED = 0
    const val FILL_ID_UTILITY_PROTOTYPE = 1

    fun bitToConduitFillID(bit: Int) = when(bit) {
        BIT_SIGNAL_RED -> FILL_ID_SIGNAL_RED
        BIT_UTILITY_PROTOTYPE -> FILL_ID_UTILITY_PROTOTYPE
        else -> null
    }


    /**
     * Encodes a byte to Float's mantissa. Normal value range is 1.0..1.99609375. When decoding, the sign and exponent bits
     * must be ignored. (e.g. the encoded float might have not-one-point-something value after "bitwise" add/subtraction.
     *
     * ```
     *   exponent ,------- mantissa ------,
     * s eeeeeeee bbbbbbbb cccccccc xxxxxxx
     * s: sign (ignored)
     * e: binary32 exponent (non-zero and non-255)
     * b: upper byte
     * c: lower byte (zero for Byte representation)
     * x: not used, all zero
     * ```
     *
     * MSB of the byte is also the highest bit in the mantissa. Therefore ```0x80``` will be encoded as ```1.5```
     */
    fun Byte.toFloatMantissa(): Float = Float.fromBits(0x3F800000 or (this.toInt().and(0xFF) shl 15))
    fun Short.toFloatMantissa(): Float = Float.fromBits(0x3F800000 or (this.toInt().and(0xFFFF) shl 7))

    /**
     * This function does the reversal calculation.
     *
     * @see net.torvald.terrarum.blockproperties.Wire.toFloatMantissa
     */
    fun Float.fromMantissaToByte(): Byte = this.toRawBits().ushr(15).and(0xFF).toByte()
    fun Float.fromMantissaToShort(): Short = this.toRawBits().ushr(7).and(0xFFFF).toShort()

}