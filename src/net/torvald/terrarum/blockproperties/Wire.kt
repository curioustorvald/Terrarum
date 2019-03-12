package net.torvald.terrarum.blockproperties

/**
 * Created by minjaesong on 2019-03-12.
 */
object Wire {

    /* A mapping for World's conduitTypes bits */
    const val BIT_NONE = 0
    const val BIT_SIGNAL_RED = 1
    const val BIT_UTILITY_PROTOTYPE = 2
    const val BIT_POWER_LOW = 4
    const val BIT_POWER_HIGHT = 8
    const val BIT_ETHERNET = 16

    /* A mapping for World's conduitFills[] index */
    const val FILL_ID_SIGNAL_RED = 0
    const val FILL_ID_UTILITY_PROTOTYPE = 1

    fun bitToConduitFillID(bit: Int) = when(bit) {
        BIT_SIGNAL_RED -> FILL_ID_SIGNAL_RED
        BIT_UTILITY_PROTOTYPE -> FILL_ID_UTILITY_PROTOTYPE
        else -> null
    }

}