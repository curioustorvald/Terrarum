package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-01-22.
 */
public class GetLocale implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        new Echo().execute(
                "Locale: "
                + Lang.get("LANGUAGE_THIS")
                + " ("
                + Lang.get("LANGUAGE_EN")
                + ")"
        );
    }

    @Override
    public void printUsage() {
        Echo echo = new Echo();
        echo.execute("Usage: getlocale");
        echo.execute("Get name of locale currently using.");
    }
}
