package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by minjaesong on 2017-08-01.
 */
public class TerrarumAppLoader implements ApplicationListener {

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2017 Torvald (minjaesong)";

    private static LwjglApplicationConfiguration appConfig;

    public static void main(String[] args) {
        appConfig = new LwjglApplicationConfiguration();
        appConfig.vSyncEnabled = false;
        appConfig.resizable = true;
        appConfig.width = 1072;
        appConfig.height = 742;
        appConfig.backgroundFPS = 9999;
        appConfig.foregroundFPS = 9999;
        appConfig.title = GAME_NAME;

        new LwjglApplication(new TerrarumAppLoader(), appConfig);
    }


    private ShaderProgram shaderBayerSkyboxFill;
    private Mesh fullscreenQuad;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private TextureRegion logo;

    private Color gradWhiteTop = new Color(0xf8f8f8ff);
    private Color gradWhiteBottom = new Color(0xd8d8d8ff);

    public Screen screen;

    private TerrarumAppLoader() {
    }

    private void initViewPort(int width, int height) {
        // Set Y to point downwards
        camera.setToOrtho(true, width, height);

        // Update camera matrix
        camera.update();

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height);
    }

    private float loadTimer = 0f;
    private final float showupTime = 50f / 1000f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera(((float) appConfig.width), ((float) appConfig.height));


        initViewPort(appConfig.width, appConfig.height);


        shaderBayerSkyboxFill = new ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer_skyboxfill.frag"));


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
    }

    @Override
    public void render() {
        if (screen == null) {
            shaderBayerSkyboxFill.begin();
            shaderBayerSkyboxFill.setUniformMatrix("u_projTrans", camera.combined);
            shaderBayerSkyboxFill.setUniformf("topColor", gradWhiteTop.r, gradWhiteTop.g, gradWhiteTop.b);
            shaderBayerSkyboxFill.setUniformf("bottomColor", gradWhiteBottom.r, gradWhiteBottom.g, gradWhiteBottom.b);
            fullscreenQuad.render(shaderBayerSkyboxFill, GL20.GL_TRIANGLES);
            shaderBayerSkyboxFill.end();

            batch.begin();
            batch.setColor(Color.WHITE);
            //blendNormal();
            batch.setShader(null);


            setCameraPosition(0f, 0f);
            batch.draw(logo, (appConfig.width - logo.getRegionWidth()) / 2f,
                    (appConfig.height - logo.getRegionHeight()) / 2f
            );
            batch.end();


            loadTimer += Gdx.graphics.getRawDeltaTime();

            if (loadTimer >= showupTime) {
                Terrarum.INSTANCE.setAppLoader(this);
                setScreen(Terrarum.INSTANCE);
            }
        }
        else {
            screen.render(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void resize(int width, int height) {
        initViewPort(width, height);
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
        if (this.screen != null) this.screen.hide();
        this.screen = screen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    private void setCameraPosition(float newX, float newY) {
        camera.position.set((-newX + appConfig.width / 2), (-newY + appConfig.height / 2), 0f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }
}
