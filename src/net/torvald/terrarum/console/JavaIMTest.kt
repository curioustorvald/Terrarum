package net.torvald.terrarum.console

import net.torvald.terrarum.swingapp.IMStringReader

/**
 * Created by minjaesong on 2017-02-05.
 */

internal object JavaIMTest : ConsoleCommand {

    override fun execute(args: Array<String>) {
        IMStringReader(
                { Echo("[JavaIMTest -> IMStringReader] $it") }, // send input to Echo
                "JavaIMTest"
        )
    }

    override fun printUsage() {
        Echo("Tests Swing input window to get non-English text input")
    }
}