package net.torvald.terrarum.modulebasegame.gameactors

import java.util.*

/**
 * Created by minjaesong on 2016-02-20.
 */
interface LandHolder {

    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    var houseDesignation: ArrayList<Long>?
    fun addHouseTile(x: Int, y: Int)
    fun removeHouseTile(x: Int, y: Int)
    fun clearHouseDesignation()

}