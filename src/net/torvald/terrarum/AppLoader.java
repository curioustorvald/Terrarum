package net.torvald.terrarum;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.strikerx3.jxinput.XInputDevice;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.torvald.gdx.graphics.PixmapIO2;
import net.torvald.getcpuname.GetCpuName;
import net.torvald.terrarum.concurrent.ThreadParallel;
import net.torvald.terrarum.controller.GdxControllerAdapter;
import net.torvald.terrarum.controller.TerrarumController;
import net.torvald.terrarum.controller.XinputControllerAdapter;
import net.torvald.terrarum.gamecontroller.KeyToggler;
import net.torvald.terrarum.gameworld.GameWorld;
import net.torvald.terrarum.imagefont.TinyAlphNum;
import net.torvald.terrarum.modulebasegame.IngameRenderer;
import net.torvald.terrarum.modulebasegame.TerrarumIngame;
import net.torvald.terrarum.utils.JsonFetcher;
import net.torvald.terrarum.utils.JsonWriter;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack;
import net.torvald.util.ArrayListMap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static net.torvald.terrarum.TerrarumKt.gdxClearAndSetBlend;

/**
 * The framework's Application Loader
 *
 *
 * Created by minjaesong on 2017-08-01.
 */
public class AppLoader implements ApplicationListener {

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2019 Torvald (minjaesong)";

