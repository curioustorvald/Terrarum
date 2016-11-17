package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-19.
 */
class GetAV : ConsoleCommand {

    val ccW = GameFontBase.colToCode["w"]
    val ccG = GameFontBase.colToCode["g"]
    val ccY = GameFontBase.colToCode["y"]
    val ccM = GameFontBase.colToCode["m"]
    val ccK = GameFontBase.colToCode["k"]
    val ccO = GameFontBase.colToCode["o"]

    override fun execute(args: Array<String>) {
        val echo = Echo()
        val error = Error()

        try {
            if (args.size == 1) {
                // print all actorvalue of player
                val av = Terrarum.ingame.player.actorValue
                val keyset = av.keySet

                echo.execute("$ccW== ActorValue list for ${ccY}player $ccW==")
                println("[GetAV] == ActorValue list for 'player' ==")
                keyset.forEach { elem ->
                    echo.execute("$ccM$elem $ccW= $ccG${av[elem as String]}")
                    println("[GetAV] $elem = ${av[elem]}")
                }
            }
            else if (args.size != 3 && args.size != 2) {
                printUsage()
            }
            else if (args.size == 2) {
                // check if args[1] is number or not
                if (!args[1].isNum()) { // args[1] is ActorValue name
                    echo.execute("${ccW}player.$ccM${args[1]} $ccW= " +
                                 ccG +
                                 Terrarum.ingame.player.actorValue[args[1]] +
                                 " $ccO" +
                                 Terrarum.ingame.player.actorValue[args[1]]!!.javaClass.simpleName
                    )
                    println("[GetAV] player.${args[1]} = " +
                            Terrarum.ingame.player.actorValue[args[1]] +
                            " " +
                            Terrarum.ingame.player.actorValue[args[1]]!!.javaClass.simpleName
                    )
                }
                else {
                    // args[1] is actor ID
                    val actor = Terrarum.ingame.getActorByID(args[1].toInt())
                    val av = actor.actorValue
                    val keyset = av.keySet

                    echo.execute("$ccW== ActorValue list for $ccY$actor $ccW==")
                    println("[GetAV] == ActorValue list for '$actor' ==")
                    if (keyset.size == 0) {
                        echo.execute("$ccK(nothing)")
                        println("[GetAV] (nothing)")
                    }
                    else {
                        keyset.forEach { elem ->
                            echo.execute("$ccM$elem $ccW= $ccG${av[elem as String]}")
                            println("[GetAV] $elem = ${av[elem]}")
                        }
                    }
                }
            }
            else if (args.size == 3) {
                val id = args[1].toInt()
                val av = args[2]
                echo.execute("$ccW$id.$ccM$av $ccW= $ccG" +
                             Terrarum.ingame.getActorByID(id).actorValue[av] +
                             " $ccO" +
                             Terrarum.ingame.getActorByID(id).actorValue[av]!!.javaClass.simpleName
                )
                println("id.av = " +
                        Terrarum.ingame.getActorByID(id).actorValue[av] +
                        " " +
                        Terrarum.ingame.getActorByID(id).actorValue[av]!!.javaClass.simpleName
                )
            }
        }
        catch (e: NullPointerException) {
            if (args.size == 2) {
                error.execute(args[1] + ": actor value does not exist.")
                System.err.println("[GetAV] ${args[1]}: actor value does not exist.")
            }
            else if (args.size == 3) {
                error.execute(args[2] + ": actor value does not exist.")
                System.err.println("[GetAV] ${args[2]}: actor value does not exist.")
            }
            else {
                throw NullPointerException()
            }
        }
        catch (e1: IllegalArgumentException) {
            error.execute("${args[1]}: no actor with this ID.")
            System.err.println("[GetAV] ${args[1]}: no actor with this ID.")
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
        echo.execute("${ccW}Get desired ActorValue of specific target.")
        echo.execute("${ccW}Usage: ${ccY}getav ${ccG}(id) <av>")
        echo.execute("${ccW}blank ID for player")
    }
}
