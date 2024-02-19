package net.torvald.terrarum.utils

import com.badlogic.gdx.Gdx
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

    fun fetch(): String = Gdx.app.clipboard.contents ?: ""

    fun copy(s: String) {
        Gdx.app.clipboard.contents = s
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