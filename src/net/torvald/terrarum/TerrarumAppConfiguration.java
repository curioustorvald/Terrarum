package net.torvald.terrarum;

/**
 * You directly modify the source code to tune the engine to suit your needs.
 *
 * Created by minjaesong on 2019-08-15.
 */
public class TerrarumAppConfiguration {

    //////////////////////////////////////
    // CONFIGURATION FOR THE APP ITSELF //
    //////////////////////////////////////

    public static final String GAME_NAME = "Terrarum";
    public static final String COPYRIGHT_DATE_NAME = "Copyright 2013-2021 Torvald (minjaesong)";

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
    public static final int VERSION_RAW = 0x00_02_06D3;


    //////////////////////////////////
    // CONFIGURATION FOR TILE MAKER //
    //////////////////////////////////

    public static final int MAX_TEX_SIZE = 4096;
    public static final int TILE_SIZE = 16;

}
