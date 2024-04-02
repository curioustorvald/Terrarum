package net.torvald.terrarum;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.TerrarumLwjgl3Application;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonValue;
import com.github.strikerx3.jxinput.XInputDevice;
import kotlin.jvm.functions.Function0;
import kotlin.text.Charsets;
import net.torvald.getcpuname.GetCpuName;
import net.torvald.terrarum.audio.AudioMixer;
import net.torvald.terrarum.audio.MusicContainer;
import net.torvald.terrarum.audio.dsp.BinoPan;
import net.torvald.terrarum.controller.GdxControllerAdapter;
import net.torvald.terrarum.controller.TerrarumController;
import net.torvald.terrarum.controller.XinputControllerAdapter;
import net.torvald.terrarum.gameactors.BlockMarkerActor;
import net.torvald.terrarum.gamecontroller.IME;
import net.torvald.terrarum.gamecontroller.InputStrober;
import net.torvald.terrarum.gamecontroller.KeyToggler;
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent;
import net.torvald.terrarum.gameitems.GameItem;
import net.torvald.terrarum.gameworld.GameWorld;
import net.torvald.terrarum.imagefont.BigAlphNum;
import net.torvald.terrarum.imagefont.TinyAlphNum;
import net.torvald.terrarum.langpack.Lang;
import net.torvald.terrarum.modulebasegame.IngameRenderer;
import net.torvald.terrarum.modulebasegame.TerrarumIngame;
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory;
import net.torvald.terrarum.serialise.WriteConfig;
import net.torvald.terrarum.ui.Toolkit;
import net.torvald.terrarum.utils.JsonFetcher;
import net.torvald.terrarum.worlddrawer.CreateTileAtlas;
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap;
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack;
import net.torvald.unsafe.AddressOverflowException;
import net.torvald.unsafe.DanglingPointerException;
import net.torvald.unsafe.UnsafeHelper;
import net.torvald.util.DebugTimers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.Thread.MAX_PRIORITY;
import static net.torvald.terrarum.TerrarumKt.*;

/**
 * The framework's Application Loader
 *
 *
 * Created by minjaesong on 2017-08-01.
 */
public class App implements ApplicationListener {

    public static final long startupTime = System.currentTimeMillis() / 1000L;

    public static final String GAME_NAME = TerrarumAppConfiguration.GAME_NAME;
    public static final long VERSION_RAW = TerrarumAppConfiguration.VERSION_RAW;
    public static final String VERSION_TAG = TerrarumAppConfiguration.VERSION_TAG;

    public static final String getVERSION_STRING() {
        return TerrarumAppConfiguration.INSTANCE.getVERSION_STRING();
    }

    public static final String getVERSION_STRING_WITHOUT_SNAPSHOT() {
        return TerrarumAppConfiguration.INSTANCE.getVERSION_STRING_WITHOUT_SNAPSHOT();
    }

    /**
     * when FALSE, some assertion and print code will not execute
     */
    public static boolean IS_DEVELOPMENT_BUILD = false;

    /**
     * Singleton instance
     */
    private static App INSTANCE = null;

    /**
     * Screen injected at init, so that you run THAT screen instead of the main game.
     */
    private static Screen injectScreen = null;

    /**
     * Initialise the application with the alternative Screen you choose
     *
     * @param appConfig    LWJGL3 Application Configuration
     * @param injectScreen GDX Screen you want to run
     */
    public App(Lwjgl3ApplicationConfiguration appConfig, Screen injectScreen) {
        App.injectScreen = injectScreen;
        App.appConfig = appConfig;
    }

    /**
     * Initialise the application with default game screen
     *
     * @param appConfig LWJGL3 Application Configuration
     */
    public App(Lwjgl3ApplicationConfiguration appConfig) {
        App.appConfig = appConfig;
    }

    /**
     * Default null constructor. Don't use it.
     */
    private App() {
    }

