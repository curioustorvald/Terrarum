package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.torvald.dataclass.ArrayListMap;
import net.torvald.terrarum.modulebasegame.IngameRenderer;
import net.torvald.terrarum.utils.JsonFetcher;
import net.torvald.terrarum.utils.JsonWriter;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack;

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
    public static final int VERSION_RAW = 0x00_02_04B1;

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
     * @return
     */
    public static AppLoader getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new AppLoader();
        }
        return INSTANCE;
    }

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2018 Torvald (minjaesong)";
    public static String GAME_LOCALE = System.getProperty("user.language") + System.getProperty("user.country");

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

    public static LwjglApplicationConfiguration appConfig;
    public static GameFontBase fontGame;

    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    public static int GLOBAL_RENDER_TIMER = new Random().nextInt(1020) + 1;


    public static ArrayListMap debugTimers = new ArrayListMap<String, Long>();


    public static void main(String[] args) {
        ShaderProgram.pedantic = false;

        LwjglApplicationConfiguration appConfig = new LwjglApplicationConfiguration();
        //appConfig.useGL30 = true; // used: loads GL 3.2, unused: loads GL 4.6; what the fuck?
        appConfig.vSyncEnabled = false;
        appConfig.resizable = false;//true;
        //appConfig.width = 1072; // IMAX ratio
        //appConfig.height = 742; // IMAX ratio
        appConfig.width = 1110; // photographic ratio (1.5:1)
        appConfig.height = 740; // photographic ratio (1.5:1)
        appConfig.backgroundFPS = 9999;
        appConfig.foregroundFPS = 9999;
        appConfig.title = GAME_NAME;
        appConfig.forceExit = false;

        if (args.length == 1 && args[0].equals("isdev=true")) {
            IS_DEVELOPMENT_BUILD = true;
        }

        new LwjglApplication(new AppLoader(appConfig), appConfig);
    }


    private static ShaderProgram shaderBayerSkyboxFill;
    public static ShaderProgram shaderHicolour;
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

    private void initViewPort(int width, int height) {
        // Set Y to point downwards
        camera.setToOrtho(true, width, height);

        // Update camera matrix
        camera.update();

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height);
    }

    private float loadTimer = 0f;
    private final float showupTime = 100f / 1000f;

    private FrameBuffer renderFBO;

    @Override
    public void create() {
        logoBatch = new SpriteBatch();
        camera = new OrthographicCamera(((float) appConfig.width), ((float) appConfig.height));

        initViewPort(appConfig.width, appConfig.height);

        logo = new TextureRegion(new Texture(Gdx.files.internal("assets/graphics/logo_placeholder.tga")));
        logo.flip(false, true);

        shaderBayerSkyboxFill = loadShader("assets/4096.vert", "assets/4096_bayer_skyboxfill.frag");
        shaderHicolour = loadShader("assets/4096.vert", "assets/hicolour.frag");
        shaderColLUT = loadShader("assets/4096.vert", "assets/passthru.frag");

        fullscreenQuad = new Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        );
        updateFullscreenQuad(appConfig.width, appConfig.height);
    }

    @Override
    public void render() {

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
            logoBatch.draw(logo, (appConfig.width - logo.getRegionWidth()) / 2f,
                    (appConfig.height - logo.getRegionHeight()) / 2f
            );
            logoBatch.end();


            loadTimer += Gdx.graphics.getRawDeltaTime();

            if (loadTimer >= showupTime) {
                setScreen(Terrarum.INSTANCE);
            }
        }
        // draw the screen
        else {
            screen.render(Gdx.graphics.getDeltaTime());
        }

        // nested FBOs are just not a thing in GL!
        net.torvald.terrarum.FrameBufferManager.end();

        PostProcessor.INSTANCE.draw(camera.combined, renderFBO);


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

        appConfig.width = screenW;
        appConfig.height = screenH;

        updateFullscreenQuad(screenW, screenH);

        printdbg(this, "Resize event");
    }

    @Override
    public void dispose() {
        System.out.println("Goodbye !");


        if (screen != null) {
            screen.hide();
            screen.dispose();
        }

        IngameRenderer.INSTANCE.dispose();


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
        }
        this.screen = screen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        printdbg(this, "Screen transisiton complete: " + this.screen.getClass().getCanonicalName());
    }

    private void postInit() {
        // load configs
        getDefaultDirectory();
        createDirs();
        readConfigJson();

        textureWhiteSquare = new Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"));
        textureWhiteSquare.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        TextureRegionPack.Companion.setGlobalFlipY(true);
        fontGame = new GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", false, true,
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, false, 256, false
        );

        audioDevice = Gdx.audio.newAudioDevice(48000, false);

        // if there is a predefined screen, open that screen after my init process
        if (injectScreen != null) {
            setScreen(injectScreen);
        }


        printdbg(this, "PostInit done");
    }


    private void setCameraPosition(float newX, float newY) {
        camera.position.set((-newX + appConfig.width / 2), (-newY + appConfig.height / 2), 0f);
        camera.update();
        logoBatch.setProjectionMatrix(camera.combined);
    }

    private void updateFullscreenQuad(int WIDTH, int HEIGHT) {
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

    private void getDefaultDirectory() {
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
        else if (System.getProperty("java.runtime.name").toUpperCase().contains("ANDROID")) {
            operationSystem = "ANDROID";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
            environment = RunningEnvironment.MOBILE;
        }
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

    private void createDirs() {
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
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static boolean getConfigBoolean(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonPrimitive)
        return ((JsonPrimitive) cfg).getAsBoolean();
        else
        return ((boolean) cfg);
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
                throw new NullPointerException("key not found: '$key'");
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
        gameConfig.set(key, value);
    }



    // //

    public static void printdbg(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD || getConfigBoolean("forcedevbuild")) {
            System.out.println("[" + obj.getClass().getSimpleName() + "] " + message.toString());
        }
    }

    public static void printdbgerr(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD || getConfigBoolean("forcedevbuild")) {
            System.err.println("[" + obj.getClass().getSimpleName() + "] " + message.toString());
        }
    }

    public static ShaderProgram loadShader(String vert, String frag) {
        ShaderProgram s = new ShaderProgram(Gdx.files.internal(vert), Gdx.files.internal(frag));

        if (s.getLog().contains("error C")) {
            throw new Error(String.format("Shader program loaded with %s, %s failed:\n%s", vert, frag, s.getLog()));
        }

        return s;
    }
}