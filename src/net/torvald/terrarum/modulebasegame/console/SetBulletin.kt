package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-01-23.
 */
internal object SetBulletin : ConsoleCommand {
    override fun execute(args: Array<String>) {
        //send(Lang["ERROR_SAVE_CORRUPTED"], Lang["MENU_LABEL_CONTINUE_QUESTION"])

        (Terrarum.ingame!! as TerrarumIngame).sendNotification(args.sliceArray(1..args.lastIndex))
        println("sent notifinator")
    }

    override fun printUsage() {

    }

}