    /**
     * Singleton pattern implementation in Java.
     *
     * This function exists because the limitation in the Java language and the design of the GDX itself, where
     * not everything (more like not every method) can be static.
     *
     * @return
     */
    public static App getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new App();
        }
        return INSTANCE;
    }

    public static String GAME_LOCALE = System.getProperty("user.language") + System.getProperty("user.country");

    public static final String systemArch = System.getProperty("os.arch");
    public static String processor = "(a super-duper virtual processor)";
    public static String processorVendor = "(andromeda software development)"; // definitely not taken from "that" demogroup
    public static String renderer = "(a super-fancy virtual photoradiator)";
    public static String rendererVendor = "(aperture science psychovisualcomputation laboratory)";

    /**
     * True if the processor's name starts with "Apple M" and not running through Rosetta.
     * To detect the presence of the M-chips only, use App.processor.startsWith("Apple M")
     */
    public static boolean isAppleM = false;

    public static int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    public static boolean MULTITHREAD;

    public static final boolean is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64");
    // some JVMs don't have this property, but they probably don't have "sun.misc.Unsafe" either, so it's no big issue \_(ツ)_/

    public static final int GLOBAL_FRAMERATE_LIMIT = 300;

    private static String undesirableConditions;

    public static String getUndesirableConditions() {
        return undesirableConditions;
    }

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
        if (value.isEmpty()) {
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

    private static boolean postInitFired = false;
    private static boolean screenshotRequested = false;
    private static boolean resizeRequested = false;
    private static Point2i resizeReqSize;

    public static Lwjgl3ApplicationConfiguration appConfig;
    public static TerrarumScreenSize scr;
    public static TerrarumGLinfo glInfo = new TerrarumGLinfo();

    public static CreateTileAtlas tileMaker;

    /** Vanilla */
    public static TerrarumSansBitmap fontGame;
    /** Vertically flipped */
    public static TerrarumSansBitmap fontGameFBO;
    /** Big interchar */
    public static TerrarumSansBitmap fontUITitle;
    public static TinyAlphNum fontSmallNumbers;
    public static BigAlphNum fontBigNumbers;

    /** A gamepad. Multiple gamepads may controll this single virtualised gamepad. */
    public static TerrarumController gamepad = null;
    public static float gamepadDeadzone = 0.2f;


    /**
     * Sorted by the lastplaytime, in reverse order (index 0 is the most recent game played)
     */
    public static ArrayList<UUID> sortedSavegameWorlds = new ArrayList();
    public static HashMap<UUID, SavegameCollection> savegameWorlds = new HashMap<>(); // UNSORTED even with the TreeMap
    public static HashMap<UUID, String> savegameWorldsName = new HashMap<>();

    public static ArrayList<UUID> sortedPlayers = new ArrayList();
    public static HashMap<UUID, SavegameCollection> savegamePlayers = new HashMap<>();
    public static HashMap<UUID, String> savegamePlayersName = new HashMap<>();

    public static void updateListOfSavegames() {
        AppUpdateListOfSavegames();
    }


    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    public static int GLOBAL_RENDER_TIMER = new Random().nextInt(1020) + 1;


    public static DebugTimers debugTimers = new DebugTimers();


    public static final String FONT_DIR = "assets/graphics/fonts/terrarum-sans-bitmap";



    public static Texture[] ditherPatterns = new Texture[4];
//    public static ShaderProgram shaderHicolour;
    public static ShaderProgram shaderDebugDiff;
    public static ShaderProgram shaderPassthruRGBA;
    public static ShaderProgram shaderColLUT;
    public static ShaderProgram shaderReflect;
    public static ShaderProgram shaderGhastlyWhite;
    public static Hq2x hq2x;

    public static Mesh fullscreenQuad;
    private static OrthographicCamera camera;
    private static FlippingSpriteBatch logoBatch;
    public static TextureRegion splashScreenLogo;
    private static TextureRegion splashBackdrop;
    public static AudioDevice audioDevice;

    public static FlippingSpriteBatch batch;
    public static ShapeRenderer shapeRender;

    private static com.badlogic.gdx.graphics.Color gradWhiteTop = new com.badlogic.gdx.graphics.Color(0xf8f8f8ff);
    private static com.badlogic.gdx.graphics.Color gradWhiteBottom = new com.badlogic.gdx.graphics.Color(0xd8d8d8ff);

    private static TerrarumGamescreen currentScreen;
    private static LoadScreenBase currentSetLoadScreen;

    private void initViewPort(int width, int height) {
        // Set Y to point downwards
        camera.setToOrtho(true, width, height); // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin

        // Update camera matrix
        camera.update();

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height);
    }

    public static final int TICK_SPEED = 60; // using 60 as it's highly composite number
    public static final float UPDATE_RATE = 1f / TICK_SPEED; // apparent framerate will be limited by update rate

    private static float loadTimer = 0f;
    private static final float showupTime = 100f / 1000f;

    private static Float16FrameBuffer renderFBO;

    public static HashSet<File> tempFilePool = new HashSet<>();

    /**
     * <p>If your object is not Disposable, try following code:</p>
     *
     * <code>
     *     App.disposables.add(Disposable { vm.dispose() })
     * </code>
     */
    public static HashSet<Disposable> disposables = new HashSet<>();

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

    public static String[] gamepadWhitelist = {
            "xinput", "xbox", "game", "joy", "pad"
    };

    public static InputStrober inputStrober;

    public static long bogoflops = 0L;
    private static double bogoflopf = Math.random();

    public static boolean hasUpdate = true;

    public static Screen getCurrentScreen() {
        return currentScreen;
    }

    public static ShapeRenderer makeShapeRenderer() {
        return new ShapeRenderer(5000, DefaultGL32Shaders.INSTANCE.createShapeRendererShader());
    }

    public static boolean gl40capable = false;

    public static Thread audioManagerThread;

    public static void main(String[] args) {

        long st = System.nanoTime();
        long sc = st;
        while (sc - st < 100000000L) {
            bogoflopf = Math.random() * bogoflopf;
            bogoflops++;
            sc = System.nanoTime();
        }
        bogoflops = Math.round((double)(bogoflops) * (1000000000.0 / (sc - st)));
//        System.out.println(sc - st);
//        System.out.println(bogoflops);

        // if -ea flag is set, turn on all the debug prints
        try {
            assert false;
        }
        catch (AssertionError e) {
            IS_DEVELOPMENT_BUILD = true;
        }


        // print copyright message
        System.out.println(csiB+GAME_NAME+" "+csiG+getVERSION_STRING()+" "+csiK+"\u2014"+" "+csi0+TerrarumAppConfiguration.COPYRIGHT_DATE_NAME);
        System.out.println(csiG+TerrarumAppConfiguration.COPYRIGHT_LICENSE_TERMS_SHORT+csi0);
        System.out.println("IS_DEVELOPMENT_BUILD = " + IS_DEVELOPMENT_BUILD);

        try {

            try {
                processor = GetCpuName.getModelName();
            }
            catch (IOException e1) {
                processor = "Unknown CPU";
            }
            try {
                processorVendor = GetCpuName.getCPUID();
            }
            catch (IOException e2) {
                processorVendor = "Unknown CPU";
            }

            if (processor != null && processor.startsWith("Apple M") && systemArch.equals("aarch64")) {
                isAppleM = true;
                System.out.println("Apple Proprietary "+processor+" detected; don't expect smooth sailing...");
            }
            if (processor != null && processor.startsWith("Apple M") && !systemArch.equals("aarch64")) {
                undesirableConditions = "apple_execution_through_rosetta";
            }

            if (processor == null) {
                processor = "null";
            }

            if (!IS_DEVELOPMENT_BUILD) {
                var p = UnsafeHelper.INSTANCE.allocate(64);
                p.destroy();
                try {
                    p.get(0);
                }
                catch (DanglingPointerException | AddressOverflowException e) {
                    throw new RuntimeException("Build Error: App is not Development Build but pointer check is still installed. If the game is a production release, please report this to the developers.");
                }
            }


            // load configs
            getDefaultDirectory();
            createDirs();
            initialiseConfig();
            readConfigJson();
            setGamepadButtonLabels();
            rectifyConfigs();


            ShaderProgram.pedantic = false;

            scr = new TerrarumScreenSize(getConfigInt("screenwidth"), getConfigInt("screenheight"));
            int width = scr.getWindowW();
            int height = scr.getWindowH();
            boolean useFullscreen = getConfigBoolean("fullscreen");
            float magn = (float) getConfigDouble("screenmagnifying");

            Lwjgl3ApplicationConfiguration appConfig = new Lwjgl3ApplicationConfiguration();
            //appConfig.useGL30 = false; // https://stackoverflow.com/questions/46753218/libgdx-should-i-use-gl30
//            if (SharedLibraryLoader.isMac)
                appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
            appConfig.useVsync(getConfigBoolean("usevsync"));
            appConfig.setResizable(false);

            if (useFullscreen) {
                // auto resize for fullscreen
                var disp = Lwjgl3ApplicationConfiguration.getDisplayMode(Lwjgl3ApplicationConfiguration.getPrimaryMonitor());
                var newWidth = ((int)(disp.width / magn)) & 0x7FFFFFFE;
                var newHeight = ((int)(disp.height / magn)) & 0x7FFFFFFE;
                scr.setDimension(newWidth, newHeight, magn);

                appConfig.setFullscreenMode(disp);

            }
            else
                appConfig.setWindowedMode(width, height);

            // if filter is none AND magn is not (100% or 200%), set filter to hq2x
            if (getConfigString("screenmagnifyingfilter").equals("none") && magn != 1f && magn != 2f) {
                setConfig("screenmagnifyingfilter", "hq2x");
            }

            appConfig.setTransparentFramebuffer(false);
            int fpsActive = Math.min(GLOBAL_FRAMERATE_LIMIT, getConfigInt("displayfps"));
            if (fpsActive <= 0) fpsActive = GLOBAL_FRAMERATE_LIMIT;
            int fpsBack = Math.min(GLOBAL_FRAMERATE_LIMIT, getConfigInt("displayfpsidle"));
            if (fpsBack <= 0) fpsBack = GLOBAL_FRAMERATE_LIMIT;
            appConfig.setIdleFPS(fpsBack);
            appConfig.setForegroundFPS(fpsActive);
            appConfig.setTitle(GAME_NAME);
            //appConfig.forceExit = true; // it seems KDE 5 likes this one better...
            // (Plasma freezes upon app exit. with forceExit = true, it's only frozen for a minute; with forceExit = false, it's indefinite)
            //appConfig.samples = 4; // force the AA on, if the graphics driver didn't do already

            // load app icon
            appConfig.setWindowIcon(Files.FileType.Classpath,
                    "res/appicon512.png",
                    "res/appicon256.png",
                    "res/appicon144.png",
                    "res/appicon128.png",
                    "res/appicon96.png",
                    "res/appicon64.png",
                    "res/appicon48.png",
                    "res/appicon32.png",
                    "res/appicon16.png"
            );

            // set some more configuration vars
            MULTITHREAD = THREAD_COUNT >= 3;

            new TerrarumLwjgl3Application(new App(appConfig), appConfig);
        }
        catch (Throwable e) {
            if (Gdx.app != null) {
                Gdx.app.exit();
            }
            e.printStackTrace();
            new GameCrashHandler(e);
        }
    }

    private static Color splashTextCol = new Color(0x282828FF);

    @Override
    public void create() {
        File loadOrderFile = new File(App.loadOrderDir);
        if (loadOrderFile.exists()) {
            // load modules
            CSVParser loadOrderCSVparser = null;
            try {
                loadOrderCSVparser = CSVParser.parse(
                        loadOrderFile,
                        Charsets.UTF_8,
                        CSVFormat.DEFAULT.withCommentMarker('#')
                );
                var loadOrder = loadOrderCSVparser.getRecords();

                if (loadOrder.size() > 0) {
                    var modname = loadOrder.get(0).get(0);

                    var textureFile = Gdx.files.internal("assets/mods/"+modname+"/splashback.png");
                    if (textureFile.exists()) {
                        splashBackdrop = new TextureRegion(new Texture(textureFile));
                        splashTextCol = new Color(0xeeeeeeff);
                    }

                    var logoFile = Gdx.files.internal("assets/mods/"+modname+"/splashlogo.png");
                    if (logoFile.exists()) {
                        splashScreenLogo = new TextureRegion(new Texture(logoFile));
                    }
                 }
            }
            catch (IOException e) {

            }
            finally {
                try {loadOrderCSVparser.close();}
                catch (IOException e) {}
            }
        }

        if (splashBackdrop == null) {
            splashBackdrop = new TextureRegion(new Texture("assets/graphics/background_white.png"));
        }
        if (splashScreenLogo == null) {
            splashScreenLogo = new TextureRegion(new Texture("assets/graphics/logo.png"));
        }

        Gdx.graphics.setContinuousRendering(true);

        GAME_LOCALE = getConfigString("language");
        printdbg(this, "locale = " + GAME_LOCALE);


        glInfo.create();
        gl40capable = (Gdx.graphics.getGLVersion().getMajorVersion() >= 4);
        printdbg(this, "GL40 capable? "+gl40capable);

        CommonResourcePool.INSTANCE.addToLoadingList("title_health1", () -> new Texture(Gdx.files.internal("./assets/graphics/gui/health_take_a_break.tga")));
        CommonResourcePool.INSTANCE.addToLoadingList("title_health2", () -> new Texture(Gdx.files.internal("./assets/graphics/gui/health_distance.tga")));
        CommonResourcePool.INSTANCE.addToLoadingList("sound:haptic_bop", () -> new MusicContainer("haptic_bop", Gdx.files.internal("./assets/audio/effects/haptic_bop.ogg").file(), false, true, (MusicContainer m) -> { return null; }));
        CommonResourcePool.INSTANCE.addToLoadingList("sound:haptic_bup", () -> new MusicContainer("haptic_bup", Gdx.files.internal("./assets/audio/effects/haptic_bup.ogg").file(), false, true, (MusicContainer m) -> { return null; }));
        CommonResourcePool.INSTANCE.addToLoadingList("sound:haptic_bip", () -> new MusicContainer("haptic_bip", Gdx.files.internal("./assets/audio/effects/haptic_bip.ogg").file(), false, true, (MusicContainer m) -> { highPrioritySoundPlaying = false; return null; }));
        // make loading list
        CommonResourcePool.INSTANCE.loadAll();

        newTempFile("wenquanyi.tga"); // temp file required by the font


        // set basis of draw
        logoBatch = new FlippingSpriteBatch();
        camera = new OrthographicCamera((scr.getWf()), (scr.getHf()));

        batch = new FlippingSpriteBatch();
        shapeRender = makeShapeRenderer();

        initViewPort(scr.getWidth(), scr.getHeight());

        // set GL graphics constants
        for (int i = 0; i < ditherPatterns.length; i++) {
            Texture t = new Texture(Gdx.files.classpath("shaders/dither_512_"+i+".tga"));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Linear);
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            ditherPatterns[i] = t;
        }

        shaderPassthruRGBA = loadShaderFromClasspath("shaders/gl32spritebatch.vert", "shaders/gl32spritebatch.frag");
        shaderReflect = loadShaderFromClasspath("shaders/default.vert", "shaders/reflect.frag");
        hq2x = new Hq2x(2);

        fullscreenQuad = new Mesh(
                true, 4, 4,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        );
        updateFullscreenQuad(fullscreenQuad, scr.getWidth(), scr.getHeight());

        // set up renderer info variables
        renderer = Gdx.graphics.getGLVersion().getRendererString();
        rendererVendor = Gdx.graphics.getGLVersion().getVendorString();


        fontGame = new TerrarumSansBitmap(FONT_DIR, false, false, false,
                false,
                256, false, 0.5f, false
        );
    }

    private FrameBuffer postProcessorOutFBO;
    private FrameBuffer postProcessorOutFBO2;

    private void firePostInit() {
        if (!postInitFired) {
            postInitFired = true;
            postInit();
        }
    }

    @Override
    public void render() {
//        Gdx.gl.glDisable(GL20.GL_DITHER);

        App.setDebugTime("GDX.rawDelta", (long) (Gdx.graphics.getDeltaTime() * 1000_000_000f));


        FrameBufferManager.begin(renderFBO);
        gdxClearAndEnableBlend(1f, 0f, 1f, 1f);
        setCameraPosition(0, 0);


        // draw splash screen when predefined screen is null
        // because in normal operation, the only time screen == null is when the app is cold-launched
        // you can't have a text drawn here :v
        if (currentScreen == null) {
            drawSplash();

            loadTimer += Gdx.graphics.getDeltaTime();

            if (loadTimer >= showupTime) {
                firePostInit();

                // hand over the scene control to this single class; Terrarum must call
                // 'AppLoader.getINSTANCE().screen.render(delta)', this is not redundant at all!

                IngameInstance title = ModMgr.INSTANCE.getTitleScreen(batch);

                if (title != null) {
                    Terrarum.INSTANCE.setCurrentIngameInstance(title);
                    setScreen(title);
                }
                else {
                    IngameInstance notitle = new NoModuleDefaultTitlescreen(batch);
                    setScreen(notitle);
                }
            }

            postProcessorOutFBO = renderFBO;
        }
        // draw the screen
        else {
            firePostInit();

            currentScreen.render(UPDATE_RATE);
            postProcessorOutFBO = TerrarumPostProcessor.INSTANCE.draw(Gdx.graphics.getDeltaTime(), camera.combined, renderFBO);
        }


        KeyToggler.INSTANCE.update(currentScreen instanceof TerrarumIngame);



        // nested FBOs are just not a thing in GL!
        FrameBufferManager.end();


        // process screenshot request
        processScreenshotRequest(postProcessorOutFBO);



        if (getConfigString("screenmagnifyingfilter").equals("hq2x") ) {
            FrameBufferManager.begin(postProcessorOutFBO2);
                shaderPassthruRGBA.bind();
                shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined);
                shaderPassthruRGBA.setUniformi("u_texture", 0);
                hq2x.renderToScreen(postProcessorOutFBO.getColorBufferTexture());
            FrameBufferManager.end();

            shaderPassthruRGBA.bind();
            shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined);
            shaderPassthruRGBA.setUniformi("u_texture", 0);
            postProcessorOutFBO2.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            postProcessorOutFBO2.getColorBufferTexture().bind(0);
            fullscreenQuad.render(shaderPassthruRGBA, GL20.GL_TRIANGLE_FAN);
        }
        else if (getConfigDouble("screenmagnifying") < 1.01 || getConfigString("screenmagnifyingfilter").equals("none")) {
            shaderPassthruRGBA.bind();
            shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined);
            shaderPassthruRGBA.setUniformi("u_texture", 0);
            postProcessorOutFBO.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            postProcessorOutFBO.getColorBufferTexture().bind(0);
            fullscreenQuad.render(shaderPassthruRGBA, GL20.GL_TRIANGLE_FAN);
        }
        else if (getConfigString("screenmagnifyingfilter").equals("bilinear")) {
            shaderPassthruRGBA.bind();
            shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined);
            shaderPassthruRGBA.setUniformi("u_texture", 0);
            postProcessorOutFBO.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            postProcessorOutFBO.getColorBufferTexture().bind(0);
            fullscreenQuad.render(shaderPassthruRGBA, GL20.GL_TRIANGLE_FAN);
        }



        // process resize request
        if (resizeRequested) {
            resizeRequested = false;
            resize(resizeReqSize.getX(), resizeReqSize.getY());
        }



        GLOBAL_RENDER_TIMER += 1;

    }

    private static void processScreenshotRequest(FrameBuffer fb) {
        if (screenshotRequested) {
            String msg = "Screenshot taken";
            FrameBufferManager.begin(fb);
            try {
                Pixmap p = Pixmap.createFromFrameBuffer(0, 0, fb.getWidth(), fb.getHeight());
                PixmapIO.writePNG(Gdx.files.absolute(defaultDir+"/Screenshot-"+String.valueOf(System.currentTimeMillis())+".png"), p, 9, true);
                p.dispose();
            }
            catch (Throwable e) {
                e.printStackTrace();
                msg = ("Failed to take screenshot: "+e.getMessage());
            }
            FrameBufferManager.end();
            screenshotRequested = false;

            Terrarum.INSTANCE.getIngame().sendNotification(msg);
        }
    }

    public static Texture getCurrentDitherTex() {
        int hash = 31 + GLOBAL_RENDER_TIMER + 0x165667B1 + GLOBAL_RENDER_TIMER * 0xC2B2AE3D;
        hash = Integer.rotateLeft(hash, 17) * 0x27D4EB2F;
        hash ^= hash >>> 15;
        hash *= 0x85EBCA77;
        hash ^= hash >>> 13;
        hash *= 0xC2B2AE3D;
        hash ^= hash >>> 16;
        hash = hash & 0x7FFFFFFF;

        return ditherPatterns[hash % ditherPatterns.length];
    }

    private void drawSplash() {
        setCameraPosition(0f, 0f);

        logoBatch.setColor(Color.WHITE);
        logoBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);


        int drawWidth = Toolkit.INSTANCE.getDrawWidth();
        int safetyTextLen = fontGame.getWidth(Lang.INSTANCE.get("APP_WARNING_HEALTH_AND_SAFETY", true));
        int logoPosX = (drawWidth - splashScreenLogo.getRegionWidth() - safetyTextLen) >>> 1;
        int logoPosY = Math.round(scr.getHeight() / 15f);
        int textY = logoPosY + splashScreenLogo.getRegionHeight() - 16;

        // draw custom backdrop (if exists)
        if (splashBackdrop != null) {
            logoBatch.setShader(null);
            logoBatch.begin();


            var size = ((float) Math.max(scr.getWidth(), scr.getHeight()));
            var x = 0f;
            var y = 0f;
            if (scr.getWidth() > scr.getHeight()) {
                y = (scr.getHeight() - size) / 2f;
            }
            else {
                x = (scr.getWidth() - size) / 2f;
            }

            logoBatch.draw(splashBackdrop, x, y, size, size);


            logoBatch.end();
        }



        // draw logo reflection
        logoBatch.setShader(shaderReflect);
        logoBatch.setColor(Color.WHITE);
        logoBatch.begin();
        if (getConfigBoolean("showhealthmessageonstartup")) {
            logoBatch.draw(splashScreenLogo, logoPosX, logoPosY + splashScreenLogo.getRegionHeight());
        }
        else {
            logoBatch.draw(splashScreenLogo, (drawWidth - splashScreenLogo.getRegionWidth()) / 2f,
                    (scr.getHeight() - splashScreenLogo.getRegionHeight() * 2) / 2f + splashScreenLogo.getRegionHeight()
            );
        }
        logoBatch.end();
        logoBatch.setShader(null);
        logoBatch.begin();
        if (getConfigBoolean("showhealthmessageonstartup")) {
            logoBatch.draw(splashScreenLogo, logoPosX, logoPosY);
        }
        else {
            logoBatch.draw(splashScreenLogo, (drawWidth - splashScreenLogo.getRegionWidth()) / 2f,
                    (scr.getHeight() - splashScreenLogo.getRegionHeight() * 2) / 2f
            );
        }



        logoBatch.end();

        // draw health messages
        if (getConfigBoolean("showhealthmessageonstartup")) {
            logoBatch.setShader(null);
            logoBatch.begin();

            logoBatch.setColor(splashTextCol);
            fontGame.draw(logoBatch, Lang.INSTANCE.get("APP_WARNING_HEALTH_AND_SAFETY", true),
                    logoPosX + splashScreenLogo.getRegionWidth(),
                    textY
            );

            // some chinese stuff
            if (GAME_LOCALE.contentEquals("zhCN")) {
                for (int i = 1; i <= 4; i++) {
                    String s = Lang.INSTANCE.get("APP_CHINESE_HEALTHY_GAME_MSG_" + i, true);

                    fontGame.draw(logoBatch, s,
                            (drawWidth - fontGame.getWidth(s)) >>> 1,
                            Math.round(scr.getHeight() * 12f / 15f + fontGame.getLineHeight() * (i - 1))
                    );
                }
            }

            Texture tex1 = CommonResourcePool.INSTANCE.getAsTexture("title_health1");
            Texture tex2 = CommonResourcePool.INSTANCE.getAsTexture("title_health2");
            int virtualHeight = scr.getHeight() - logoPosY - splashScreenLogo.getRegionHeight() / 4;
            int virtualHeightOffset = scr.getHeight() - virtualHeight;
            logoBatch.drawFlipped(tex1, (drawWidth - tex1.getWidth()) >>> 1, virtualHeightOffset + (virtualHeight >>> 1) - 16, tex1.getWidth(), -tex1.getHeight());
            logoBatch.drawFlipped(tex2, (drawWidth - tex2.getWidth()) >>> 1, virtualHeightOffset + (virtualHeight >>> 1) + 16 + tex2.getHeight(), tex2.getWidth(), -tex2.getHeight());

        }

        logoBatch.end();
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * This resize takes the apparent screen size (i.e. zoomed size) as parameters.
     *
     * All other Terrarum's resize() must take real screen size. (i.e not zoomed)
     *
     * @param w0 the new width in pixels
     * @param h0 the new height in pixels
     */
    @Override
    public void resize(int w0, int h0) {

        float magn = (float) getConfigDouble("screenmagnifying");
        int width = (int) Math.floor(w0 / magn);
        int height = (int) Math.floor(h0 / magn);


        printdbg(this, "Resize called: "+width+","+height);
        printStackTrace(this);

        if (width < 2 || height < 2) return;

        //initViewPort(width, height);

        scr.setDimension(width, height, magn);

        if (currentScreen != null) currentScreen.resize(scr.getWidth(), scr.getHeight());
        TerrarumPostProcessor.INSTANCE.resize(scr.getWidth(), scr.getHeight());
        updateFullscreenQuad(fullscreenQuad, scr.getWidth(), scr.getHeight());


        if (renderFBO == null ||
                (renderFBO.getWidth() != scr.getWidth() ||
                        renderFBO.getHeight() != scr.getHeight())
        ) {
            renderFBO = new Float16FrameBuffer(
                    scr.getWidth(),
                    scr.getHeight(),
                    false
            );
            postProcessorOutFBO2 = new Float16FrameBuffer(
                    scr.getWidth() * 2,
                    scr.getHeight() * 2,
                    false
            );


            if (IS_DEVELOPMENT_BUILD) {
                try {
                    Field field = GLFrameBuffer.class.getDeclaredField("framebufferHandle");
                    field.setAccessible(true);
                    System.out.println("Attachment ID for renderFBO: " + field.get(renderFBO));
                }
                catch (NoSuchFieldException | IllegalAccessException e) {
                    System.err.println("Attachment ID for renderFBO: X_x");
                    e.printStackTrace();
                }
            }
        }

        Toolkit.INSTANCE.resize();

        printdbg(this, "Resize end");
    }

    public static void resizeScreen(int width, int height) {
        resizeRequested = true;
        resizeReqSize = new Point2i(width, height);
    }



    @Override
    public void dispose() {
        System.out.println("Goodbye !");

        if (audioManagerThread != null) {
            audioManagerThread.interrupt();
        }

        if (audioMixerInitialised) {
            audioMixer.dispose();
        }

        if (currentScreen != null) {
            currentScreen.hide();
            currentScreen.dispose();
        }

        //IngameRenderer.INSTANCE.dispose();
        //PostProcessor.INSTANCE.dispose();
        //MinimapComposer.INSTANCE.dispose();
        //FloatDrawer.INSTANCE.dispose();

        for (Texture texture : ditherPatterns) {
            texture.dispose();
        }
//        shaderHicolour.dispose();
        shaderDebugDiff.dispose();
        shaderPassthruRGBA.dispose();
        shaderColLUT.dispose();
        shaderReflect.dispose();
        shaderGhastlyWhite.dispose();
        hq2x.dispose();

        CommonResourcePool.INSTANCE.dispose();
        fullscreenQuad.dispose();
        logoBatch.dispose();
        batch.dispose();
//        shapeRender.dispose();

        fontGame.dispose();
        fontGameFBO.dispose();
        fontSmallNumbers.dispose();
        fontBigNumbers.dispose();
        ItemSlotImageFactory.INSTANCE.dispose();

        splashScreenLogo.getTexture().dispose();
        splashBackdrop.getTexture().dispose();

        ModMgr.INSTANCE.disposeMods();

        GameWorld.Companion.makeNullWorld().dispose();

        Terrarum.INSTANCE.dispose();

        inputStrober.dispose();

        audioDevice.dispose();

        deleteTempfiles();

        disposables.forEach((it) -> {
            try {
                it.dispose();
            }
            catch (NullPointerException | IllegalArgumentException | GdxRuntimeException | ConcurrentModificationException e) { }
        });

        // kill the zombies, no matter what
        System.exit(0);
    }

    @Override
    public void pause() {
        if (currentScreen != null) currentScreen.pause();
    }

    @Override
    public void resume() {
        if (currentScreen != null) currentScreen.resume();
    }

    public static LoadScreenBase getLoadScreen() {
        return currentSetLoadScreen;
    }

    public static void setLoadScreen(LoadScreenBase screen) {
        currentSetLoadScreen = screen;
        _setScr(screen);
    }

    public static void setScreen(Screen screen) {
        if (!(screen instanceof TerrarumGamescreen)) {
            throw new IllegalArgumentException("Screen must be instance of TerrarumGameScreen: " + screen.getClass().getCanonicalName());
        }
        
        if (screen instanceof LoadScreenBase) {
            throw new RuntimeException(
                    "Loadscreen '" + screen.getClass().getSimpleName() + "' must be set with 'setLoadScreen()' method");
        }

        _setScr((TerrarumGamescreen) screen);
    }

    private static void _setScr(TerrarumGamescreen screen) {

        printdbg("AppLoader-Static", "Changing screen to " + screen.getClass().getCanonicalName());

        // this whole thing is directtly copied from com.badlogic.gdx.Game

        if (currentScreen != null) {
            printdbg("AppLoader-Static", "Screen before change: " + currentScreen.getClass().getCanonicalName());

            currentScreen.hide();
            currentScreen.dispose();
        }
        else {
            printdbg("AppLoader-Static", "Screen before change: null");
        }


        currentScreen = screen;

        currentScreen.show();
        currentScreen.resize(scr.getWidth(), scr.getHeight());

        TerrarumGlobalState.INSTANCE.getHAS_KEYBOARD_INPUT_FOCUS().unset();

        System.gc();

        printdbg("AppLoader-Static", "Screen transition complete: " + currentScreen.getClass().getCanonicalName());
    }

    private static Boolean audioMixerInitialised = false;

    public static AudioMixer audioMixer;
    public static int audioBufferSize;

    /**
     * Make sure to call App.audioMixerRenewHooks.remove(Object) whenever the class gets disposed of
     * <p>
     * Key: the class that calls the hook, value: the actual operation (function)
     */
    public static HashMap<Object, Function0> audioMixerReloadHooks = new HashMap<>();

    /**
     * Init stuffs which needs GL context
     */
    private void postInit() {
        long t1 = System.nanoTime();


        CommonResourcePool.INSTANCE.addToLoadingList("blockmarkings_common", () -> new TextureRegionPack(Gdx.files.internal("assets/graphics/blocks/block_markings_common.tga"), 16, 16, 0, 0, 0, 0, false, false, false));
        CommonResourcePool.INSTANCE.addToLoadingList("blockmarking_actor", () -> new BlockMarkerActor());
        CommonResourcePool.INSTANCE.addToLoadingList("loading_circle_64", () -> new TextureRegionPack(Gdx.files.internal("assets/graphics/gui/loading_circle_64.tga"), 64, 64, 0, 0, 0, 0, false, false, false));
        CommonResourcePool.INSTANCE.addToLoadingList("inline_loading_spinner", () -> new TextureRegionPack(Gdx.files.internal("assets/graphics/gui/inline_loading_spinner.tga"), 20, 20, 0, 0, 0, 0, false, false, false));
        CommonResourcePool.INSTANCE.addToLoadingList("inventory_category", () -> new TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20, 0, 0, 0, 0, false, false, false));
        CommonResourcePool.INSTANCE.loadAll();

