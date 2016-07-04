package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-22.
 */
class GetLocale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo().execute(
                "Locale: "
                + Lang["MENU_LANGUAGE_THIS"]
                + " ("
                + Lang["MENU_LANGUAGE_THIS_EN"]
                + ")")
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Usage: getlocale")
        echo.execute("Get name of locale currently using.")
    }
}
