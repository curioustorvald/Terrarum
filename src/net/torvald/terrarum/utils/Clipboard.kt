package net.torvald.terrarum.utils

import net.torvald.terrarum.App
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

/**
 * Created by minjaesong on 2016-07-31.
 */
object Clipboard {
    private val IS_MACOS = App.operationSystem == "OSX"

    fun fetch(): String =
        if (IS_MACOS) "Clipboard is disabled on macOS" else
        try {
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        }
        catch (e: UnsupportedFlavorException) {
            ""
        }

    fun copy(s: String) {
        if (IS_MACOS) return
        val selection = StringSelection(s)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}

/**
 * Created by minjaesong on 2023-08-25.
 */
object OpenFile {
    private val IS_MACOS = App.operationSystem == "OSX"
    operator fun invoke(file: File) {
        if (IS_MACOS) return // at this point macOS might as well be a bane of existence for "some" devs Apple fanboys think they are not worthy of existence
        Desktop.getDesktop().open(file)
    }
}