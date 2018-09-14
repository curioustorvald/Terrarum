package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack;

import java.util.Arrays;
import java.util.Random;

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
     *
     * e.g. 0x02010034 can be translated as 2.1.52
     */
    public static final int VERSION_RAW = 0x00_02_027C;
    public static final boolean IS_DEVELOPMENT_BUILD = true;




    private static AppLoader INSTANCE = null;

    private AppLoader() { }

    public static AppLoader getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new AppLoader();
        }
        return INSTANCE;
    }

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2018 Torvald (minjaesong)";
    public static String GAME_LOCALE = System.getProperty("user.language") + System.getProperty("user.country");

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

    public static final String getVERSION_STRING() {
        return String.format("%d.%d.%d", VERSION_RAW >>> 24, (VERSION_RAW & 0xff0000) >>> 16, VERSION_RAW & 0xFFFF);
    }

    public static LwjglApplicationConfiguration appConfig;

    public static GameFontBase fontGame;

    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    public static int GLOBAL_RENDER_TIMER = new Random().nextInt(1020) + 1;


    public static void main(String[] args) {
        ShaderProgram.pedantic = false;

        appConfig = new LwjglApplicationConfiguration();
        appConfig.vSyncEnabled = false;
        appConfig.resizable = false;//true;
        //appConfig.width = 1072; // IMAX ratio
        //appConfig.height = 742; // IMAX ratio
        appConfig.width = 1110; // photographic ratio (1.5:1)
        appConfig.height = 740; // photographic ratio (1.5:1)
        appConfig.backgroundFPS = 9999;
        appConfig.foregroundFPS = 9999;
        appConfig.title = GAME_NAME;

        new LwjglApplication(new AppLoader(), appConfig);
    }


    private static ShaderProgram shaderBayerSkyboxFill;
    public static ShaderProgram shaderHicolour;
    public static ShaderProgram shaderColLUT;

    public static Mesh fullscreenQuad;
    private OrthographicCamera camera;
    private SpriteBatch logoBatch;
    public static TextureRegion logo;

    private Color gradWhiteTop = new Color(0xf8f8f8ff);
    private Color gradWhiteBottom = new Color(0xd8d8d8ff);

    public Screen screen;

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


        shaderBayerSkyboxFill = new ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer_skyboxfill.frag"));
        shaderHicolour = new ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/hicolour.frag"));
        shaderColLUT = new ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/passthru.frag"));




        fullscreenQuad = new Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        );

        fullscreenQuad.setVertices(new float[]{
            0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                    ((float) appConfig.width), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                    ((float) appConfig.width), ((float) appConfig.height), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                    0f, ((float) appConfig.height), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        });
        fullscreenQuad.setIndices(new short[]{0, 1, 2, 2, 3, 0});


        logo = new TextureRegion(new Texture(Gdx.files.internal("assets/graphics/logo_placeholder.tga")));
        logo.flip(false, true);


        TextureRegionPack.Companion.setGlobalFlipY(true);
        fontGame = new GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", false, true, Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    @Override
    public void render() {

        FrameBufferManager.begin(renderFBO);
        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
        FrameBufferManager.end();


        FrameBufferManager.begin(renderFBO);
        setCameraPosition(0, 0);

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
                Terrarum.INSTANCE.setScreenW(appConfig.width);
                Terrarum.INSTANCE.setScreenH(appConfig.height);
                setScreen(Terrarum.INSTANCE);
            }
        }
        else {
            screen.render(Gdx.graphics.getDeltaTime());
        }

        // nested FBOs are just not a thing in GL!
        FrameBufferManager.end();

        PostProcessor.INSTANCE.draw(camera.combined, renderFBO);



        GLOBAL_RENDER_TIMER += 1;
    }

    @Override
    public void resize(int width, int height) {
        //initViewPort(width, height);

        Terrarum.INSTANCE.resize(width, height);
        if (screen != null) screen.resize(Terrarum.INSTANCE.getWIDTH(), Terrarum.INSTANCE.getHEIGHT());


        if (renderFBO == null ||
                (renderFBO.getWidth() != Terrarum.INSTANCE.getWIDTH() ||
                renderFBO.getHeight() != Terrarum.INSTANCE.getHEIGHT())
                ) {
            renderFBO = new FrameBuffer(
                    Pixmap.Format.RGBA8888,
                    Terrarum.INSTANCE.getWIDTH(),
                    Terrarum.INSTANCE.getHEIGHT(),
                    false
            );
        }

        appConfig.width = Terrarum.INSTANCE.getWIDTH();
        appConfig.height = Terrarum.INSTANCE.getHEIGHT();

        printdbg(this, "Resize event");
    }

    @Override
    public void dispose () {
        if (screen != null) screen.hide();
    }

    @Override
    public void pause () {
        if (screen != null) screen.pause();
    }

    @Override
    public void resume () {
        if (screen != null) screen.resume();
    }

    public void setScreen(Screen screen) {
        printdbg(this, "Changing screen to " + screen.getClass().getCanonicalName());

        if (this.screen != null) this.screen.hide();
        this.screen = screen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        printdbg(this, "Screen transisiton complete: " + this.screen.getClass().getCanonicalName());
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


    public static final void printdbg(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            System.out.println("["+obj.getClass().getSimpleName()+"] "+message.toString());
        }
    }
    public static final void printdbgerr(Object obj, Object message) {
        if (IS_DEVELOPMENT_BUILD) {
            System.err.println("["+obj.getClass().getSimpleName()+"] "+message.toString());
        }
    }
}
