package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.UserInterface.Bulletin;

/**
 * Created by minjaesong on 16-01-23.
 */
public class SetBulletin implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        new Echo().execute(Lang.get("ERROR_SAVE_CORRUPTED")
                + " "
                + Lang.get("MENU_LABEL_CONTINUE_QUESTION"));

        String[] testMsg = {
                "SetBulletin: this is a test!"
                , "게임 내 방송입니다."
        };
        send(testMsg);
    }

    @Override
    public void printUsage() {

    }

    /**
     * Actually send bulletin
     * @param message real message
     */
    public void send(String[] message) {
        ((Bulletin) (Terrarum.game.bulletin.getUI())).sendBulletin(message);
        System.out.println("sent bulletin");
    }
}
