package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.Notification;

/**
 * Created by minjaesong on 16-01-23.
 */
public class SetBulletin implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        String[] testMsg = {
                //Lang.get("ERROR_SAVE_CORRUPTED")
                //, Lang.get("MENU_LABEL_CONTINUE_QUESTION")
                "갎갎갎갎갎갎갎갎갎갎갎갎갎갎"
        };
        send(testMsg);
    }

    @Override
    public void printUsage() {

    }

    /**
     * Actually send notifinator
     * @param message real message
     */
    public void send(String[] message) {
        Terrarum.game.sendNotification(message);
        System.out.println("sent notifinator");
    }
}
