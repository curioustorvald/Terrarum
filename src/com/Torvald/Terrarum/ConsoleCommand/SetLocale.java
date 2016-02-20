package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.ImageFont.GameFontBase;
import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;
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
            Terrarum.gameLocale = args[1].toLowerCase();
            try {
                new Lang();
                new Echo().execute("Set locale to '" + Terrarum.gameLocale + "'.");

                if ((!prevLocale.contains("cn") && Terrarum.gameLocale.contains("cn"))
                        || (prevLocale.contains("cn") && !Terrarum.gameLocale.contains("cn"))) {
                    try { ((GameFontBase) Terrarum.gameFontWhite).reloadUnihan(); }
                    catch (SlickException e) { e.printStackTrace(); }
                }
            }
            catch (IOException e) {
                new Echo().execute("Locale '"
                        + args[1].toLowerCase()
                        + "' does not exist or could not read file."
                );
                Terrarum.gameLocale = prevLocale;
            }
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
