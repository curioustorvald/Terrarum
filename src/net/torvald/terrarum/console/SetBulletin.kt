package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.ui.Notification

/**
 * Created by minjaesong on 16-01-23.
 */
internal object SetBulletin : ConsoleCommand {
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
        TerrarumGDX.ingame!!.sendNotification(message)
        println("sent notifinator")
    }
}
