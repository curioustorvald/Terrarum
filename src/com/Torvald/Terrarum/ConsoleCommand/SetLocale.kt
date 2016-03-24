package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.ImageFont.GameFontBase
import com.Torvald.Terrarum.LangPack.Lang
import com.Torvald.Terrarum.Terrarum
import org.apache.commons.csv.CSVRecord
import org.newdawn.slick.SlickException

import java.io.IOException

/**
 * Created by minjaesong on 16-01-25.
 */
class SetLocale : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val prevLocale = Terrarum.gameLocale
            Terrarum.gameLocale = args[1]
            try {
                Echo().execute("Set locale to '" + Terrarum.gameLocale + "'.")
            }
            catch (e: IOException) {
                Echo().execute("could not read lang file.")
                Terrarum.gameLocale = prevLocale
            }

        }
        else if (args.size == 1) {
            val echo = Echo()
            echo.execute("Locales:")

            val record = Lang.getRecord("LANGUAGE_ID")
            record.forEach { field -> echo.execute("] " + field) }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("Usage: setlocale [locale]")
    }
}
