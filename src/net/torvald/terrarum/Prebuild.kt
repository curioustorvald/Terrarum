package net.torvald.terrarum

import java.io.File

/**
 * Created by minjaesong on 2023-08-25.
 */

fun main() {
    val s = App.getVERSION_STRING()
    val f = File("./out/build_version_string.autogen")
    f.delete()
    f.writeText(s)
}