package net.torvald.terrarum

import net.torvald.terrarum.langpack.Lang

/**
 * You directly modify the source code to tune the engine to suit your needs.
 *
 * Created by minjaesong on 2019-08-15.
 */
object TerrarumAppConfiguration {
    //////////////////////////////////////
    // CONFIGURATION FOR THE APP ITSELF //
    //////////////////////////////////////
    const val GAME_NAME = "Terrarum"
    const val COPYRIGHT_DATE_NAME = "Copyright 2013-2021 CuriousTorvald (minjaesong)"
    val COPYRIGHT_LICENSE: String; get() = Lang["COPYRIGHT_GNU_GPL_3"]
    const val COPYRIGHT_LICENSE_ENGLISH = "Distributed under GNU GPL 3"

    /**
     *
     *
     * Version numbering that follows Semantic Versioning 2.0.0 (https://semver.org/)
     *
     *
     *
     *
     * 0xAA_BB_XXXX, where:
     *
     *  * AA: Major version
     *  * BB: Minor version
     *  * XXXX: Patch version
     *
     *
     * e.g. 0x02010034 will be translated as 2.1.52
     *
     */
    const val VERSION_RAW = 0x000206D3

    //////////////////////////////////////////////////////////
    //             CONFIGURATION FOR TILE MAKER             //
    // MAKE SURE THESE VALUES ARE UNIQUE IN THE SOURCE CODE //
    //////////////////////////////////////////////////////////
    const val TILE_SIZE = 16
    const val TILE_SIZEF = TILE_SIZE.toFloat()
    const val TILE_SIZED = TILE_SIZE.toDouble()
}