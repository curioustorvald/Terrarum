package com.Torvald.Terrarum;

import com.Torvald.Terrarum.Actors.*;
import com.Torvald.Terrarum.ConsoleCommand.CommandDict;
import com.Torvald.Terrarum.GameControl.GameController;
import com.Torvald.Terrarum.GameControl.KeyMap;
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
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import shader.Shader;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;

/**
 * Created by minjaesong on 15-12-30.
 */
public class Game extends BasicGameState {

    public static long memInUse;
    public static long totalVMMem;
    int game_mode = 0;

    public GameConfig gameConfig;

    public GameMap map;

    public LinkedList<Actor> actorContainer = new LinkedList<>();
    public LinkedList<UIHandler> uiContainer = new LinkedList<>();

    public UIHandler consoleHandler;
    public UIHandler debugWindow;
    public UIHandler bulletin;

    @NotNull
    Player player;

    private Image GRADIENT_IMAGE;
    private Rectangle skyBox;

    public float screenZoom = 1.0f;
    public final float ZOOM_MAX = 2.0f;
    public final float ZOOM_MIN = 0.25f;

    private Shader shader12BitCol;
    private Shader shaderBlurH;
    private Shader shaderBlurV;

    public Game() throws SlickException {
        new GameController();
        KeyMap.build();
        GameController.setKeyMap(new KeyMap());



        gameConfig = new GameConfig();
        gameConfig.set("smoothlighting", true);

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

        bulletin = new UIHandler(new Bulletin());
        bulletin.setPosition(
                (Terrarum.WIDTH - bulletin.getUI().getWidth())
                        / 2
                , 0
        );
        bulletin.setVisibility(true);


        UIHandler msgtest = new UIHandler(new Message(400, true));
        String[] msg = {"Hello, world!", "안녕, 세상아!"};
        ((Message) msgtest.getUI()).setMessage(msg);
        msgtest.setPosition(32, 32);
        // msgtest.setVisibility(true);

        uiContainer.add(msgtest);
    }

    @Override
    public void init(GameContainer gameContainer, StateBasedGame stateBasedGame) throws
            SlickException {
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void update(GameContainer gc, StateBasedGame sbg, int delta_t) {
        setAppTitle();

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

    private void setAppTitle() {
        Runtime runtime = Runtime.getRuntime();
        memInUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
        totalVMMem = runtime.maxMemory() >> 20;

        Terrarum.appgc.setTitle(
                "Simple Slick Game — FPS: "
                        + Terrarum.appgc.getFPS() + " ("
                        + String.valueOf(Terrarum.TARGET_INTERNAL_FPS)
                        + ") — "
                        + String.valueOf(memInUse) + "M / "
                        + String.valueOf(totalVMMem) + "M"
        );
    }

    @Override
    public void render(GameContainer gc, StateBasedGame sbg, Graphics g) {
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

        actorContainer.forEach(
                actor -> {
                    if (actor instanceof Visible) {
                        ((Visible) actor).drawBody(gc, g);
                    }
                }
        );
        actorContainer.forEach(
                actor -> {
                    if (actor instanceof Glowing) {
                        ((Glowing) actor).drawGlow(gc, g);
                    }
                }
        );
        MapDrawer.render(gc, g);

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

        uiContainer.forEach(ui -> ui.render(gc, g));
        debugWindow.render(gc, g);
        consoleHandler.render(gc, g);
        //bulletin.render(gc, g);
    }

    private Color[] getGradientColour(int timeSec) {
        Color[] colourTable = new Color[2];

        int gradMapWidth = GRADIENT_IMAGE.getWidth();
        int phase = Math.round((timeSec / WorldTime.DAY_LENGTH) * gradMapWidth);

        //update in every 60 frames
        colourTable[0] = GRADIENT_IMAGE.getColor(phase, 0);
        colourTable[1] = GRADIENT_IMAGE.getColor(phase, 1);

        return colourTable;
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

    @Override
    public int getID() {
        return Terrarum.SCENE_ID_GAME;
    }

    private void drawSkybox(Graphics g) {
        Color[] colourTable = getGradientColour(WorldTime.elapsedSeconds());
        GradientFill skyColourFill = new GradientFill(0, 0, colourTable[0], 0, Terrarum.HEIGHT, colourTable[1]);
        g.fill(skyBox, skyColourFill);
    }
}
