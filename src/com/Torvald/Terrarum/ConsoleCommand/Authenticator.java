package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.ConsoleWindow;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Created by minjaesong on 16-02-19.
 */
public class Authenticator implements ConsoleCommand {

    private static boolean a = false;

    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            String pwd = args[1];

            String hashedPwd = DigestUtils.sha256Hex(pwd);

            if ("54c5b3dd459d5ef778bb2fa1e23a5fb0e1b62ae66970bcb436e8f81a1a1a8e41".equalsIgnoreCase(hashedPwd)) {
                // alpine
                String msg = (a) ? "Locked" : "Authenticated";
                new Echo().execute(msg);
                System.out.println("[Authenticator] " + msg);
                a = !a;
                ((ConsoleWindow) (Terrarum.game.consoleHandler.getUI())).reset();
            }
            else {
                printUsage(); // thou shalt not pass!
            }
        }
        else {
            printUsage();
        }
    }

    public boolean b() {
        return a;
    }

    @Override
    public void printUsage() {
        CommandInterpreter.echoUnknownCmd("auth");
    }
}
