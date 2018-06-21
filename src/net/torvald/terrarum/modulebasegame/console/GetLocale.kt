package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2016-01-22.
 */
internal object GetLocale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo(
                "Locale: "
                + Lang["MENU_LANGUAGE_THIS"]
                + " ("
                + Lang["MENU_LANGUAGE_THIS_EN"]
                + ")")
    }

    override fun printUsage() {

        Echo("Usage: getlocale")
        Echo("Get name of locale currently using.")
    }
}
