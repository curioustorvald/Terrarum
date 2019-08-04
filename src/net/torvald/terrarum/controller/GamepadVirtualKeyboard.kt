package net.torvald.terrarum.controller

import net.torvald.CURRENCY
import net.torvald.EMDASH
import net.torvald.MIDDOT

/**
 * Created by minjaesong on 2019-04-10.
 */
object GamepadVirtualKeyboard : VirtualKeyboard(20) {

    val keyLayoutAlph = arrayOf(
            "abcdef", "ghijkl", "mnopqr", "stuvwx", "yzçñßı", "æøþð˚,", "´`ˆ¨ˇ~˛", ".-,/!?", // normal
            "ABCDEF", "GHIJKL", "MNOPQR", "STUVWX", "YZÇÑẞİ", "ÆØÞÐ˚,", "´`ˆ¨ˇ~˛", ".-,/!?"  // shift
    )
    // note: aeiou-cedila must produce ogonek instead.
    //       aeiouszc-caron must be produced as-is. Otherwise breve is produced instead.

    val keyLayoutSym = arrayOf(
            "12345,", "67890.", "-/:;()", "&@?!'\"", "$EMDASH#%^*+", "=_\\|<>", "$MIDDOT$CURRENCY[]{}", "«»•"
    )

    override fun takeFromInputBuffer() {
        TODO("not implemented")
    }
}


/*class GamepadVirtualKeyboardUI : UICanvas {

}*/