package net.torvald.terrarum.console

import net.torvald.terrarum.INGAME
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
 * Created by minjaesong on 2016-02-19.
 */
@ConsoleAlias("auth")
internal object Authenticator : ConsoleCommand {

    private var a = false

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val pwd = args[1]
            val hashedPwd = DigestUtils.sha256Hex(pwd)

            println("auth passwd: '$pwd'")
            println("hash: $hashedPwd")

            if ("2d962f949f55906ac47f16095ded190c9e44d95920259b8f36c2e54bd75df173".equals(hashedPwd, ignoreCase = true)) {
                // beedle
                val msg = if (a) "Locked" else "Authenticated"
                Echo(msg)
                println("[Authenticator] " + msg)
                a = !a
                INGAME.consoleHandler.reset()
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
