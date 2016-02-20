package com.Torvald.Terrarum;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.Torvald.ImageFont.GameFontBase;
import com.Torvald.ImageFont.GameFontWhite;
import com.Torvald.Terrarum.ConsoleCommand.Authenticator;
import com.Torvald.Terrarum.LangPack.Lang;
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
     * Must choose a value so that 1000 / VAL is still integer
     */
    public static final int TARGET_INTERNAL_FPS = 100;

    public static AppGameContainer appgc;
    public static final int WIDTH = 960;
    public static final int HEIGHT = 720;
    public static Game game;

    public static String OSName;
    public static String OSVersion;
    public static String OperationSystem;
    public static String defaultDir;
    public static String defaultSaveDir;

    public static String gameLocale = "jp";

    public static Font gameFontWhite;

    public static final int SCENE_ID_HOME = 1;
    public static final int SCENE_ID_GAME = 3;

    public static boolean hasController = false;
    public static final float CONTROLLER_DEADZONE = 0.1f;

    public Terrarum(String gamename) throws SlickException {
        super(gamename);

        getDefaultDirectory();
        createDirs();
        try {
            createFiles();
            new Lang();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initStatesList(GameContainer gc) throws SlickException {
        gameFontWhite = new GameFontWhite();

        hasController = (gc.getInput().getControllerCount() > 0);
        if (hasController) {
            for (int c = 0; c < Controllers.getController(0).getAxisCount(); c++) {
                Controllers.getController(0).setDeadZone(c, CONTROLLER_DEADZONE);
            }
        }

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
            appgc.setVSync(true);
            appgc.setShowFPS(false);
            appgc.setUpdateOnlyWhenVisible(false);
            appgc.setMaximumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS);
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

    private static void createFiles() throws IOException {
        File[] files = {
                new File(defaultDir + "/properties")
        };

        for (File f : files){
            if (!f.exists()){
                f.createNewFile();
            }
        }
    }
}
