package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.ConsoleWindow
import org.apache.commons.codec.digest.DigestUtils

/**
 * Created by minjaesong on 16-02-19.
 */
class Authenticator : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val pwd = args[1]
            val hashedPwd = DigestUtils.sha256Hex(pwd)

            if ("54c5b3dd459d5ef778bb2fa1e23a5fb0e1b62ae66970bcb436e8f81a1a1a8e41".equals(hashedPwd, ignoreCase = true)) {
                // alpine
                val msg = if (a) "Locked" else "Authenticated"
                Echo().execute(msg)
                println("[Authenticator] " + msg)
                a = !a
                (Terrarum.game.consoleHandler.UI as ConsoleWindow).reset()
            }
            else {
                printUsage() // thou shalt not pass!
            }
        }
        else {
            printUsage()
        }
    }

    fun b(): Boolean {
        return a
    }

    override fun printUsage() {
        CommandInterpreter.echoUnknownCmd("auth")
    }

    companion object {

        private var a = false
    }
}
