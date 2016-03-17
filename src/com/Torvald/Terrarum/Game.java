package com.Torvald.Terrarum;

import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.Actors.*;
import com.Torvald.Terrarum.ConsoleCommand.Authenticator;
import com.Torvald.Terrarum.ConsoleCommand.CommandDict;
import com.Torvald.Terrarum.GameControl.GameController;
import com.Torvald.Terrarum.GameControl.KeyMap;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.GameMap.WorldTime;
import com.Torvald.Terrarum.MapDrawer.LightmapRenderer;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.MapGenerator.MapGenerator;
import com.Torvald.Terrarum.MapGenerator.RoguelikeRandomiser;
import com.Torvald.Terrarum.TileProperties.TilePropCodex;
import com.Torvald.Terrarum.TileStat.TileStat;
import com.Torvald.Terrarum.UserInterface.*;
import com.jme3.math.FastMath;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.*;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.fills.GradientFill;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import shader.Shader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.HashSet;

/**
 * Created by minjaesong on 15-12-30.
 */
public class Game extends BasicGameState {

    public static long memInUse;
    public static long totalVMMem;
    int game_mode = 0;

    public GameMap map;

    public HashSet<Actor> actorContainer = new HashSet<>();
    public HashSet<UIHandler> uiContainer = new HashSet<>();

    public UIHandler consoleHandler;
    public UIHandler debugWindow;
    public UIHandler notifinator;

    @NotNull Player player;

    private Image GRADIENT_IMAGE;
    private Rectangle skyBox;

    public float screenZoom = 1.0f;
    public final float ZOOM_MAX = 2.0f;
    public final float ZOOM_MIN = 0.25f;

    private Shader shader12BitCol;
    private Shader shaderBlurH;
    private Shader shaderBlurV;

    public static Authenticator auth = new Authenticator();

    public Game() throws SlickException {  }


    private boolean useShader;
    private int shaderProgram = 0;


    private final int ENV_COLTEMP_SUNRISE = 2500;
    private final int ENV_SUNLIGHT_DELTA = MapDrawer.getENV_COLTEMP_NOON() - ENV_COLTEMP_SUNRISE;


