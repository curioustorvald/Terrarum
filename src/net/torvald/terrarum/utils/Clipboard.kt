package net.torvald.terrarum.utils

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.App
import java.awt.Desktop
import java.io.File
import java.net.URL

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

object OpenURL {
    private val IS_MACOS = App.operationSystem == "OSX"
    operator fun invoke(url: URL) {
        Gdx.net.openURI(url.toURI().toString())
    }
}