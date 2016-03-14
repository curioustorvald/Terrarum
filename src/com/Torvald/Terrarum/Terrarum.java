package com.Torvald.Terrarum;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.Torvald.ImageFont.GameFontWhite;
import com.Torvald.JsonFetcher;
import com.Torvald.JsonWriter;
import com.Torvald.Terrarum.LangPack.Lang;
import com.google.gson.JsonObject;
import org.lwjgl.input.Controllers;
import org.newdawn.slick.*;
import org.newdawn.slick.state.StateBasedGame;

/**
 * Created by minjaesong on 15-12-30.
 */
public class Terrarum extends StateBasedGame {

    /**
     * To be used with physics simulator
     */
    public static final int TARGET_FPS = 50;

    /**
     * To be used with render, to achieve smooth frame drawing
     *
     * TARGET_INTERNAL_FPS > TARGET_FPS for smooth frame drawing
     *
     * Must choose a value so that (1000 / VAL) is still integer
     */
    public static final int TARGET_INTERNAL_FPS = 100;

    public static AppGameContainer appgc;

    public static final int WIDTH = 1060;
    public static final int HEIGHT = 742; // IMAX ratio
    public static boolean VSYNC = true;
    public static final int VSYNC_TRIGGER_THRESHOLD = 56;

    public static Game game;
    public static GameConfig gameConfig;

    public static String OSName;
    public static String OSVersion;
    public static String OperationSystem;
    public static String defaultDir;
    public static String defaultSaveDir;

    public static String gameLocale = ""; // locale override

    public static Font gameFontWhite;

    public static final int SCENE_ID_HOME = 1;
    public static final int SCENE_ID_GAME = 3;

    public static boolean hasController = false;
    public static final float CONTROLLER_DEADZONE = 0.1f;

    private static String configDir;

    public Terrarum(String gamename) throws SlickException {
        super(gamename);

        gameConfig = new GameConfig();

        getDefaultDirectory();
        createDirs();

        boolean readFromDisk = readConfigJson();
        if (!readFromDisk) readConfigJson();

        // get locale from config
        gameLocale = gameConfig.getAsString("language");

        // if game locale were not set, use system locale
        if (gameLocale.length() < 4)
            gameLocale = getSysLang();

        System.out.println("[Terrarum] Locale: " + gameLocale);
    }

    @Override
    public void initStatesList(GameContainer gc) throws SlickException {
        gameFontWhite = new GameFontWhite();

        try { new Lang(); }
        catch (IOException e) { e.printStackTrace(); }

        hasController = (gc.getInput().getControllerCount() > 0);
        if (hasController) {
            for (int c = 0; c < Controllers.getController(0).getAxisCount(); c++) {
                Controllers.getController(0).setDeadZone(c, CONTROLLER_DEADZONE);
            }
        }

        appgc.getInput().enableKeyRepeat();

        game = new Game();
        addState(game);
    }

    public static void main(String[] args)
    {
        try
        {
            appgc = new AppGameContainer(new Terrarum("Terrarum"));
            appgc.setDisplayMode(WIDTH, HEIGHT, false);

            appgc.setTargetFrameRate(TARGET_INTERNAL_FPS);
            appgc.setVSync(VSYNC);
            appgc.setMaximumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS);
            appgc.setMinimumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS - 1);

            appgc.setShowFPS(false);
            appgc.setUpdateOnlyWhenVisible(false);

            appgc.start();
        }
        catch (SlickException ex)
        {
            Logger.getLogger(Terrarum.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void getDefaultDirectory(){
        OSName = System.getProperty("os.name");
        OSVersion = System.getProperty("os.version");

        String OS = System.getProperty("os.name").toUpperCase();
        if (OS.contains("WIN")){
            OperationSystem = "WINDOWS";
            defaultDir = System.getenv("APPDATA") + "/Terrarum";
        }
        else if (OS.contains("OS X")){
            OperationSystem = "OSX";
            defaultDir = System.getProperty("user.home") + "/Library/Application "
                    + "Support" + "/Terrarum";
        }
        else if (OS.contains("NUX") || OS.contains("NIX")){
            OperationSystem = "LINUX";
            defaultDir = System.getProperty("user.home") + "/.terrarum";
        }
        else if (OS.contains("SUNOS")){
            OperationSystem = "SOLARIS";
            defaultDir = System.getProperty("user.home") + "/.terrarum";
        }
        else{
            OperationSystem = "UNKNOWN";
            defaultDir = System.getProperty("user.home") + "/.terrarum";
        }

        defaultSaveDir = defaultDir + "/Saves";
        configDir = defaultDir + "/config.json";
    }

    private static void createDirs(){
        File[] dirs = {
                new File(defaultSaveDir),
        };

        for (File d : dirs){
            if (!d.exists()){
                d.mkdirs();
            }
        }
    }

    private static void createConfigJson() throws IOException {
        File configFile = new File(configDir);

        if (!configFile.exists() || configFile.length() == 0) {
            JsonWriter.writeToFile(DefaultConfig.fetch(), configDir);
        }
    }

    private static boolean readConfigJson() {
        try {
            // read from disk and build config from it
            JsonObject jsonObject = JsonFetcher.readJson(configDir);

            // make config
            jsonObject.entrySet().forEach(
                    entry -> gameConfig.set(entry.getKey(), entry.getValue())
            );

            return true;
        }
        catch (IOException e) {
            // write default config to game dir. Call this method again to read config from it.
            try {
                createConfigJson();
            }
            catch (IOException e1) {
                e.printStackTrace();
            }

            return false;
        }
    }

    public static String getSysLang() {
        String lan = System.getProperty("user.language");
        String country = System.getProperty("user.country");

        // exception handling
        if      (lan.equals("en")) country = "US";
        else if (lan.equals("fr")) country = "FR";
        else if (lan.equals("de")) country = "DE";
        else if (lan.equals("ko")) country = "KR";

        return lan + country;
    }


    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * @return Config from config set or default config if it does not exist.
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static int getConfigInt(String key) {
        int cfg = 0;
        try {
            cfg = gameConfig.getAsInt(key);
        }
        catch (NullPointerException e) {
            try {
                cfg = DefaultConfig.fetch().get(key).getAsInt();
            }
            catch (NullPointerException e1) {
                e.printStackTrace();
            }
        }
        return cfg;
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * @return Config from config set or default config if it does not exist.
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static float getConfigFloat(String key) {
        float cfg = 0;
        try {
            cfg = gameConfig.getAsFloat(key);
        }
        catch (NullPointerException e) {
            try {
                cfg = DefaultConfig.fetch().get(key).getAsFloat();
            }
            catch (NullPointerException e1) {
                e.printStackTrace();
            }
        }
        return cfg;
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * @return Config from config set or default config if it does not exist.
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static String getConfigString(String key) {
        String cfg = "";
        try {
            cfg = gameConfig.getAsString(key);
        }
        catch (NullPointerException e) {
            try {
                cfg = DefaultConfig.fetch().get(key).getAsString();
            }
            catch (NullPointerException e1) {
                e.printStackTrace();
            }
        }
        return cfg;
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * @return Config from config set or default config if it does not exist.
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static boolean getConfigBoolean(String key) {
        boolean cfg = false;
        try {
            cfg = gameConfig.getAsBoolean(key);
        }
        catch (NullPointerException e) {
            try {
                cfg = DefaultConfig.fetch().get(key).getAsBoolean();
            }
            catch (NullPointerException e1) {
                e.printStackTrace();
            }
        }
        return cfg;
    }
}
