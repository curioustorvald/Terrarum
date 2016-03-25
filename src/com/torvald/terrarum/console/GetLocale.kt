package com.torvald.terrarum.console

import com.torvald.terrarum.langpack.Lang
import com.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-22.
 */
class GetLocale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo().execute(
                "Locale: "
                + Lang.get("LANGUAGE_THIS")
                + " ("
                + Lang.get("LANGUAGE_EN")
                + ")")
    }

    override fun printUsage() {
        val echo = Echo()
        echo.execute("Usage: getlocale")
        echo.execute("Get name of locale currently using.")
    }
}
