package net.torvald.terrarum;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import net.torvald.terrarumsansbitmap.gdx.GameFontBase;

import java.io.File;

/**
 * Created by minjaesong on 2019-06-27.
 */
public class MusicComposerApp extends ApplicationAdapter {

    public static LwjglApplicationConfiguration appConfig;
    public static GameFontBase fontGame;

    public MusicComposerApp(LwjglApplicationConfiguration appConfig) {
        this.appConfig = appConfig;
    }

    public void main(String[] args) {

        LwjglApplicationConfiguration appConfig = new LwjglApplicationConfiguration();
        appConfig.useGL30 = true; // utilising some GL trickeries, need this to be TRUE
        appConfig.resizable = false;//true;
        //appConfig.width = 1110; // photographic ratio (1.5:1)
        //appConfig.height = 740; // photographic ratio (1.5:1)
        appConfig.width = 1000;;;
        appConfig.height = 666;
        appConfig.backgroundFPS = 9999;
        appConfig.foregroundFPS = 9999;
        appConfig.title = "Speelklok";
        appConfig.forceExit = false;

        // load app icon
        int[] appIconSizes = new int[]{256,128,64,32,16};
        for (int size : appIconSizes) {
            String name = "assets/appicon" + size + ".png";
            if (new File("./" + name).exists()) {
                appConfig.addIcon(name, Files.FileType.Internal);
            }
        }

        new LwjglApplication(new MusicComposerApp(appConfig), appConfig);
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
