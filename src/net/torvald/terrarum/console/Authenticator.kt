package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.ConsoleWindow
import org.apache.commons.codec.digest.DigestUtils

/**
 * Created by minjaesong on 16-02-19.
 */
class Authenticator : ConsoleCommand {

    private var a = false

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val pwd = args[1]
            val hashedPwd = DigestUtils.sha256Hex(pwd)

            if ("65b9aa150332ed7096134efb20220e5ebec04d4dbe1c537ff3816f68c2391c1c".equals(hashedPwd, ignoreCase = true)) {
                // aryll
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
}
