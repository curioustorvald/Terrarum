package com.torvald.terrarum.console

import com.torvald.terrarum.langpack.Lang
import com.torvald.terrarum.Terrarum
import com.torvald.terrarum.ui.Notification

/**
 * Created by minjaesong on 16-01-23.
 */
class SetBulletin : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val testMsg = arrayOf(
                Lang["ERROR_SAVE_CORRUPTED"],
                Lang["MENU_LABEL_CONTINUE_QUESTION"]
        )
        send(testMsg)
    }

    override fun printUsage() {

    }

    /**
     * Actually send notifinator
     * @param message real message
     */
    fun send(message: Array<String>) {
        Terrarum.game.sendNotification(message)
        println("sent notifinator")
    }
}
