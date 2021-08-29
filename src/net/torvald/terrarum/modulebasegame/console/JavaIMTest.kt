package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
//import net.torvald.terrarum.swingapp.IMStringReader

/**
 * Created by minjaesong on 2017-02-05.
 */

@ConsoleAlias("imtest")
internal object JavaIMTest : ConsoleCommand {

    override fun execute(args: Array<String>) {
        /*IMStringReader(
                { Echo("[JavaIMTest -> IMStringReader] $it") }, // send input to Echo
                "JavaIMTest"
        )*/
        val inputListener = object : Input.TextInputListener {
            override fun input(text: String?) {
                Echo("[TextInputText] $text")
            }
            override fun canceled() {
                Echo("[TextInputText] (input canceled)")
            }
        }
        Gdx.input.getTextInput(inputListener, "TextInputTest", "", "type anything!")
    }

    override fun printUsage() {
        Echo("Tests Swing input window to get non-English text input")
    }
}