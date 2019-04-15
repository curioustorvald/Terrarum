package net.torvald.terrarum.controller

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
            "12345,", "67890.", "-/:;()", "&@?!'\"", "—#%^*+", "=_\\|<>", "·¤[]{}", "«»•"
    )

    override fun takeFromInputBuffer() {
        TODO("not implemented")
    }
}


/*class GamepadVirtualKeyboardUI : UICanvas {

}*/