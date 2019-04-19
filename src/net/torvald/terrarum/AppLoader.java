package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.strikerx3.jxinput.XInputDevice;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.torvald.getcpuname.GetCpuName;
import net.torvald.terrarum.blockstats.MinimapComposer;
import net.torvald.terrarum.controller.GdxControllerAdapter;
import net.torvald.terrarum.controller.TerrarumController;
import net.torvald.terrarum.controller.XinputControllerAdapter;
import net.torvald.terrarum.gamecontroller.KeyToggler;
import net.torvald.terrarum.imagefont.TinyAlphNum;
import net.torvald.terrarum.modulebasegame.Ingame;
import net.torvald.terrarum.modulebasegame.IngameRenderer;
import net.torvald.terrarum.utils.JsonFetcher;
import net.torvald.terrarum.utils.JsonWriter;
import net.torvald.terrarum.worlddrawer.BlocksDrawer;
import net.torvald.terrarum.worlddrawer.LightmapRenderer;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack;
import net.torvald.util.ArrayListMap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static net.torvald.terrarum.TerrarumKt.gdxClearAndSetBlend;

/**
 * The framework's Application Loader
 *
 *
 * Created by minjaesong on 2017-08-01.
 */
public class AppLoader implements ApplicationListener {

    /**
     * 0xAA_BB_XXXX
     * AA: Major version
     * BB: Minor version
     * XXXX: Revision (Repository commits, or something arbitrary)
     * <p>
     * e.g. 0x02010034 will be translated as 2.1.52
     */
    public static final int VERSION_RAW = 0x00_02_0590;

    public static final String getVERSION_STRING() {
        return String.format("%d.%d.%d", VERSION_RAW >>> 24, (VERSION_RAW & 0xff0000) >>> 16, VERSION_RAW & 0xFFFF);
    }

    /**
     * when FALSE, some assertion and print code will not execute
     */
    public static boolean IS_DEVELOPMENT_BUILD = false;


    /**
     * Singleton instance
     */
    private static AppLoader INSTANCE = null;

    /**
     * Screen injected at init, so that you run THAT screen instead of the main game.
     */
    private static Screen injectScreen = null;

    /**
     * Initialise the application with the alternative Screen you choose
     *
     * @param appConfig    LWJGL(2) Application Configuration
     * @param injectScreen GDX Screen you want to run
     */
    public AppLoader(Lwjgl3ApplicationConfiguration appConfig, Screen injectScreen, int width, int height) {
        AppLoader.injectScreen = injectScreen;
        AppLoader.appConfig = appConfig;
        setWindowWidth = width;
        setWindowHeight = height;
    }

    /**
     * Initialise the application with default game screen
     *
     * @param appConfig LWJGL(2) Application Configuration
     */
    public AppLoader(Lwjgl3ApplicationConfiguration appConfig) {
        AppLoader.appConfig = appConfig;
    }

    /**
     * Default null constructor. Don't use it.
     */
    private AppLoader() {
    }

