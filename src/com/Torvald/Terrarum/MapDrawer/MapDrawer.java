package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.Terrarum.*;
import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.GameMap.GameMap;
import org.newdawn.slick.*;
import org.newdawn.slick.geom.Rectangle;

/**
 * Created by minjaesong on 15-12-31.
 */
public class MapDrawer {

    private static SpriteSheet mapTileMap;
    private static SpriteSheet wallTileMap;

    public static final int TILE_SIZE = 16;
    public static final int ENVCOLOUR_MAX = 128;

    private static int envmap = 64;
    private static Rectangle envOverlay;
    private static Image envOverlayColourmap;

    public MapDrawer(GameMap map) throws SlickException {
        mapTileMap = new SpriteSheet("./res/graphics/terrain/terrain.png", TILE_SIZE, TILE_SIZE);
        wallTileMap = new SpriteSheet("./res/graphics/terrain/wall.png", TILE_SIZE, TILE_SIZE);

        new MapCamera(map, TILE_SIZE);

        Rectangle envOverlay = new Rectangle(
                MapCamera.getCameraX() * Game.screenZoom
                , MapCamera.getCameraY() * Game.screenZoom
                , Terrarum.WIDTH
                , Terrarum.HEIGHT
        );

        System.gc();
    }

    public static void update(GameContainer gc, int delta_t) {
        MapCamera.update(gc, delta_t);
    }

    public static void render(GameContainer gc, Graphics g) {
        MapCamera.render(gc, g);
    }

    public static void drawEnvOverlay(Graphics g) {
        envOverlay.setX(MapCamera.getCameraX() * Game.screenZoom);
        envOverlay.setY(MapCamera.getCameraY() * Game.screenZoom);
        envOverlay.setSize(Terrarum.WIDTH * Game.screenZoom
                , Terrarum.HEIGHT * Game.screenZoom
        );

        // Color[] colourTable = getGradientColour(WorldTime.elapsedSeconds());
    }

}
