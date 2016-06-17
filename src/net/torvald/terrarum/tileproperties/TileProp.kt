package net.torvald.terrarum.tileproperties

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamemap.WorldTime

/**
 * Created by minjaesong on 16-02-16.
 */
class TileProp {

    var id: Int = 0

    var nameKey: String = ""

    /**
     * @param opacity Raw RGB value, without alpha
     */
    var opacity: Int = 0 // colour attenuation

    var strength: Int = 0
    var density: Int = 0

    var isFluid: Boolean = false
    var isSolid: Boolean = false
    var isWallable: Boolean = false

    /**
     * @param luminosity Raw RGB value, without alpha
     */
    private var realLum: Int = 0
    var luminosity: Int
        set(value) {
            realLum = value
        }
        get() = TilePropUtil.getDynamicLumFunc(realLum, dynamicLuminosityFunction)

    var drop: Int = 0
    var dropDamage: Int = 0

    var isFallable: Boolean = false

    var friction: Int = 0

    var dynamicLuminosityFunction: Int = 0
}