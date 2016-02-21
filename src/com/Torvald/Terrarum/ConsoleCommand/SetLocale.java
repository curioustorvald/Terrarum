package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.ImageFont.GameFontBase;
import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;
import org.apache.commons.csv.CSVRecord;
import org.newdawn.slick.SlickException;

import java.io.IOException;

/**
 * Created by minjaesong on 16-01-25.
 */
public class SetLocale implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            String prevLocale = Terrarum.gameLocale;
            Terrarum.gameLocale = args[1];
            try {
                new Lang();
                new Echo().execute("Set locale to '" + Terrarum.gameLocale + "'.");
            }
            catch (IOException e) {
                new Echo().execute("could not read lang file.");
                Terrarum.gameLocale = prevLocale;
            }
        }
        else if (args.length == 1) {
            Echo echo = new Echo();
            echo.execute("Locales:");

            CSVRecord record = Lang.getRecord("LANGUAGE_ID");
            record.forEach(field -> echo.execute("] " + field));
        }
        else {
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("Usage: setlocale [locale]");
    }
}
