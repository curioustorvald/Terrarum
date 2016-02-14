package com.Torvald.Terrarum.ConsoleCommand;

import java.util.Hashtable;

/**
 * Created by minjaesong on 16-01-15.
 */
public class CommandDict {

    protected static Hashtable<String, ConsoleCommand> dict;

    public CommandDict() {
        dict = new Hashtable<>();

        dict.put("setav", new SetAV());
        dict.put("qqq", new QuitApp());
        dict.put("codex", new CodexEdictis());
        dict.put("export", new ExportMap());
        dict.put("gc", new ForceGC());
        dict.put("getav", new GetAV());
        dict.put("getlocale", new GetLocale());
        dict.put("togglenoclip", new ToggleNoClip());
        dict.put("nc", dict.get("togglenoclip"));
        dict.put("bulletintest", new SetBulletin());
        dict.put("setlocale", new SetLocale());
        dict.put("zoom", new Zoom());
        dict.put("teleport", new TeleportPlayer());
        dict.put("cat", new CatStdout());
        dict.put("exportav", new ExportAV());
        dict.put("gsontest", new GsonTest());
    }

    public static ConsoleCommand getCommand(String commandName) {
        return dict.get(commandName);
    }

}