    /**
     * Singleton pattern implementation in Java.
     *
     * @return
     */
    public static AppLoader getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new AppLoader();
        }
        return INSTANCE;
    }

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2019 Torvald (minjaesong)";
    public static String GAME_LOCALE = System.getProperty("user.language") + System.getProperty("user.country");

    public static final String systemArch = System.getProperty("os.arch");
    public static String processor;
    public static String processorVendor;
    public static String renderer;
    public static String rendererVendor;

    public static final boolean is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64");


    public static final float TV_SAFE_GRAPHICS = 0.05f; // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)
    public static final float TV_SAFE_ACTION = 0.035f; // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)

    public static int getTvSafeGraphicsWidth() { return Math.round(screenW * TV_SAFE_GRAPHICS); }
    public static int getTvSafeGraphicsHeight() { return Math.round(screenH * TV_SAFE_GRAPHICS); }
    public static int getTvSafeActionWidth() { return Math.round(screenW * TV_SAFE_ACTION); }
    public static int getTvSafeActionHeight() { return Math.round(screenH * TV_SAFE_ACTION); }

    /**
     * These languages won't distinguish regional differences (e.g. enUS and enUK, frFR and frCA)
     */
    private static final String[] localeSimple = {"de", "en", "es", "it"}; // must be sorted!!

    public static String getSysLang() {
        String lan = System.getProperty("user.language");
        String country = System.getProperty("user.country");
        return lan + country;
    }

    public static void setGAME_LOCALE(String value) {
        if (value.isEmpty() || value.equals("")) {
            GAME_LOCALE = getSysLang();
        }
        else {
            try {
                if (Arrays.binarySearch(localeSimple, value.substring(0, 2)) >= 0) {
                    GAME_LOCALE = value.substring(0, 2);
                }
                else {
                    GAME_LOCALE = value;
                }
            }
            catch (StringIndexOutOfBoundsException e) {
                GAME_LOCALE = value;
            }
        }
    }

    private static boolean splashDisplayed = false;
    private static boolean postInitFired = false;
    private static boolean screenshotRequested = false;
    private static boolean resizeRequested = false;
    private static Point2i resizeReqSize;

    public static Lwjgl3ApplicationConfiguration appConfig;
    public static GameFontBase fontGame;
    public static TinyAlphNum fontSmallNumbers;

    /** A gamepad. Multiple gamepads may controll this single virtualised gamepad. */
    public static TerrarumController gamepad = null;
    public static float gamepadDeadzone = 0.2f;


    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    public static int GLOBAL_RENDER_TIMER = new Random().nextInt(1020) + 1;


    public static ArrayListMap debugTimers = new ArrayListMap<String, Long>();

    public static final int defaultW = 1110;
    public static final int defaultH = 740;
    public static final int minimumW = 1080;
    public static final int minimumH = 720;

    public static int setWindowWidth;
    public static int setWindowHeight;
    
    public static void main(String[] args) {
        // load configs
        getDefaultDirectory();
        createDirs();
        readConfigJson();


        try { processor = GetCpuName.getModelName(); }
        catch (IOException e1) { processor = "Unknown"; }
        try { processorVendor = GetCpuName.getCPUID(); }
        catch (IOException e2) { processorVendor = "Unknown"; }


        ShaderProgram.pedantic = false;

        Lwjgl3ApplicationConfiguration appConfig = new Lwjgl3ApplicationConfiguration();
        appConfig.useOpenGL3(true, 3, 0);// utilising some GL trickeries, need this to be TRUE
        appConfig.setResizable(false);
        appConfig.useVsync(getConfigBoolean("usevsync"));
        //setWindowWidth = 1110; // photographic ratio (1.5:1)
        //setWindowHeight = 740; // photographic ratio (1.5:1)
        setWindowWidth = getConfigInt("screenwidth");
        setWindowHeight = getConfigInt("screenheight");
        appConfig.setWindowedMode(setWindowWidth, setWindowHeight);
        appConfig.setIdleFPS(getConfigInt("displayfps"));
        appConfig.setTitle(GAME_NAME);
        //appConfig.forceExit = false;
        if (IS_DEVELOPMENT_BUILD) {
            appConfig.enableGLDebugOutput(true, System.err);
        }

        // load app icon
        appConfig.setWindowIcon(
                "assets/appicon256.png",
                "assets/appicon128.png",
                "assets/appicon64.png",
                "assets/appicon32.png",
                "assets/appicon16.png"
        );

        if (args.length == 1 && args[0].equals("isdev=true")) {
            IS_DEVELOPMENT_BUILD = true;
            // safe area box
            //KeyToggler.INSTANCE.forceSet(Input.Keys.F11, true);
        }

        new Lwjgl3Application(new AppLoader(appConfig), appConfig);
    }


    private static ShaderProgram shaderBayerSkyboxFill;
    public static ShaderProgram shaderHicolour;
    public static ShaderProgram shaderPassthruRGB;
    public static ShaderProgram shaderColLUT;

    public static Mesh fullscreenQuad;
    private OrthographicCamera camera;
    private SpriteBatch logoBatch;
    public static TextureRegion logo;
    public static AudioDevice audioDevice;

    private Color gradWhiteTop = new Color(0xf8f8f8ff);
    private Color gradWhiteBottom = new Color(0xd8d8d8ff);

    public Screen screen;
    public static int screenW = 0;
    public static int screenH = 0;

    public static Texture textureWhiteSquare;
    public static Texture textureWhiteCircle;

    private void initViewPort(int width, int height) {
        // Set Y to point downwards
        camera.setToOrtho(true, width, height);

        // Update camera matrix
        camera.update();

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height);
    }

    public static final double UPDATE_RATE = 1.0 / 60.0; // TODO set it like 1/100, because apparent framerate is limited by update rate

    private float loadTimer = 0f;
    private final float showupTime = 100f / 1000f;

    private FrameBuffer renderFBO;

    public static CommonResourcePool resourcePool;

    @Override
    public void create() {
        resourcePool = CommonResourcePool.INSTANCE;


        // set basis of draw
        logoBatch = new SpriteBatch();

        camera = new OrthographicCamera(((float) setWindowWidth), ((float) setWindowHeight));

        initViewPort(setWindowWidth, setWindowHeight);

        // logo here :p
        logo = new TextureRegion(new Texture(Gdx.files.internal("assets/graphics/logo_placeholder.tga")));
        logo.flip(false, true);

        // set GL graphics constants
        shaderBayerSkyboxFill = loadShader("assets/4096.vert", "assets/4096_bayer_skyboxfill.frag");
        shaderHicolour = loadShader("assets/4096.vert", "assets/hicolour.frag");
        shaderPassthruRGB = loadShader("assets/4096.vert", "assets/passthrurgb.frag");
        shaderColLUT = loadShader("assets/4096.vert", "assets/passthrurgb.frag");

        fullscreenQuad = new Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        );
        updateFullscreenQuad(setWindowWidth, setWindowHeight);


        // set up renderer info variables
        renderer = Gdx.graphics.getGLVersion().getRendererString();
        rendererVendor = Gdx.graphics.getGLVersion().getVendorString();


        // make gamepad(s)
        if (AppLoader.getConfigBoolean("usexinput")) {
            try {
                gamepad = new XinputControllerAdapter(XInputDevice.getDeviceFor(0));
            }
            catch (Throwable e) {
                gamepad = null;
            }

            // nullify if not actually connected
            try {
                if (!((XinputControllerAdapter) gamepad).getC().isConnected()) {
                    gamepad = null;
                }
            }
            catch (NullPointerException notQuiteWindows) {
                gamepad = null;
            }
        }

        if (gamepad == null) {
            try {
                gamepad = new GdxControllerAdapter(Controllers.getControllers().get(0));
            }
            catch (Throwable e) {
                gamepad = null;
            }

        }

        if (gamepad != null) {
            environment = RunningEnvironment.CONSOLE;

            // calibrate the sticks
            printdbg(this, "Calibrating the gamepad...");
            float[] axesZeroPoints = new float[]{
                    gamepad.getAxisRaw(0),
                    gamepad.getAxisRaw(1),
                    gamepad.getAxisRaw(2),
                    gamepad.getAxisRaw(3)
            };
            setConfig("gamepadaxiszeropoints", axesZeroPoints);
            for (int i = 0; i < 4; i++) {
                printdbg(this, "Axis " + i + ": " + axesZeroPoints[i]);
            }

        }
        else {
            environment = RunningEnvironment.PC;
        }

        // make loading list


    }

    /**
     * @link http://bilgin.esme.org/BitsAndBytes/KalmanFilterforDummies
     */
    private void updateKalmanRenderDelta() {
        // moved to LwjglGraphics
    }

    @Override
    public void render() {
        Gdx.gl.glDisable(GL20.GL_DITHER);

        if (splashDisplayed && !postInitFired) {
            postInitFired = true;
            postInit();
        }

        FrameBufferManager.begin(renderFBO);
        gdxClearAndSetBlend(.094f, .094f, .094f, 0f);
        setCameraPosition(0, 0);

        // draw splash screen when predefined screen is null
        // because in normal operation, the only time screen == null is when the app is cold-launched
        // you can't have a text drawn here :v
        if (screen == null) {
            shaderBayerSkyboxFill.begin();
            shaderBayerSkyboxFill.setUniformMatrix("u_projTrans", camera.combined);
            shaderBayerSkyboxFill.setUniformf("parallax_size", 0f);
            shaderBayerSkyboxFill.setUniformf("topColor", gradWhiteTop.r, gradWhiteTop.g, gradWhiteTop.b);
            shaderBayerSkyboxFill.setUniformf("bottomColor", gradWhiteBottom.r, gradWhiteBottom.g, gradWhiteBottom.b);
            fullscreenQuad.render(shaderBayerSkyboxFill, GL20.GL_TRIANGLES);
            shaderBayerSkyboxFill.end();

            logoBatch.begin();
            logoBatch.setColor(Color.WHITE);
            //blendNormal();
            logoBatch.setShader(null);


            setCameraPosition(0f, 0f);
            logoBatch.draw(logo, (setWindowWidth - logo.getRegionWidth()) / 2f,
                    (setWindowHeight - logo.getRegionHeight()) / 2f
            );
            logoBatch.end();


            loadTimer += Gdx.graphics.getRawDeltaTime();

            if (loadTimer >= showupTime) {
                // hand over the scene control to this single class; Terrarum must call
                // 'AppLoader.getINSTANCE().screen.render(delta)', this is not redundant at all!
                setScreen(Terrarum.INSTANCE);
            }
        }
        // draw the screen
        else {
            screen.render((float) UPDATE_RATE);
        }

        KeyToggler.INSTANCE.update(screen instanceof Ingame);

        // nested FBOs are just not a thing in GL!
        net.torvald.terrarum.FrameBufferManager.end();

        PostProcessor.INSTANCE.draw(camera.combined, renderFBO);


        // process resize request
        if (resizeRequested) {
            resizeRequested = false;
            resize(resizeReqSize.getX(), resizeReqSize.getY());
        }


        // process screenshot request
        if (screenshotRequested) {
            screenshotRequested = false;

            try {
                Pixmap p = ScreenUtils.getFrameBufferPixmap(0, 0, setWindowWidth, setWindowHeight);
                PixmapIO2.writeTGA(Gdx.files.absolute(defaultDir + "/Screenshot.tga"), p, true);
                p.dispose();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }

        splashDisplayed = true;
        GLOBAL_RENDER_TIMER += 1;
    }

    @Override
    public void resize(int width, int height) {
        printdbg(this, "Resize called");
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            printdbg(this, stackTraceElement);
        }

        //initViewPort(width, height);

        screenW = width;
        screenH = height;

        if (screenW % 2 == 1) screenW -= 1;
        if (screenH % 2 == 1) screenH -= 1;

        if (screen != null) screen.resize(screenW, screenH);


        if (renderFBO == null ||
                (renderFBO.getWidth() != screenW ||
                        renderFBO.getHeight() != screenH)
        ) {
            renderFBO = new FrameBuffer(
                    Pixmap.Format.RGBA8888,
                    screenW,
                    screenH,
                    false
            );
        }

        setWindowWidth = screenW;
        setWindowHeight = screenH;

        updateFullscreenQuad(screenW, screenH);

        printdbg(this, "Resize end");
    }

    public static void resizeScreen(int width, int height) {
        resizeRequested = true;
        resizeReqSize = new Point2i(Math.max(width, minimumW), Math.max(height, minimumH));
    }

    @Override
    public void dispose() {
        System.out.println("Goodbye !");


        if (screen != null) {
            screen.hide();
            screen.dispose();
        }

        IngameRenderer.INSTANCE.dispose();
        PostProcessor.INSTANCE.dispose();
        MinimapComposer.INSTANCE.dispose();

        Terrarum.INSTANCE.dispose();

        shaderBayerSkyboxFill.dispose();
        shaderHicolour.dispose();
        shaderPassthruRGB.dispose();
        shaderColLUT.dispose();

        resourcePool.dispose();
        fullscreenQuad.dispose();
        logoBatch.dispose();

        fontGame.dispose();
        fontSmallNumbers.dispose();

        textureWhiteSquare.dispose();
        textureWhiteCircle.dispose();
        logo.getTexture().dispose();

        ModMgr.INSTANCE.disposeMods();

        // delete temp files
        new File("./tmp_wenquanyi.tga").delete(); // FIXME this is pretty much ad-hoc
    }

    @Override
    public void pause() {
        if (screen != null) screen.pause();
    }

    @Override
    public void resume() {
        if (screen != null) screen.resume();
    }

    public void setScreen(Screen screen) {
        printdbg(this, "Changing screen to " + screen.getClass().getCanonicalName());

        // this whole thing is directtly copied from com.badlogic.gdx.Game

        if (this.screen != null) {
            this.screen.hide();
            this.screen.dispose();
        }
        this.screen = screen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        System.gc();

        printdbg(this, "Screen transisiton complete: " + this.screen.getClass().getCanonicalName());
    }

    /**
     * Init stuffs which needs GL context
     */
    private void postInit() {
        textureWhiteSquare = new Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"));
        textureWhiteSquare.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        textureWhiteCircle = new Texture(Gdx.files.internal("assets/graphics/circle_512.tga"));
        textureWhiteCircle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        TextureRegionPack.Companion.setGlobalFlipY(true);
        fontGame = new GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", false, true,
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, false, 256, false
        );
        fontSmallNumbers = TinyAlphNum.INSTANCE;

        try {
            audioDevice = Gdx.audio.newAudioDevice(48000, false);
        }
        catch (NullPointerException deviceInUse) {
            deviceInUse.printStackTrace();
            System.err.println("[AppLoader] failed to create audio device: Audio device occupied by Exclusive Mode Device? (e.g. ASIO4all)");
        }

        // if there is a predefined screen, open that screen after my init process
        if (injectScreen != null) {
            setScreen(injectScreen);
        }


        ModMgr.INSTANCE.invoke(); // invoke Module Manager
        AppLoader.resourcePool.loadAll();
        printdbg(this, "all modules loaded successfully");


        BlocksDrawer.INSTANCE.getWorld(); // will initialize the BlocksDrawer by calling dummy method
        LightmapRenderer.INSTANCE.hdr(0f);


        printdbg(this, "PostInit done");
    }


    private void setCameraPosition(float newX, float newY) {
        camera.position.set((-newX + setWindowWidth / 2), (-newY + setWindowHeight / 2), 0f);
        camera.update();
        logoBatch.setProjectionMatrix(camera.combined);
    }

    private void updateFullscreenQuad(int WIDTH, int HEIGHT) { // NOT y-flipped quads!
        fullscreenQuad.setVertices(new float[]{
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                WIDTH, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                WIDTH, HEIGHT, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, HEIGHT, 0f, 1f, 1f, 1f, 1f, 0f, 0f
        });
        fullscreenQuad.setIndices(new short[]{0, 1, 2, 2, 3, 0});
    }

    public static void requestScreenshot() {
        screenshotRequested = true;
    }

    // DEFAULT DIRECTORIES //

    public static String OSName = System.getProperty("os.name");
    public static String OSVersion = System.getProperty("os.version");
    public static String operationSystem;
    /** %appdata%/Terrarum, without trailing slash */
    public static String defaultDir;
    /** defaultDir + "/Saves", without trailing slash */
    public static String defaultSaveDir;
    /** defaultDir + "/config.json" */
    public static String configDir;
    public static RunningEnvironment environment;

    private static void getDefaultDirectory() {
        String OS = OSName.toUpperCase();
        if (OS.contains("WIN")) {
            operationSystem = "WINDOWS";
            defaultDir = System.getenv("APPDATA") + "/Terrarum";
        }
        else if (OS.contains("OS X")) {
            operationSystem = "OSX";
            defaultDir = System.getProperty("user.home") + "/Library/Application Support/Terrarum";
        }
        else if (OS.contains("NUX") || OS.contains("NIX") || OS.contains("BSD")) {
            operationSystem = "LINUX";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }
        else if (OS.contains("SUNOS")) {
            operationSystem = "SOLARIS";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }
        /*else if (System.getProperty("java.runtime.name").toUpperCase().contains("ANDROID")) {
            operationSystem = "ANDROID";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
            environment = RunningEnvironment.MOBILE;
        }*/
        else {
            operationSystem = "UNKNOWN";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }

        defaultSaveDir = defaultDir + "/Saves";
        configDir = defaultDir + "/config.json";

        System.out.println(String.format("os.name = %s (with identifier %s)", OSName, operationSystem));
        System.out.println(String.format("os.version = %s", OSVersion));
        System.out.println(String.format("default directory: %s", defaultDir));
    }

    private static void createDirs() {
        File[] dirs = {new File(defaultSaveDir)};

        for (File it : dirs) {
            if (!it.exists())
                it.mkdirs();
        }

        //dirs.forEach { if (!it.exists()) it.mkdirs() }
    }


    // CONFIG //

    private static KVHashMap gameConfig = new KVHashMap();

    private static void createConfigJson() throws IOException {
        File configFile = new File(configDir);

        if (!configFile.exists() || configFile.length() == 0L) {
            JsonWriter.INSTANCE.writeToFile(DefaultConfig.INSTANCE.fetch(), configDir);
        }
    }

    /**
     *
     * @return true on successful, false on failure.
     */
    private static Boolean readConfigJson() {
        try {
            // read from disk and build config from it
            JsonObject jsonObject = JsonFetcher.INSTANCE.invoke(configDir);

            // make config
            jsonObject.entrySet().forEach((entry) ->
                    gameConfig.set(entry.getKey(), entry.getValue())
            );

            return true;
        }
        catch (java.nio.file.NoSuchFileException e) {
            // write default config to game dir. Call this method again to read config from it.
            try {
                createConfigJson();
            }
            catch (IOException e1) {
                System.out.println("[AppLoader] Unable to write config.json file");
                e.printStackTrace();
            }

            return false;
        }

    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static int getConfigInt(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonPrimitive)
            return ((JsonPrimitive) cfg).getAsInt();
        else
            return Integer.parseInt(((String) cfg));
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static String getConfigString(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonPrimitive)
            return ((JsonPrimitive) cfg).getAsString();
        else
            return ((String) cfg);
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist. If the default value is undefined, will return false.
     */
    public static boolean getConfigBoolean(String key) {
        try {
            Object cfg = getConfigMaster(key);
            if (cfg instanceof JsonPrimitive)
                return ((JsonPrimitive) cfg).getAsBoolean();
            else
                return ((boolean) cfg);
        }
        catch (NullPointerException keyNotFound) {
            return false;
        }
    }

    public static int[] getConfigIntArray(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonArray) {
            JsonArray jsonArray = ((JsonArray) cfg).getAsJsonArray();
            //return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
            int[] intArray = new int[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                intArray[i] = jsonArray.get(i).getAsInt();
            }
            return intArray;
        }
        else
            return ((int[]) cfg);
    }

    public static float[] getConfigFloatArray(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonArray) {
            JsonArray jsonArray = ((JsonArray) cfg).getAsJsonArray();
            //return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
            float[] floatArray = new float[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                floatArray[i] = jsonArray.get(i).getAsInt();
            }
            return floatArray;
        }
        else
            return ((float[]) cfg);
    }

    /**
     * Get config from config file. If the entry does not exist, get from defaults; if the entry is not in the default, NullPointerException will be thrown
     */
    private static JsonObject getDefaultConfig() {
        return DefaultConfig.INSTANCE.fetch();
    }

    private static Object getConfigMaster(String key1) {
        String key = key1.toLowerCase();

        Object config;
        try {
            config = gameConfig.get(key);
        }
        catch (NullPointerException e) {
            config = null;
        }

        Object defaults;
        try {
            defaults = getDefaultConfig().get(key);
        }
        catch (NullPointerException e) {
            defaults = null;
        }

        if (config == null) {
            if (defaults == null) {
                throw new NullPointerException("key not found: '" + key + "'");
            }
            else {
                return defaults;
            }
        }
        else {
            return config;
        }
    }

    public static void setConfig(String key, Object value) {
        gameConfig.set(key.toLowerCase(), value);
    }



    // //

    public static void printdbg(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            System.out.println("[" + obj.getClass().getSimpleName() + "] " + message.toString());
        }
    }

    public static void printdbgerr(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            System.err.println("[" + obj.getClass().getSimpleName() + "] " + message.toString());
        }
    }

    public static void printmsg(Object obj, Object message) {
        System.out.println("[" + obj.getClass().getSimpleName() + "] " + message.toString());
    }

    public static ShaderProgram loadShader(String vert, String frag) {
        ShaderProgram s = new ShaderProgram(Gdx.files.internal(vert), Gdx.files.internal(frag));

        if (s.getLog().toLowerCase().contains("error")) {
            throw new Error(String.format("Shader program loaded with %s, %s failed:\n%s", vert, frag, s.getLog()));
        }

        return s;
    }

    public static void measureDebugTime(String name, kotlin.jvm.functions.Function0<kotlin.Unit> block) {
        if (IS_DEVELOPMENT_BUILD) {
            //debugTimers.put(name, kotlin.system.TimingKt.measureNanoTime(block));

            long start = System.nanoTime();
            block.invoke();
            debugTimers.put(name, System.nanoTime() - start);
        }
    }

    public static void setDebugTime(String name, long value) {
        if (IS_DEVELOPMENT_BUILD) {
            debugTimers.put(name, value);
        }
    }

    public static void addDebugTime(String target, String... targets) {
        if (IS_DEVELOPMENT_BUILD) {
            long l = 0L;
            for (String s : targets) {
                l += ((long) debugTimers.get(s));
            }
            debugTimers.put(target, l);
        }
    }
}