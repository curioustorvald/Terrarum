package net.torvald.terrarum.utils

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * Created by minjaesong on 2016-07-31.
 */
object Clipboard {
    fun fetch(): String = try {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
    }
    catch (e: UnsupportedFlavorException) {
        ""
    }

    fun copy(s: String) {
        val selection = StringSelection(s)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}