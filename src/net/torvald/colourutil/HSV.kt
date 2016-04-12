package net.torvald.colourutil

/**
 * Created by minjaesong on 16-03-10.
 */
/**
 * @param h : Hue 0-359
 * @param s : Saturation 0-1
 * @param v : Value (brightness in Adobe Photoshop(TM)) 0-1
 */
data class HSV(
    var h: Float,
    var s: Float,
    var v: Float
)
