package com.Torvald.Terrarum;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.Torvald.ImageFont.GameFontBase;
import com.Torvald.ImageFont.GameFontBlack;
import com.Torvald.ImageFont.GameFontWhite;
import com.Torvald.Terrarum.Actors.PlayerBuildFactory;
import com.Torvald.Terrarum.GameControl.GameController;
import com.Torvald.Terrarum.GameControl.KeyMap;
import com.Torvald.Terrarum.LangPack.Lang;
import org.newdawn.slick.*;

/**
 * Created by minjaesong on 15-12-30.
 */
public class Terrarum extends BasicGame {

    public static AppGameContainer appgc;
    public static final int WIDTH = 960;
    public static final int HEIGHT = 720;
    private static Game game;
    public static final int TARGET_FPS = 50;

    /**
     * To be used with render, to achieve smooth frame drawing
     *
     * TARGET_INTERNAL_FPS > TARGET_FPS for smooth frame drawing
     */
    public static final int TARGET_INTERNAL_FPS = 100;

    public static String OSName;
    public static String OSVersion;
    public static String OperationSystem;
    public static String defaultDir;
    public static String defaultSaveDir;

    public static String gameLocale = "ko";

    public static Font gameFontWhite;

    public static long memInUse;
    public static long totalVMMem;

    public Terrarum(String gamename) {
        super(gamename);
    }

    @Override
    public void init(GameContainer gc) throws SlickException {
        getDefaultDirectory();
        createDirs();
        try {
            createFiles();
            new Lang();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        game = new Game();

        new GameController();
        KeyMap.build();
        GameController.setKeyMap(new KeyMap());

        gameFontWhite = new GameFontWhite();


    }

    @Override
    public void update(GameContainer gc, int delta_t) throws SlickException{
        Runtime runtime = Runtime.getRuntime();
        memInUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
        totalVMMem = runtime.maxMemory() >> 20;

        appgc.setTitle(
                "Simple Slick Game — FPS: "
                + appgc.getFPS() + " ("
                + String.valueOf(TARGET_INTERNAL_FPS)
                + ") — "
                + String.valueOf(memInUse) + "M / "
                + String.valueOf(totalVMMem) + "M"
        );
        Game.update(gc, delta_t);
    }

    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException
    {
        Game.render(gc, g);
    }

    public void keyPressed(int key, char c) {
        GameController.keyPressed(key, c);
    }

    public void keyReleased(int key, char c) {
        GameController.keyReleased(key, c);
    }

    public void mouseMoved(int oldx, int oldy, int newx, int newy) {
        GameController.mouseMoved(oldx, oldy, newx, newy);
    }

    public void mouseDragged(int oldx, int oldy, int newx, int newy) {
        GameController.mouseDragged(oldx, oldy, newx, newy);
    }

    public void mousePressed(int button, int x, int y) {
        GameController.mousePressed(button, x, y);
    }

    public void mouseReleased(int button, int x, int y) {
        GameController.mouseReleased(button, x, y);
    }

    public void mouseWheelMoved(int change) {
        GameController.mouseWheelMoved(change);
    }

    public void controllerButtonPressed(int controller, int button) {
        GameController.controllerButtonPressed(controller, button);
    }

    public void controllerButtonReleased(int controller, int button) {
        GameController.controllerButtonReleased(controller, button);
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
