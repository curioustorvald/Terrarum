package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.Terrarum.*;
import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.TileProperties.TileNameCode;
import com.Torvald.Terrarum.TileStat.TileStat;
import com.jme3.math.FastMath;
import org.newdawn.slick.*;
import org.newdawn.slick.geom.Rectangle;

/**
 * Created by minjaesong on 15-12-31.
 */
public class MapDrawer {

    public static final int TILE_SIZE = 16;

    private static Image envOverlayColourmap;

    private static final int ENV_COLTEMP_LOWEST = 5500;
    private static final int ENV_COLTEMP_HIGHEST = 7500;

    private static int colTemp;

    public MapDrawer(GameMap map) throws SlickException {
        new MapCamera(map);

        envOverlayColourmap = new Image("./res/graphics/black_body_col_1000_40000_K.png");

        System.gc();
    }

    public static void update(GameContainer gc, int delta_t) {
    }

    public static void render(GameContainer gc, Graphics g) {
    }

    public static void drawEnvOverlay(Graphics g) {
        int onscreen_tiles_max = FastMath.ceil(Terrarum.HEIGHT * Terrarum.WIDTH / FastMath.sqr(TILE_SIZE))
                * 2;
        float onscreen_tiles_cap = onscreen_tiles_max / 4f;
        float onscreen_cold_tiles = TileStat.getCount(
                  TileNameCode.ICE_MAGICAL
                , TileNameCode.ICE_FRAGILE
                , TileNameCode.ICE_NATURAL
                , TileNameCode.SNOW
        );

        colTemp = colTempLinearFunc((onscreen_cold_tiles / onscreen_tiles_cap));
        float zoom = Terrarum.game.screenZoom;

        g.setColor(getColourFromMap(colTemp));
        g.fillRect(MapCamera.getCameraX() * zoom
                , MapCamera.getCameraY() * zoom
                , Terrarum.WIDTH * ((zoom < 1) ? 1f / zoom : zoom)
                , Terrarum.HEIGHT * ((zoom < 1) ? 1f / zoom : zoom)
        );

        // Color[] colourTable = getGradientColour(WorldTime.elapsedSeconds());
    }

    /**
     *
     * @param x [-1 , 1], 0 for 6500K (median of ENV_COLTEMP_HIGHEST and ENV_COLTEMP_LOWEST)
     * @return
     */
    private static int colTempLinearFunc(float x) {
        int colTempMedian = (ENV_COLTEMP_HIGHEST + ENV_COLTEMP_LOWEST) / 2;

        return Math.round((ENV_COLTEMP_HIGHEST - ENV_COLTEMP_LOWEST) / 2 * FastMath.clamp(x, -1f, 1f)
                + colTempMedian);
    }

    private static Color getColourFromMap(int K) {
        return envOverlayColourmap.getColor(colTempToImagePos(K), 0);
    }

    private static int colTempToImagePos(int K) {
        if (K < 1000 || K >= 40000) throw new IllegalArgumentException("K: out of range. (" + K + ")");
        return (K - 1000) / 10;
    }

    public static int getColTemp() {
        return colTemp;
    }
}