//        shaderHicolour = loadShaderFromClasspath("shaders/default.vert", "shaders/hicolour.frag");
        shaderDebugDiff = loadShaderFromClasspath("shaders/default.vert", "shaders/diff.frag");
        shaderColLUT = loadShaderFromClasspath("shaders/default.vert", "shaders/rgbonly.frag");
        shaderGhastlyWhite = loadShaderFromClasspath("shaders/default.vert", "shaders/ghastlywhite.frag");

        // make gamepad(s)
        if (App.getConfigBoolean("usexinput")) {
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

        // tell the game that we have a gamepad
        environment = RunningEnvironment.PC;

        if (gamepad != null) {
            String name = gamepad.getName().toLowerCase();
            for (String allowedName : gamepadWhitelist) {
                if (name.contains(allowedName)) {
                    environment = RunningEnvironment.CONSOLE;
                    break;
                }
            }
        }
        /*if (gamepad != null) {
            environment = RunningEnvironment.CONSOLE;

            // calibrate the sticks
            printdbg(this, "Calibrating the gamepad...");
            float[] axesZeroPoints = new float[]{
                    gamepad.getAxisRaw(0),
                    gamepad.getAxisRaw(1),
                    gamepad.getAxisRaw(2),
                    gamepad.getAxisRaw(3)
            };
            setConfig("control_gamepad_axiszeropoints", axesZeroPoints);
            for (int i = 0; i < 4; i++) {
                printdbg(this, "Axis " + i + ": " + axesZeroPoints[i]);
            }

        }
        else {
            environment = RunningEnvironment.PC;
        }*/
        fontUITitle = new TerrarumSansBitmap(FONT_DIR, false, false, false,
                false,
                64, false, 0.5f, false
        );
        fontUITitle.setInterchar(1);
        fontGameFBO = new TerrarumSansBitmap(FONT_DIR, false, true, false,
                false,
                64, false, 203f/255f, false
        );
        Lang.invoke();



        ModMgr.INSTANCE.invoke(); // invoke Module Manager


        fontSmallNumbers = TinyAlphNum.INSTANCE;
        fontBigNumbers = BigAlphNum.INSTANCE;

        IME.invoke();
        inputStrober = InputStrober.INSTANCE;

        try {
            audioDevice = Gdx.audio.newAudioDevice(48000, false);
        }
        catch (NullPointerException deviceInUse) {
            deviceInUse.printStackTrace();
            System.err.println("[AppLoader] failed to create audio device: Audio device occupied by Exclusive Mode Device? (e.g. ASIO4all)");
        }

        CommonResourcePool.INSTANCE.loadAll();

        // check if selected IME is accessible; if not, set selected IME to none
        String selectedIME = getConfigString("inputmethod");
        if (!selectedIME.equals("none") && !IME.INSTANCE.getAllHighLayers().contains(selectedIME)) {
            setConfig("inputmethod", "none");
        }

        if (ModMgr.INSTANCE.getModuleInfo().isEmpty()) {



            return;
        }



        printdbg(this, "all modules loaded successfully");


        // test print
        if (IS_DEVELOPMENT_BUILD) {
            System.out.println("[App] Test printing every registered item");
            Terrarum.INSTANCE.getItemCodex().getItemCodex().values().stream().map(GameItem::getOriginalID).forEach(
                    (String s) -> System.out.print(s + " "));
            System.out.println();
        }


        try {
            // create tile atlas
            printdbg(this, "Making terrain textures...");
            tileMaker = new CreateTileAtlas();
            tileMaker.invoke(false);
        }
        catch (NullPointerException e) {
            throw new Error("TileMaker failed to load", e);
        }


        audioBufferSize = getConfigInt("audio_buffer_size");
        audioMixer = new AudioMixer();
        audioMixerInitialised = true;
        audioManagerThread = new Thread(new AudioManagerRunnable(audioMixer), "TerrarumAudioManager");
        audioManagerThread.setPriority(MAX_PRIORITY); // higher = more predictable; audio delay is very noticeable so it gets high priority
        audioManagerThread.start();

        Terrarum.initialise();


        // if there is a predefined screen, open that screen after my init process
        if (injectScreen != null) {
            setScreen(injectScreen);
        }
        else {
            IngameRenderer.initialise();
        }


        hasUpdate = CheckUpdate.INSTANCE.hasUpdate();
        printdbg(this, "Has update: " + hasUpdate);


        long t2 = System.nanoTime();
        double tms = (t2 - t1) / 1000000000.0;
        printdbg(this, "PostInit done; took "+tms+" seconds");
    }

    public static void reloadAudioProcessor(int bufferSize) {
        // copy music tracks
        var oldDynamicTracks = audioMixer.getDynamicTracks();
        var oldStaticTracks = audioMixer.getTracks();

        audioManagerThread.interrupt();
        audioMixer.dispose();

        audioBufferSize = bufferSize;
        audioMixer = new AudioMixer();

        // paste music tracks
        for (int i = 0; i < audioMixer.getDynamicTracks().length; i++) {
            var track = audioMixer.getDynamicTracks()[i];
            oldDynamicTracks[i].copyStatusTo(track);

            var ingame = Terrarum.INSTANCE.getIngame();
            if (ingame != null) {
                // update track references for the actors
                for (var actor : ingame.getActorContainerActive()) {
                    var tracks = actor.getMusicTracks();
                    for (var trackMusic : tracks.keySet()) {
                        if (tracks.get(trackMusic).equals(oldDynamicTracks[i])) {
                            tracks.put(trackMusic, track);
                        }
                    }
                }
                for (var actor : ingame.getActorContainerInactive()) {
                    var tracks = actor.getMusicTracks();
                    for (var trackMusic : tracks.keySet()) {
                        if (tracks.get(trackMusic).equals(oldDynamicTracks[i])) {
                            tracks.put(trackMusic, track);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < audioMixer.getTracks().length; i++) {
            var track = audioMixer.getTracks()[i];
            oldStaticTracks[i].copyStatusTo(track);
        }

        audioManagerThread = new Thread(new AudioManagerRunnable(audioMixer), "TerrarumAudioManager");
        audioManagerThread.setPriority(MAX_PRIORITY); // higher = more predictable; audio delay is very noticeable so it gets high priority
        audioManagerThread.start();


        for (var it : audioMixerReloadHooks.values()) {
            try {
                it.invoke();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }

    }


    private void setCameraPosition(float newX, float newY) {
        camera.position.set((-newX + scr.getWidth() / 2), (-newY + scr.getHeight() / 2), 0f); // deliberate integer division
        camera.update();
        logoBatch.setProjectionMatrix(camera.combined);
    }

    private void updateFullscreenQuad(Mesh mesh, int WIDTH, int HEIGHT) { // NOT y-flipped quads!
        mesh.setVertices(new float[]{
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                WIDTH, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                WIDTH, HEIGHT, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, HEIGHT, 0f, 1f, 1f, 1f, 1f, 0f, 0f
        });
        mesh.setIndices(new short[]{0, 1, 2, 3});
    }

    public static void setGamepadButtonLabels() {
        switch (getConfigString("control_gamepad_labelstyle")) {
            case "nwii"     : gamepadLabelStart = 0xE04B; break; // + mark
            case "logitech" : gamepadLabelStart = 0xE05A; break; // number 10
            case "msxbone"  : gamepadLabelStart = 0xE049; break; // trifold equal sign?
            default         : gamepadLabelStart = 0xE042; break; // |> mark (sonyps, msxb360, generic)
        }

        switch (getConfigString("control_gamepad_labelstyle")) {
            case "nwii"     : gamepadLabelSelect = 0xE04D; break; // - mark
            case "logitech" : gamepadLabelSelect = 0xE059; break; // number 9
            case "sonyps"   : gamepadLabelSelect = 0xE043; break; // solid rectangle
            case "msxb360"  : gamepadLabelSelect = 0xE041; break; // <| mark
            case "msxbone"  : gamepadLabelSelect = 0xE048; break; // multitask button?
            default         : gamepadLabelSelect = 0xE043; break; // solid rectangle
        }


        switch (getConfigString("control_gamepad_labelstyle")) {
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

    public static boolean isScreenshotRequested() {
        return screenshotRequested;
    }

    // DEFAULT DIRECTORIES //

    public static String OSName = System.getProperty("os.name");
    public static String OSVersion = System.getProperty("os.version");
    private static String tempDir = System.getProperty("java.io.tmpdir");
    public static String operationSystem;
    /** %appdata%/Terrarum, without trailing slash */
    public static String defaultDir;
    /** For Demo version only. defaultDir + "/Saves", without trailing slash */
    public static String saveDir;
    /** For shared materials (e.g. image of a computer disk). defaultDir + "/Shared", without trailing slash */
    public static String saveSharedDir;
    /** For the main game where any players can access any world (unless flagged as private). defaultDir + "/Players", without trailing slash */
    public static String playersDir;
    /** For the main game. defaultDir + "/Worlds", without trailing slash */
    public static String worldsDir;
    /** defaultDir + "/config.json" */
    public static String configDir;
    /** defaultDir + "/LoadOrder.txt" */
    public static String loadOrderDir;
    /** defaultDir + "/Imported" */
    public static String importDir;

    public static RunningEnvironment environment;

    /** defaultDir + "/Recycled/Players" */
    public static String recycledPlayersDir;
    /** defaultDir + "/Recycled/Worlds" */
    public static String recycledWorldsDir;
    /** defaultDir + "/Custom/" */
    public static String customDir;
    /** defaultDir + "/Custom/Music" */
    public static String customMusicDir;
    public static String customAmbientDir;

    private static void getDefaultDirectory() {
        String OS = OSName.toUpperCase();
        if (OS.contains("WIN")) {
            operationSystem = "WINDOWS";
            defaultDir = System.getenv("APPDATA") + "/Terrarum";
        }
        else if (OS.contains("OS X") || OS.contains("MACOS")) { // OpenJDK for mac will still report "Mac OS X" with version number "10.16", even on Big Sur and beyond
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
        else {
            operationSystem = "UNKNOWN";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }

        saveDir = defaultDir + "/Saves"; // for the demo release
        saveSharedDir = defaultDir + "/Shared";
        playersDir = defaultDir + "/Players";
        worldsDir = defaultDir + "/Worlds";
        configDir = defaultDir + "/config.json";
        loadOrderDir = defaultDir + "/LoadOrder.txt";
        recycledPlayersDir = defaultDir + "/Recycled/Players";
        recycledWorldsDir = defaultDir + "/Recycled/Worlds";
        importDir = defaultDir + "/Imports";
        customDir = defaultDir + "/Custom";
        customMusicDir = customDir + "/Music";
        customAmbientDir = customDir + "/Ambient";

        System.out.println(String.format("os.name = %s (with identifier %s)", OSName, operationSystem));
        System.out.println(String.format("os.version = %s", OSVersion));
        System.out.println(String.format("default directory: %s", defaultDir));
        System.out.println(String.format("java version = %s", System.getProperty("java.version")));
    }

    private static void createDirs() {
        File[] dirs = {
//                new File(saveDir),
                new File(saveSharedDir),
                new File(playersDir),
                new File(worldsDir),
                new File(recycledPlayersDir),
                new File(recycledWorldsDir),
                new File(importDir),
                new File(customDir),
                new File(customMusicDir)
        };

        for (File it : dirs) {
            if (!it.exists())
                it.mkdirs();
        }

        try {
            createLoadOrderFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //dirs.forEach { if (!it.exists()) it.mkdirs() }
    }

    public static File newTempFile(String filename) {
        File tempfile = new File(tempDir, filename);
        tempFilePool.add(tempfile);
        return tempfile;
    }

    private static void deleteTempfiles() {
        for (File file : tempFilePool) {
            file.delete();
        }
    }

    // CONFIG //

    public static KVHashMap gameConfig = new KVHashMap();

    private static void createLoadOrderFile() throws IOException {
        File loadOrderFile = new File(loadOrderDir);

        if (!loadOrderFile.exists() || loadOrderFile.length() == 0L) {
            var writer = new FileWriter(loadOrderFile);
            writer.write(TerrarumAppConfiguration.DEFAULT_LOADORDER_FILE);
            writer.flush(); writer.close();
        }
    }

    private static void createConfigJson() throws IOException {
        File configFile = new File(configDir);

        if (!configFile.exists() || configFile.length() == 0L) {
            WriteConfig.INSTANCE.invoke();
        }
    }

    /**
     * Reads DefaultConfig to populate the gameConfig
     */
    private static void initialiseConfig() {
        for (Map.Entry<String, Object> entry : DefaultConfig.INSTANCE.getHashMap().entrySet()) {
            gameConfig.set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Will forcibly overwrite previously loaded config value.
     *
     * Key naming convention will be 'modName:propertyName'; if modName is null, the key will be just propertyName.
     *
     * @param value JsonValue (the key-value pair)
     * @param modName module name, nullable
     */
    public static void setToGameConfigForced(JsonValue value, String modName) {
        gameConfig.set((modName == null) ? value.name : modName+":"+value.name,
                value.isArray() ? value.asDoubleArray() :
                value.isDouble() ? value.asDouble() :
                value.isBoolean() ? value.asBoolean() :
                value.isLong() ? value.asInt() :
                value.asString()
        );
    }

    /**
     * Will not overwrite previously loaded config value.
     *
     * Key naming convention will be 'modName:propertyName'; if modName is null, the key will be just propertyName.
     *
     * @param value JsonValue (the key-value pair)
     * @param modName module name, nullable
     */
    public static void setToGameConfig(JsonValue value, String modName) {
        String key = (modName == null) ? value.name : modName+":"+value.name;
        if (gameConfig.get(key) == null) {
            gameConfig.set(key,
                    value.isArray() ? value.asDoubleArray() :
                    value.isDouble() ? value.asDouble() :
                    value.isBoolean() ? value.asBoolean() :
                    value.isLong() ? value.asInt() :
                    value.asString()
            );
        }
    }

    /**
     *
     * @return true on successful, false on failure.
     */
    private static Boolean readConfigJson() {
        try {
            // read from disk and build config from it
            JsonValue map = JsonFetcher.INSTANCE.invoke(configDir);

            // make config
            for (JsonValue entry = map.child; entry != null; entry = entry.next) {
                setToGameConfigForced(entry, null);
            }

            return true;
        }
        catch (IOException e) {
            // write default config to game dir. Call th.is method again to read config from it.
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
        if (key == null) return -1;

        Object cfg = getConfigMaster(key);

        if (cfg instanceof Integer) return ((int) cfg);

        double value = (double) cfg;

        if (Math.abs(value % 1.0) < 0.00000001)
            return (int) Math.round(value);
        return ((int) cfg);
    }


    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static double getConfigDouble(String key) {
        Object cfg = getConfigMaster(key);
        return (cfg instanceof Integer) ? (((Integer) cfg) * 1.0) : ((double) (cfg));
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
            return ((boolean) cfg);
        }
        catch (NullPointerException keyNotFound) {
            return false;
        }
    }

    /*public static int[] getConfigIntArray(String key) {
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
    }*/

    public static double[] getConfigDoubleArray(String key) {
        Object cfg = getConfigMaster(key);
        return ((double[]) cfg);
    }

    public static int[] getConfigIntArray(String key) {
        double[] a = getConfigDoubleArray(key);
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = ((int) a[i]);
        }
        return r;
    }

    /*public static String[] getConfigStringArray(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonArray) {
            JsonArray jsonArray = ((JsonArray) cfg).getAsJsonArray();
            //return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
            String[] intArray = new String[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                intArray[i] = jsonArray.get(i).getAsString();
            }
            return intArray;
        }
        else
            return ((String[]) cfg);
    }*/

    /**
     * Get config from config file. If the entry does not exist, get from defaults; if the entry is not in the default, NullPointerException will be thrown
     */
    private static HashMap<String, Object> getDefaultConfig() {
        return DefaultConfig.INSTANCE.getHashMap();
    }

    public static Object getConfigMaster(String key1) {
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

    public static String csiR = "\u001B[31m";
    public static String csiG = "\u001B[32m";
    public static String csiB = "\u001B[34m";
    public static String csiK = "\u001B[37m";
    public static String csi0 = "\u001B[m";

    static String[] csis = {
            "\u001B[32m",
            "\u001B[33m",
            "\u001B[34m",
            "\u001B[35m",
            "\u001B[36m",

            "\u001B[38;5;22m",
            "\u001B[38;5;23m",
            "\u001B[38;5;24m",
            "\u001B[38;5;25m",
            "\u001B[38;5;26m",
            "\u001B[38;5;27m", // 10
            "\u001B[38;5;28m",
            "\u001B[38;5;29m",
            "\u001B[38;5;30m",
            "\u001B[38;5;31m",
            "\u001B[38;5;32m",
            "\u001B[38;5;33m",

            "\u001B[38;5;54m",
            "\u001B[38;5;56m",
            "\u001B[38;5;57m",
            "\u001B[38;5;58m", // 20
            "\u001B[38;5;63m",

            "\u001B[38;5;126m",
            "\u001B[38;5;127m",
            "\u001B[38;5;128m",
            "\u001B[38;5;129m", // 24
            "\u001B[38;5;130m",
            "\u001B[38;5;131m",

            "\u001B[38;5;162m", // 27
            "\u001B[38;5;163m",
            "\u001B[38;5;164m",
            "\u001B[38;5;165m",
            "\u001B[38;5;166m",
    };

    public static void printdbg(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            var timeNow = System.currentTimeMillis();
            var ss = (timeNow / 1000) % 60;
            var mm = (timeNow / 60000) % 60;
            var hh = (timeNow / 3600000) % 24;
            var ms = timeNow % 1000;
            String out = (obj instanceof String) ? (String) obj : obj.getClass().getSimpleName();
            var hash = (out.hashCode() & 0x7FFFFFFF) % csis.length;
            String prompt = csis[hash]+String.format("%02d:%02d:%02d.%03d [%s]%s ",hh,mm,ss,ms,out,csi0);
            if (message == null) {
                System.out.println(prompt+"null");
            }
            else {
                String indentation = " ".repeat(out.length() + 16);
                String[] msgLines = message.toString().split("\\n");
                for (int i = 0; i < msgLines.length; i++) {
                    System.out.println((i == 0 ? prompt : indentation) + msgLines[i]);
                }
            }
        }
    }

    public static void printdbgerr(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            var timeNow = System.currentTimeMillis();
            var ss = (timeNow / 1000) % 60;
            var mm = (timeNow / 60000) % 60;
            var hh = (timeNow / 3600000) % 24;
            var ms = timeNow % 1000;
            String out = (obj instanceof String) ? (String) obj : obj.getClass().getSimpleName();
            String prompt = csiB+String.format("%02d:%02d:%02d.%03d%s [%s] ",hh,mm,ss,ms,csiR,out);
            if (message == null) {
                System.out.println(prompt+"null"+csi0);
            }
            else {
                String indentation = " ".repeat(out.length() + 16);
                String[] msgLines = message.toString().split("\\n");
                for (int i = 0; i < msgLines.length; i++) {
                    System.out.println((i == 0 ? prompt : indentation) + csiR + msgLines[i] + csi0);
                }
            }
        }
    }

    public static void printmsg(Object obj, Object message) {
        var timeNow = System.currentTimeMillis();
        var ss = (timeNow / 1000) % 60;
        var mm = (timeNow / 60000) % 60;
        var hh = (timeNow / 3600000) % 24;
        var ms = timeNow % 1000;
        String out = (obj instanceof String) ? (String) obj : obj.getClass().getSimpleName();
        String prompt = csiG+String.format("%02d:%02d:%02d.%03d%s [%s] ",hh,mm,ss,ms,csi0,out);
        if (message == null)
            System.out.println(prompt+"null");
        else
            System.out.println(prompt+message);
    }

    public static void printmsgerr(Object obj, Object message) {
        var timeNow = System.currentTimeMillis();
        var ss = (timeNow / 1000) % 60;
        var mm = (timeNow / 60000) % 60;
        var hh = (timeNow / 3600000) % 24;
        var ms = timeNow % 1000;
        String out = (obj instanceof String) ? (String) obj : obj.getClass().getSimpleName();
        String prompt = csiB+String.format("%02d:%02d:%02d.%03d%s [%s] ",hh,mm,ss,ms,csiR,out);
        if (message == null)
            System.out.println(prompt+"null"+csi0);
        else
            System.out.println(prompt+message+csi0);
    }

    public static ShaderProgram loadShaderFromClasspath(String vert, String frag) {
        String v = Gdx.files.classpath(vert).readString("utf-8");
        String f = Gdx.files.classpath(frag).readString("utf-8");
        return loadShaderInline(v, f);
    }

    public static ShaderProgram loadShaderFromFile(String vert, String frag) {
        String v = Gdx.files.internal(vert).readString("utf-8");
        String f = Gdx.files.internal(frag).readString("utf-8");
        return loadShaderInline(v, f);
    }

    public static ShaderProgram loadShaderInline(String vert0, String frag0) {
        // insert version code
        String vert, frag;
        if (gl40capable) {
            vert = "#version 400\n"+vert0;
            frag = "#version 400\n"+frag0;
        }
        else {
            vert = "#version 330\n#define fma(a,b,c) (((a)*(b))+(c))\n"+vert0;
            frag = "#version 330\n#define fma(a,b,c) (((a)*(b))+(c))\n"+frag0;
        }

        ShaderProgram s = new ShaderProgram(vert, frag);

        if (s.getLog().toLowerCase().contains("error")) {
            throw new Error(String.format("Shader program loaded with %s, %s failed:\n%s", vert, frag, s.getLog()));
        }

        return s;
    }

    public static void measureDebugTime(String name, kotlin.jvm.functions.Function0<kotlin.Unit> block) {
            //debugTimers.put(name, kotlin.system.TimingKt.measureNanoTime(block));

        long start = System.nanoTime();
        block.invoke();
        debugTimers.put(name, System.nanoTime() - start);
    }

    public static void setDebugTime(String name, long value) {
        debugTimers.put(name, value);
    }

    public static void addDebugTime(String target, String... targets) {
        long l = 0L;
        for (String s : targets) {
            l += ((long) debugTimers.get(s));
        }
        debugTimers.put(target, l);
    }

    public static long getTIME_T() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Just an event handler I'm slipping in
     * @param event
     */
    public static void inputStrobed(TerrarumKeyboardEvent event) {
        currentScreen.inputStrobed(event);
    }

    /**
     * Corrects out-of-range config values
     */
    private static void rectifyConfigs() {
        // force set min autosave interval to 5 minutes
        if (getConfigInt("autosaveinterval") < 5 * 60000) {
            setConfig("autosaveinterval", 5 * 60000);
        }
    }


    private static boolean highPrioritySoundPlaying = false;

    public static void playGUIsound(MusicContainer sound, double volume, float pan) {
        if (!highPrioritySoundPlaying) {
            var it = audioMixer.getFreeGuiTrack();
            if (it != null) {
                it.stop();
                it.setCurrentTrack(sound);
                it.setMaxVolumeFun(() -> volume);
                it.setVolume(volume);
                ((BinoPan) it.getFilters()[0]).setPan(pan);
                it.play();
            }
        }
    }
    public static void playGUIsound(MusicContainer sound, double volume) { playGUIsound(sound, volume, 0.0f); }
    public static void playGUIsound(MusicContainer sound) { playGUIsound(sound, 1.0, 0.0f); }

    public static void playGUIsoundHigh(MusicContainer sound, double volume, float pan) {
        // TODO when a sound is played thru this function, other sound play calls thru playGUIsound are ignored until this sound finishes playing
        var it = audioMixer.getFreeGuiTrackNoMatterWhat();
        highPrioritySoundPlaying = true;
        it.stop();
        it.setCurrentTrack(sound);
        it.setMaxVolumeFun(() -> volume);
        it.setVolume(volume);
        ((BinoPan) it.getFilters()[0]).setPan(pan);
        it.play();
    }
    public static void playGUIsoundHigh(MusicContainer sound, double volume) { playGUIsoundHigh(sound, volume, 0.0f); }
    public static void playGUIsoundHigh(MusicContainer sound) { playGUIsoundHigh(sound, 1.0, 0.0f); }
}
