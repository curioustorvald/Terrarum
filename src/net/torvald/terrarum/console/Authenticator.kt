package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.ui.ConsoleWindow
import org.apache.commons.codec.digest.DigestUtils

/**
 *
 * Password setting rules:
 *
 * For each releases new password should be set. The new password must:
 *      - start with next alphabet of previous password
 *          if previous password started with Z, the new password must start with A
 *      - be a name appear in the Legend of Zelda series which officially released by Nintendo (no CD-i)
 *      - be lowercase
 *      - BE CRACKABLE (crackstation.net)
 *
 * Example passwords would be:
 *  aryll -> beedle -> ciela -> daruk -> ... -> linebeck -> mido -> navi -> ...
 *
 * Notes:
 *      do NOT put plaintext anywhere in the code (except for comments maybe)
 *      must use SHA-256
 *
 * Created by minjaesong on 16-02-19.
 */
internal object Authenticator : ConsoleCommand {

    private var a = false

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val pwd = args[1]
            val hashedPwd = DigestUtils.sha256Hex(pwd)

            if ("65b9aa150332ed7096134efb20220e5ebec04d4dbe1c537ff3816f68c2391c1c".equals(hashedPwd, ignoreCase = true)) {
                // aryll
                val msg = if (a) "Locked" else "Authenticated"
                Echo(msg)
                println("[Authenticator] " + msg)
                a = !a
                (TerrarumGDX.ingame!!.consoleHandler.UI as ConsoleWindow).reset()
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
