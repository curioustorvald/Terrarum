package net.torvald.terrarum.console

import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.Game
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-19.
 */
class GetAV : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val echo = Echo()

        try {
            if (args.size == 1) {
                // print all actorvalue of player
                val av = Terrarum.game.player.actorValue
                val keyset = av.keySet

                echo.execute("== ActorValue list for player ==")
                keyset.forEach { elem -> echo.execute("$elem = ${av[elem as String]}") }
            }
            else if (args.size != 3 && args.size != 2) {
                printUsage()
            }
            else if (args.size == 2) {
                // check if args[1] is number or not
                if (!args[1].isNum()) { // args[1] is ActorValue name
                    echo.execute("player.${args[1]} = "
                                 + Terrarum.game.player.actorValue[args[1]]
                                 + " ("
                                 + Terrarum.game.player.actorValue[args[1]]!!.javaClass.simpleName
                                 + ")"
                    )
                }
                else { // args[1] is actor ID
                    val av = Terrarum.game.getActor(args[1].toInt()).actorValue
                    val keyset = av.keySet

                    echo.execute("== ActorValue list for ${args[1].toInt()} ==")
                    if (keyset.size == 0)
                        echo.execute("(nothing)")
                    else
                        keyset.forEach { elem -> echo.execute("$elem = ${av[elem as String]}") }
                }
            }
            else if (args.size == 3) {
                val id = args[1].toInt()
                val av = args[2]
                echo.execute("$id.$av = " +
                             Terrarum.game.getActor(id).actorValue[av] +
                             " (" +
                             Terrarum.game.getActor(id).actorValue[av]!!.javaClass.simpleName +
                             ")"
                )
            }
        }
        catch (e: NullPointerException) {
            if (args.size == 2) {
                echo.error(args[1] + ": actor value does not exist.")
            }
            else if (args.size == 3) {
                echo.error(args[2] + ": actor value does not exist.")
            }
            else {
                throw NullPointerException()
            }
        }
        catch (e1: IllegalArgumentException) {
            if (args.size == 3) {
                echo.error(args[1] + ": no actor with this ID.")
            }
        }

    }

    fun String.isNum(): Boolean {
        try {
            this.toInt()
            return true
        }
        catch (e: NumberFormatException) {
            return false
        }
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Get desired actor value of specific target.")
        echo.execute("Usage: getav (id) <av>")
        echo.execute("blank ID for player")
    }
}
