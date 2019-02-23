package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-01-23.
 */
internal object SetBulletin : ConsoleCommand {
    override fun execute(args: Array<String>) {
        send(Lang["ERROR_SAVE_CORRUPTED"], Lang["MENU_LABEL_CONTINUE_QUESTION"])
    }

    override fun printUsage() {

    }

    /**
     * Actually send notifinator
     * @param message real message
     */
    fun send(msg1: String, msg2: String? = null) {
        (Terrarum.ingame!! as Ingame).sendNotification(msg1, msg2)
        println("sent notifinator")
    }
}
