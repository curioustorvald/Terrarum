package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.swingapp.IMStringReader
import javax.swing.JFrame

/**
 * Created by SKYHi14 on 2017-02-05.
 */

object JavaIMTest : ConsoleCommand {

    override fun execute(args: Array<String>) {
        IMStringReader(
                Terrarum.appgc,
                { Echo("[JavaIMTest -> IMStringReader] $it") }, // send input to Echo
                "JavaIMTest"
        )
    }

    override fun printUsage() {
        Echo("Tests Swing input window to get non-English text input")
    }
}