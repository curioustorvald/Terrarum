package net.torvald.terrarum.tests

import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.getXYZUsingIntegral
import net.torvald.colourutil.toRGB
import net.torvald.colourutil.toRGBRaw

fun main() {
    val waves =   floatArrayOf(485f,495f,505f,515f,525f,535f,545f,555f,565f)
    val samples = floatArrayOf(  0f,0.2f,0.5f,1f,  1f,    1f,0.5f,0.2f,0f)

    val xyz = getXYZUsingIntegral(waves, samples)
    val srgb = xyz.toRGB()

    println(xyz)
    println(srgb)


    println(CIEXYZ(100f/3f, 100f/3f, 100f/3f).toRGB())
}