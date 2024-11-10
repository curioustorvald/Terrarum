package net.torvald.terrarum.console

import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.TheGameWorld
import net.torvald.terrarum.modulebasegame.console.GetAV.isNum

/**
 * Created by minjaesong on 2022-06-03.
 */
internal object GetGR : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val gameRules = (INGAME.world as TheGameWorld).gameRules

        // check if args[1] is number or not
        if (args.size > 1 && !args[1].isNum()) { // args[1] is Gamerule name
            gameRules[args[1]].let {
                if (it != null) {
                    Echo("${ccW}Gamerule $ccM${args[1]} $ccW= " +
                         ccG +
                         it +
                         " $ccO" +
                         it.javaClass.simpleName
                    )
                    println("[GetGR] Gamerule ${args[1]} = " +
                            it +
                            " " +
                            it.javaClass.simpleName
                    )
                }
                else {
                    EchoError("No such Gamerule defined: ${args[1]}")
                    println("[GetGR] No such Gamerule defined: ${args[1]}")
                }
            }

        }
        else {
            // args[1] is actor ID
            val keyset = gameRules.keySet

            Echo("$ccW== List of$ccY Gamerules $ccW==")
            println("[GetGR] == List of Gamerules ==")
            if (keyset.isEmpty()) {
                Echo("$ccK(nothing)")
                println("[GetGR] (nothing)")
            }
            else {
                keyset.forEach { elem ->
                    Echo("$ccM$elem $ccW= $ccG${gameRules[elem as String]}")
                    println("[GetGR] $elem = ${gameRules[elem]}")
                }
            }
        }
    }

    override fun printUsage() {
        Echo("Usage: getgr <gamerule>")
    }
}