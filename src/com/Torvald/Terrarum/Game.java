package com.Torvald.Terrarum;

import com.Torvald.Rand.HighQualityRandom;
import com.Torvald.Terrarum.Actors.*;
import com.Torvald.Terrarum.ConsoleCommand.CommandDict;
import com.Torvald.Terrarum.GameControl.GameController;
import com.Torvald.Terrarum.GameControl.KeyToggler;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.MapDrawer.LightmapRenderer;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.MapGenerator.MapGenerator;
import com.Torvald.Terrarum.TileStat.TileStat;
import com.Torvald.Terrarum.UserInterface.*;
import com.sun.istack.internal.NotNull;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.*;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.fills.GradientFill;
import org.newdawn.slick.geom.Rectangle;
import shader.Shader;

import java.util.LinkedList;

/**
 * Created by minjaesong on 15-12-30.
 */
public class Game {

    static int game_mode = 0;

    public static GameConfig gameConfig;

    public static GameMap map;

    public static LinkedList<Actor> actorContainer = new LinkedList<>();
    public static LinkedList<UIHandler> uiContainer = new LinkedList<>();

    public static UIHandler consoleHandler;
    public static UIHandler debugWindow;
    public static UIHandler bulletin;

    @NotNull
    static Player player;

    public static final long PLAYER_REF_ID = 0x51621D;

    private static Image GRADIENT_IMAGE;
    private static Rectangle skyBox;

    public static float screenZoom = 1.0f;
    public static final float ZOOM_MAX = 2.0f;
    public static final float ZOOM_MIN = 0.25f;

    private static Shader shader12BitCol;
    private static Shader shaderBlurH;
    private static Shader shaderBlurV;

    public Game() throws SlickException {
        gameConfig = new GameConfig();
        gameConfig.addKey("smoothlighting", true);

        shader12BitCol = Shader.makeShader("./res/4096.vrt", "./res/4096.frg");
        shaderBlurH = Shader.makeShader("./res/blurH.vrt", "./res/blur.frg");
        shaderBlurV = Shader.makeShader("./res/blurV.vrt", "./res/blur.frg");

        GRADIENT_IMAGE = new Image("res/graphics/backgroundGradientColour.png");
        skyBox = new Rectangle(0, 0, Terrarum.WIDTH, Terrarum.HEIGHT);

        new WorldTime();

        map = new GameMap(8192, 2048);
        map.setGravitation(9.8f);

        MapGenerator.attachMap(map);
        MapGenerator.setSeed(0x51621D);
        //MapGenerator.setSeed(new HighQualityRandom().nextLong());
        MapGenerator.generateMap();

        new CommandDict();

        // add new player and put it to actorContainer
        //player = new Player();
        player = new PBFSigrid().build();
        //player.setNoClip(true);
        actorContainer.add(player);

        new MapDrawer(map);

        new LightmapRenderer();

        consoleHandler = new UIHandler(new ConsoleWindow());
        consoleHandler.setPosition(0, 0);

        debugWindow = new UIHandler(new BasicDebugInfoWindow());
        debugWindow.setPosition(0, 0);

        /*bulletin = new UIHandler(new Bulletin());
        bulletin.setPosition(
                (Terrarum.WIDTH - bulletin.getUI().getWidth())
                        / 2
                , 0
        );
        bulletin.setVisibility(true);
        */

        UIHandler msgtest = new UIHandler(new Message(400, true));
        String[] msg = {"Hello, world!", "안녕, 세상아!"};
        ((Message) msgtest.getUI()).setMessage(msg);
        msgtest.setPosition(32, 32);
        // msgtest.setVisibility(true);

        uiContainer.add(msgtest);
    }

    public static Player getPlayer() {
        return player;
    }

    public static void update(GameContainer gc, int delta_t) {
        MapDrawer.update(gc, delta_t);

        GameController.processInput(gc.getInput());

        actorContainer.forEach(actor -> actor.update(gc, delta_t));
        actorContainer.forEach(
                actor -> {
                    if (actor instanceof Visible) {
                        ((Visible) actor).updateBodySprite(gc, delta_t);
                    }
                    if (actor instanceof Glowing) {
                        ((Glowing) actor).updateGlowSprite(gc, delta_t);
                    }
                }
        );

        uiContainer.forEach(ui -> ui.update(gc, delta_t));

        KeyToggler.update(gc);

        //bulletin.update(gc, delta_t);

        TileStat.update();
    }

    public static void render(GameContainer gc, Graphics g) {
        // shader12BitCol.setUniformIntVariable("pixelSize", 1);
        // shader12BitCol.startShader();
        // shaderBlurH.startShader();
        // shaderBlurV.startShader();

        drawSkybox(g);

        // compensate for zoom. UIs have to be treated specially! (see UIHandler)
        g.translate(
                -MapCamera.getCameraX() * screenZoom
                , -MapCamera.getCameraY() * screenZoom
        );

        MapDrawer.render(gc, g);
        actorContainer.forEach(
                actor -> {
                    if (actor instanceof Visible) {
                        ((Visible) actor).drawBody(gc, g);
                    }
                }
        );

        // Slick's MODE_COLOR_MULTIPLY is clearly broken... using GL11
        LightmapRenderer.renderLightMap();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // draw lightmap
        LightmapRenderer.draw(g);
        // draw environment colour overlay
        // MapDrawer.drawEnvOverlay(g);
        GL11.glDisable(GL11.GL_BLEND);
        g.setDrawMode(Graphics.MODE_NORMAL);

        actorContainer.forEach(
                actor -> {
                    if (actor instanceof Glowing) {
                        ((Glowing) actor).drawGlow(gc, g);
                    }
                }
        );

        uiContainer.forEach(ui -> ui.render(gc, g));
        debugWindow.render(gc, g);
        consoleHandler.render(gc, g);
        //bulletin.render(gc, g);
    }

    private static Color[] getGradientColour(int timeSec) {
        Color[] colourTable = new Color[2];

        int gradMapWidth = GRADIENT_IMAGE.getWidth();
        int phase = Math.round((timeSec / WorldTime.DAY_LENGTH) * gradMapWidth);

        //update in every 60 frames
        colourTable[0] = GRADIENT_IMAGE.getColor(phase, 0);
        colourTable[1] = GRADIENT_IMAGE.getColor(phase, 1);

        return colourTable;
    }

    private static void drawSkybox(Graphics g) {
        Color[] colourTable = getGradientColour(WorldTime.elapsedSeconds());
        GradientFill skyColourFill = new GradientFill(0, 0, colourTable[0], 0, Terrarum.HEIGHT, colourTable[1]);
        g.fill(skyBox, skyColourFill);
    }
}
