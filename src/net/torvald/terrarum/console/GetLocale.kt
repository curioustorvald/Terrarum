package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-01-22.
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
