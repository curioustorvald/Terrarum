package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;
import org.apache.commons.codec.digest.DigestUtils;
import org.newdawn.slick.SlickException;

/**
 * Created by minjaesong on 16-02-19.
 */
public class Authenticator implements ConsoleCommand {

    private static final String A = "0d34076fc15db1b7c7a0943045699eba6f186ec1"; // alpine
    // or 'srisrimahasri'

    private static boolean B = false;

    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            String pwd = args[1];

            String hashedPwd = DigestUtils.sha1Hex(pwd);

            if (A.equalsIgnoreCase(hashedPwd)) {
                new Echo().execute(((B) ? "Dis" : "") + "authenticated.");
                B = !B;
                ((ConsoleWindow) (Terrarum.game.consoleHandler.getUI())).reset();
            }
        }
        else {
            printUsage();
        }
    }

    public boolean C() {
        return B;
    }

    @Override
    public void printUsage() {
        CommandInterpreter.echoUnknownCmd("auth");
    }
}
