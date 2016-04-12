package net.torvald.terrarum.gameactors

import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
interface LandHolder {

    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    var houseDesignation: ArrayList<Int>?
    fun addHouseTile(x: Int, y: Int);
    fun removeHouseTile(x: Int, y: Int);
    fun clearHouseDesignation();

}