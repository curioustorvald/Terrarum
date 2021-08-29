package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2016-07-04.
 */
@ConsoleAlias("tips")
internal object PrintRandomTips : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo("Nope.")
        //Echo(Lang["GAME_TIPS_${Random().nextInt(Lang.TIPS_COUNT) + 1}"])
    }

    override fun printUsage() {
        Echo("Prints random tips for game.")
    }
}