    @Override
    public void init(GameContainer gameContainer, StateBasedGame stateBasedGame) throws
            SlickException {
        new GameController();
        KeyMap.build();
        GameController.setKeyMap(new KeyMap());


        shader12BitCol = Shader.makeShader("./res/4096.vrt", "./res/4096.frg");
        shaderBlurH = Shader.makeShader("./res/blurH.vrt", "./res/blur.frg");
        shaderBlurV = Shader.makeShader("./res/blurV.vrt", "./res/blur.frg");


        GRADIENT_IMAGE = new Image("res/graphics/sky_colour.png");
        skyBox = new Rectangle(0, 0, Terrarum.WIDTH, Terrarum.HEIGHT);

        new WorldTime();
        new TilePropCodex();
        // new ItemPropCodex() -- This is kotlin object and already initialised.

        map = new GameMap(8192, 2048);
        map.setGravitation(9.8f);

        MapGenerator.attachMap(map);
        MapGenerator.setSeed(0x51621D);
        //MapGenerator.setSeed(new HQRNG().nextLong());
        MapGenerator.generateMap();

        RoguelikeRandomiser.setSeed(0x540198);
        //RoguelikeRandomiser.setSeed(new HQRNG().nextLong());


        new CommandDict();

        // add new player and put it to actorContainer
        //player = new Player();
        player = PFSigrid.build();
        //player.setNoClip(true);
        actorContainer.add(player);

        consoleHandler = new UIHandler(new ConsoleWindow());
        consoleHandler.setPosition(0, 0);

        debugWindow = new UIHandler(new BasicDebugInfoWindow());
        debugWindow.setPosition(0, 0);

        notifinator = new UIHandler(new Notification());
        notifinator.setPosition(
                (Terrarum.WIDTH - notifinator.getUI().getWidth())
                        / 2
                , Terrarum.HEIGHT - notifinator.getUI().getHeight()
        );
        notifinator.setVisibility(true);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void update(GameContainer gc, StateBasedGame sbg, int delta_t) {
        setAppTitle();

        // GL at after_sunrise-noon_before_sunset
        //map.setGlobalLight();

        GameController.processInput(gc.getInput());

        TileStat.update();

        MapDrawer.update(gc, delta_t);
        MapCamera.update(gc, delta_t);

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

        notifinator.update(gc, delta_t);

        Terrarum.appgc.setVSync(Terrarum.appgc.getFPS() >= Terrarum.VSYNC_TRIGGER_THRESHOLD);
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

        drawSkybox(g);

        // compensate for zoom. UIs have to be treated specially! (see UIHandler)
        g.translate(
                  -MapCamera.getCameraX() * screenZoom
                , -MapCamera.getCameraY() * screenZoom
        );

        MapCamera.renderBehind(gc, g);

        actorContainer.forEach(
                actor -> { if (actor instanceof Visible) ((Visible) actor).drawBody(gc, g); }
        );
        actorContainer.forEach(
                actor -> { if (actor instanceof Glowing) ((Glowing) actor).drawGlow(gc, g); }
        );

        LightmapRenderer.renderLightMap();

        MapCamera.renderFront(gc, g);
        MapDrawer.render(gc, g);

        setBlendModeMul();
        MapDrawer.drawEnvOverlay(g);
        LightmapRenderer.draw(g);
        setBlendModeNormal();

        uiContainer.forEach(ui -> ui.render(gc, g));
        debugWindow.render(gc, g);
        consoleHandler.render(gc, g);
        notifinator.render(gc, g);
    }

    public boolean addActor(Actor e) {
        return actorContainer.add(e);
    }

    public boolean removeActor(Actor e) {
        return actorContainer.remove(e);
    }

    private Color[] getGradientColour(int timeSec) {
        Color[] colourTable = new Color[2];

        int gradMapWidth = GRADIENT_IMAGE.getWidth();
        int phase = Math.round((timeSec / WorldTime.DAY_LENGTH) * gradMapWidth);

        //update in every INTERNAL_FRAME frames
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
        Color[] colourTable = getGradientColour(Terrarum.game.map.getWorldTime().elapsedSeconds());
        GradientFill skyColourFill = new GradientFill(0, 0, colourTable[0], 0, Terrarum.HEIGHT, colourTable[1]);
        g.fill(skyBox, skyColourFill);
    }

    private void setBlendModeMul() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void setBlendModeNormal() {
        GL11.glDisable(GL11.GL_BLEND);
        Terrarum.appgc.getGraphics().setDrawMode(Graphics.MODE_NORMAL);
    }

    public void sendNotification(String[] msg) {
        ((Notification) notifinator.getUI()).sendNotification(msg);
    }

    private int createShader(String filename, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

            if(shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename));
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception exc) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw exc;
        }
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    private String readFileAsString(String filename) throws Exception {
        StringBuilder source = new StringBuilder();

        FileInputStream in = new FileInputStream(filename);

        Exception exception = null;

        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));

            Exception innerExc= null;
            try {
                String line;
                while((line = reader.readLine()) != null)
                    source.append(line).append('\n');
            }
            catch(Exception exc) {
                exception = exc;
            }
            finally {
                try {
                    reader.close();
                }
                catch(Exception exc) {
                    if(innerExc == null)
                        innerExc = exc;
                    else
                        exc.printStackTrace();
                }
            }

            if(innerExc != null)
                throw innerExc;
        }
        catch(Exception exc) {
            exception = exc;
        }
        finally {
            try {
                in.close();
            }
            catch(Exception exc) {
                if(exception == null)
                    exception = exc;
                else
                    exc.printStackTrace();
            }

            if(exception != null)
                throw exception;
        }

        return source.toString();
    }

    public long getMemInUse() {
        return memInUse;
    }

    public long getTotalVMMem() {
        return totalVMMem;
    }

    private int getSunlightColtemp() {
        int half_today = WorldTime.DAY_LENGTH / 2;
        int timeToday = WorldTime.elapsedSeconds();
        float sunAlt = (timeToday < half_today) ?
                        timeToday / half_today * FastMath.PI
                       : 0f;
        return Math.round(ENV_COLTEMP_SUNRISE + (ENV_SUNLIGHT_DELTA * FastMath.sin(sunAlt)));
    }
}
