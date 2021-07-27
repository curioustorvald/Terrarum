package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;

import java.io.File;

/**
 * Created by minjaesong on 2019-06-27.
 */
public class MusicComposerApp extends ApplicationAdapter {

    public static Lwjgl3ApplicationConfiguration appConfig;
    public static GameFontBase fontGame;

    public MusicComposerApp(Lwjgl3ApplicationConfiguration appConfig) {
        this.appConfig = appConfig;
    }

    public void main(String[] args) {

        Lwjgl3ApplicationConfiguration appConfig = new Lwjgl3ApplicationConfiguration();

        appConfig.setResizable(false);
        appConfig.setWindowedMode(1280, 720);
        appConfig.setTitle("Speelklok");

        new Lwjgl3Application(new MusicComposerApp(appConfig), appConfig);
    }

    @Override
    public void create() {
        fontGame = new GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", false, true, false,
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, false, 256, false
        );
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