    /**
     * <p>
     * Version numbering that follows Semantic Versioning 2.0.0 (https://semver.org/)
     * </p>
     *
     * <p>
     * 0xAA_BB_XXXX, where:
     * </p>
     * <li>AA: Major version</li>
     * <li>BB: Minor version</li>
     * <li>XXXX: Patch version</li>
     * <p>
     * e.g. 0x02010034 will be translated as 2.1.52
     * </p>
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
    public AppLoader(LwjglApplicationConfiguration appConfig, Screen injectScreen) {
        AppLoader.injectScreen = injectScreen;
        AppLoader.appConfig = appConfig;
    }

    /**
     * Initialise the application with default game screen
     *
     * @param appConfig LWJGL(2) Application Configuration
     */
    public AppLoader(LwjglApplicationConfiguration appConfig) {
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
     * This function exists because the limitation in the Java language and the design of the GDX itself, where
     * not everything (more like not every method) can be static.
     *
     * @return
     */
    public static AppLoader getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new AppLoader();
        }
        return INSTANCE;
    }

    public static String GAME_LOCALE = System.getProperty("user.language") + System.getProperty("user.country");

    public static final String systemArch = System.getProperty("os.arch");
    public static String processor = "(a super-duper virtual processor)";
    public static String processorVendor = "(andromeda software development)"; // definitely not taken from "that" demogroup
    public static String renderer = "(a super-fancy virtual photoradiator)";
    public static String rendererVendor = "(radiosity)";

    public static int THREADS = ThreadParallel.INSTANCE.getThreadCount();
    public static boolean MULTITHREAD;

    public static final boolean is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64");

    public static int GL_VERSION;
    public static final int MINIMAL_GL_VERSION = 320;

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

    public static LwjglApplicationConfiguration appConfig;
    public static GameFontBase fontGame;
    public static TinyAlphNum fontSmallNumbers;

    /** A gamepad. Multiple gamepads may controll this single virtualised gamepad. */
    public static TerrarumController gamepad = null;
    public static float gamepadDeadzone = 0.3f;


    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    public static int GLOBAL_RENDER_TIMER = new Random().nextInt(1020) + 1;


    public static ArrayListMap debugTimers = new ArrayListMap<String, Long>();

    public static final int defaultW = 1110;
    public static final int defaultH = 740;
    public static final int minimumW = 1080;
    public static final int minimumH = 720;



    private static ShaderProgram shaderBayerSkyboxFill;
    public static ShaderProgram shaderHicolour;
    public static ShaderProgram shaderPassthruRGB;
    public static ShaderProgram shaderColLUT;

    public static Mesh fullscreenQuad;
    private static OrthographicCamera camera;
    private static SpriteBatch logoBatch;
    public static TextureRegion logo;
    public static AudioDevice audioDevice;

    public static SpriteBatch batch;
    public static ShapeRenderer shapeRender;

    private static com.badlogic.gdx.graphics.Color gradWhiteTop = new com.badlogic.gdx.graphics.Color(0xf8f8f8ff);
    private static com.badlogic.gdx.graphics.Color gradWhiteBottom = new com.badlogic.gdx.graphics.Color(0xd8d8d8ff);

    private static Screen currenScreen;
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

    private static float loadTimer = 0f;
    private static final float showupTime = 100f / 1000f;

    private static FrameBuffer renderFBO;

    public static CommonResourcePool resourcePool;
    public static HashSet<File> tempFilePool = new HashSet();
    public static HashSet<Disposable> disposableSingletonsPool = new HashSet();

    public static char gamepadLabelStart = 0xE000; // lateinit
    public static char gamepadLabelSelect = 0xE000; // lateinit
    public static char gamepadLabelEast = 0xE000; // lateinit
    public static char gamepadLabelSouth = 0xE000; // lateinit
    public static char gamepadLabelNorth = 0xE000; // lateinit
    public static char gamepadLabelWest = 0xE000; // lateinit
    public static char gamepadLabelLB = 0xE000; // lateinit
    public static char gamepadLabelRB = 0xE000; // lateinit
    public static char gamepadLabelLT = 0xE000; // lateinit
    public static char gamepadLabelRT = 0xE000; // lateinit
    public static char gamepadLabelLEFT = 0xE068;
    public static char gamepadLabelDOWN = 0xE069;
    public static char gamepadLabelUP = 0xE06A;
    public static char gamepadLabelRIGHT = 0xE06B;
    public static char gamepadLabelUPDOWN = 0xE072;
    public static char gamepadLabelLEFTRIGHT = 0xE071;
    public static char gamepadLabelDPAD = 0xE070;
    public static char gamepadLabelLStick = 0xE044;
    public static char gamepadLabelRStick = 0xE045;
    public static char gamepadLabelLStickPush = 0xE046;
    public static char gamepadLabelRStickPush = 0xE047;
    
    public static void main(String[] args) {
        // load configs
        getDefaultDirectory();
        createDirs();
        readConfigJson();

        setGamepadButtonLabels();


        try { processor = GetCpuName.getModelName(); }
        catch (IOException e1) { processor = "Unknown CPU"; }
        try { processorVendor = GetCpuName.getCPUID(); }
        catch (IOException e2) { processorVendor = "Unknown CPU"; }


        ShaderProgram.pedantic = false;

        LwjglApplicationConfiguration appConfig = new LwjglApplicationConfiguration();
        appConfig.useGL30 = true; // utilising some GL trickeries, need this to be TRUE
        appConfig.vSyncEnabled = getConfigBoolean("usevsync");
        appConfig.resizable = false;//true;
        //appConfig.width = 1110; // photographic ratio (1.5:1)
        //appConfig.height = 740; // photographic ratio (1.5:1)
        appConfig.width = getConfigInt("screenwidth");
        appConfig.height = getConfigInt("screenheight");
        appConfig.backgroundFPS = getConfigInt("displayfps");
        appConfig.foregroundFPS = getConfigInt("displayfps");
        appConfig.title = GAME_NAME;
        appConfig.forceExit = false;
        appConfig.samples = 4; // force the AA on, if the graphics driver didn't do already

        // load app icon
        int[] appIconSizes = new int[]{256,128,64,32,16};
        for (int size : appIconSizes) {
            String name = "assets/appicon" + size + ".png";
            if (new File("./" + name).exists()) {
                appConfig.addIcon(name, Files.FileType.Internal);
            }
        }

        if (args.length == 1 && args[0].equals("isdev=true")) {
            IS_DEVELOPMENT_BUILD = true;
            // safe area box
            //KeyToggler.INSTANCE.forceSet(Input.Keys.F11, true);
        }

        // set some more configuration vars
        MULTITHREAD = ThreadParallel.INSTANCE.getThreadCount() >= 3 && getConfigBoolean("multithread");

        new LwjglApplication(new AppLoader(appConfig), appConfig);
    }
    
    @Override
    public void create() {
        Gdx.graphics.setContinuousRendering(true);

        GAME_LOCALE = getConfigString("language");
        printdbg(this, "locale = " + GAME_LOCALE);

        String glInfo = Gdx.graphics.getGLVersion().getDebugVersionString();

        GL_VERSION = Gdx.graphics.getGLVersion().getMajorVersion() * 100 +
                     Gdx.graphics.getGLVersion().getMinorVersion() * 10 +
                     Gdx.graphics.getGLVersion().getReleaseVersion();

        System.out.println("GL_VERSION = " + GL_VERSION);
        System.out.println("GL info:\n" + glInfo); // debug info


        if (GL_VERSION < MINIMAL_GL_VERSION) {
            // TODO notify properly
            throw new GdxRuntimeException("Graphics device not capable -- device's GL_VERSION: " + GL_VERSION +
                    ", required: " + MINIMAL_GL_VERSION);
        }


        resourcePool = CommonResourcePool.INSTANCE;
        resourcePool.addToLoadingList("blockmarkings_common", () -> new TextureRegionPack(Gdx.files.internal("assets/graphics/blocks/block_markings_common.tga"), 16, 16, 0, 0, 0, 0, false));

        newTempFile("wenquanyi.tga"); // temp file required by the font


        // set basis of draw
        logoBatch = new SpriteBatch();
        camera = new OrthographicCamera(((float) appConfig.width), ((float) appConfig.height));

        batch = new SpriteBatch();
        shapeRender = new ShapeRenderer();

        initViewPort(appConfig.width, appConfig.height);

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
        updateFullscreenQuad(appConfig.width, appConfig.height);


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

    @Override
    public void render() {
        Gdx.gl.glDisable(GL20.GL_DITHER);

        if (splashDisplayed && !postInitFired) {
            postInitFired = true;
            postInit();
        }

        AppLoader.setDebugTime("GDX.rawDelta", (long) (Gdx.graphics.getRawDeltaTime() * 1000_000_000f));
        AppLoader.setDebugTime("GDX.smtDelta", (long) (Gdx.graphics.getDeltaTime() * 1000_000_000f));


        FrameBufferManager.begin(renderFBO);
        gdxClearAndSetBlend(.094f, .094f, .094f, 0f);
        setCameraPosition(0, 0);

        // draw splash screen when predefined screen is null
        // because in normal operation, the only time screen == null is when the app is cold-launched
        // you can't have a text drawn here :v
        if (currenScreen == null) {
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
            logoBatch.draw(logo, (appConfig.width - logo.getRegionWidth()) / 2f,
                    (appConfig.height - logo.getRegionHeight()) / 2f
            );
            logoBatch.end();


            loadTimer += Gdx.graphics.getRawDeltaTime();

            if (loadTimer >= showupTime) {
                // hand over the scene control to this single class; Terrarum must call
                // 'AppLoader.getINSTANCE().screen.render(delta)', this is not redundant at all!

                printdbg(this, "!! Force set current screen and ingame instance to TitleScreen !!");

                IngameInstance title = new TitleScreen(batch);
                Terrarum.INSTANCE.setCurrentIngameInstance(title);
                setScreen(title);
            }
        }
        // draw the screen
        else {
            currenScreen.render((float) UPDATE_RATE);
        }

        KeyToggler.INSTANCE.update(currenScreen instanceof TerrarumIngame);

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
                Pixmap p = ScreenUtils.getFrameBufferPixmap(0, 0, appConfig.width, appConfig.height);
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

        if (currenScreen != null) currenScreen.resize(screenW, screenH);


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

        appConfig.width = screenW;
        appConfig.height = screenH;

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


        if (currenScreen != null) {
            currenScreen.hide();
            currenScreen.dispose();
        }

        //IngameRenderer.INSTANCE.dispose();
        //PostProcessor.INSTANCE.dispose();
        //MinimapComposer.INSTANCE.dispose();
        //FloatDrawer.INSTANCE.dispose();


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

        disposableSingletonsPool.forEach(Disposable::dispose);

        ModMgr.INSTANCE.disposeMods();

        GameWorld.Companion.makeNullWorld().dispose();

        Terrarum.INSTANCE.dispose();

        deleteTempfiles();
    }

    @Override
    public void pause() {
        if (currenScreen != null) currenScreen.pause();
    }

    @Override
    public void resume() {
        if (currenScreen != null) currenScreen.resume();
    }

    public static void setScreen(Screen screen) {
        printdbg("[AppLoader-Static]", "Changing screen to " + screen.getClass().getCanonicalName());

        // this whole thing is directtly copied from com.badlogic.gdx.Game

        if (currenScreen != null) {
            printdbg("[AppLoader-Static]", "Screen before change: " + currenScreen.getClass().getCanonicalName());

            currenScreen.hide();
            currenScreen.dispose();
        }
        else {
            printdbg("[AppLoader-Static]", "Screen before change: null");
        }


        currenScreen = screen;

        currenScreen.show();
        currenScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());


        System.gc();

        printdbg("[AppLoader-Static]", "Screen transition complete: " + currenScreen.getClass().getCanonicalName());
    }
    
    /**
     * Init stuffs which needs GL context
     */
    private void postInit() {
        Terrarum.initialise();


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
        else {
            ModMgr.INSTANCE.invoke(); // invoke Module Manager
            AppLoader.resourcePool.loadAll();
            printdbg(this, "all modules loaded successfully");
            IngameRenderer.initialise();
        }


        printdbg(this, "PostInit done");
    }


    private void setCameraPosition(float newX, float newY) {
        camera.position.set((-newX + appConfig.width / 2), (-newY + appConfig.height / 2), 0f); // deliberate integer division
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

    public static void setGamepadButtonLabels() {
        switch (getConfigString("gamepadlabelstyle")) {
            case "nwii"     : gamepadLabelStart = 0xE04B; break; // + mark
            case "logitech" : gamepadLabelStart = 0xE05A; break; // number 10
            case "msxbone"  : gamepadLabelStart = 0xE049; break; // trifold equal sign?
            default         : gamepadLabelStart = 0xE042; break; // |> mark (sonyps, msxb360, generic)
        }

        switch (getConfigString("gamepadlabelstyle")) {
            case "nwii"     : gamepadLabelSelect = 0xE04D; break; // - mark
            case "logitech" : gamepadLabelSelect = 0xE059; break; // number 9
            case "sonyps"   : gamepadLabelSelect = 0xE043; break; // solid rectangle
            case "msxb360"  : gamepadLabelSelect = 0xE041; break; // <| mark
            case "msxbone"  : gamepadLabelSelect = 0xE048; break; // multitask button?
            default         : gamepadLabelSelect = 0xE043; break; // solid rectangle
        }


        switch (getConfigString("gamepadlabelstyle")) {
            case "msxb360": case "msxbone" : {
                gamepadLabelSouth = 0xE061;
                gamepadLabelEast = 0xE062;
                gamepadLabelWest = 0xE078;
                gamepadLabelNorth = 0xE079;
                gamepadLabelLB = 0xE06D;
                gamepadLabelRB = 0xE06E;
                gamepadLabelLT = 0xE06C;
                gamepadLabelRT = 0xE06F;
                break;
            }
            case "nwii": {
                gamepadLabelSouth = 0xE062;
                gamepadLabelEast = 0xE061;
                gamepadLabelWest = 0xE079;
                gamepadLabelNorth = 0xE078;
                gamepadLabelLB = 0xE065;
                gamepadLabelRB = 0xE066;
                gamepadLabelLT = 0xE064;
                gamepadLabelRT = 0xE067;
                break;
            }
            case "sonyps": {
                gamepadLabelSouth = 0xE063;
                gamepadLabelEast = 0xE050;
                gamepadLabelWest = 0xE073;
                gamepadLabelNorth = 0xE074;
                gamepadLabelLB = 0xE07B;
                gamepadLabelRB = 0xE07C;
                gamepadLabelLT = 0xE07A;
                gamepadLabelRT = 0xE07D;
                break;
            }
            case "logitech": {
                gamepadLabelSouth = 0xE052;
                gamepadLabelEast = 0xE053;
                gamepadLabelWest = 0xE051;
                gamepadLabelNorth = 0xE054;
                gamepadLabelLB = 0xE055;
                gamepadLabelRB = 0xE056;
                gamepadLabelLT = 0xE057;
                gamepadLabelRT = 0xE058;
                break;
            }
        }
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
        System.out.println(String.format("java version = %s", System.getProperty("java.version")));
    }

    private static void createDirs() {
        File[] dirs = {new File(defaultSaveDir)};

        for (File it : dirs) {
            if (!it.exists())
                it.mkdirs();
        }

        //dirs.forEach { if (!it.exists()) it.mkdirs() }
    }

    public static File newTempFile(String filename) {
        File tempfile = new File("./tmp_" + filename);
        tempFilePool.add(tempfile);
        return tempfile;
    }

    private static void deleteTempfiles() {
        for (File file : tempFilePool) {
            file.delete();
        }
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