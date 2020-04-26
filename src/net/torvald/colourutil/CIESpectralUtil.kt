package net.torvald.colourutil

import com.jme3.math.FastMath.exp
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Using Analytic approximation by Wyman el al.
 *
 * https://www.ppsloan.org/publications/XYZJCGT.pdf
 *
 * Created by minjaesong on 2020-04-20.
 */

/**
 * @return resonable approximation of xbar(lambda) aka CIE XYZ Colour Matching Function
 */
fun getX(wave: Float): Float {
    val xt1 = (wave - 442.0f) * if (wave < 442.0f) 0.0624f else 0.0374f
    val xt2 = (wave - 599.8f) * if (wave < 599.8f) 0.0264f else 0.0323f
    val xt3 = (wave - 501.1f) * if (wave < 501.1f) 0.0490f else 0.0382f
    return 0.362f * exp(-0.5f * xt1 * xt1) + 1.056f * exp(-0.5f * xt2 * xt2) - 0.065f * exp(-0.5f * xt3 * xt3)
}

/**
 * @return resonable approximation of ybar(lambda) aka CIE XYZ Colour Matching Function
 */
fun getY(wave: Float): Float {
    val yt1 = (wave - 568.8f) * if (wave < 568.8f) 0.0213f else 0.0247f
    val yt2 = (wave - 530.9f) * if (wave < 530.9f) 0.0613f else 0.0322f
    return 0.821f * exp(-0.5f * yt1 * yt1) + 0.286f * exp(-0.5f * yt2 * yt2)
}

/**
 * @return resonable approximation of zbar(lambda) aka CIE XYZ Colour Matching Function
 */
fun getZ(wave: Float): Float {
    val zt1 = (wave - 437.0f) * if (wave < 437.0f) 0.0845f else 0.0278f
    val zt2 = (wave - 459.0f) * if (wave < 459.0f) 0.0385f else 0.0725f
    return 1.217f * exp(-0.5f * zt1 * zt1) + 0.681f * exp(-0.5f * zt2 * zt2)
}

fun getXYZUsingIntegral(waves: FloatArray, samples: FloatArray): CIEXYZ {
    assertTrue(
            waves.size == samples.size,
            "number of waves and number of samples must match! (${waves.size}, ${samples.size})"
    )

    val X = (0 until waves.size - 1).map { i ->
        val delta = waves[i + 1] - waves[i]
        val pThis = samples[i] * getX(waves[i])
        val pNext = samples[i + 1] * getX(waves[i + 1])
        pThis * delta + (pNext - pThis) * delta / 2f // linear interpolation
    }.sum()
    val Y = (0 until waves.size - 1).map { i ->
        val delta = waves[i + 1] - waves[i]
        val pThis = samples[i] * getY(waves[i])
        val pNext = samples[i + 1] * getY(waves[i + 1])
        pThis * delta + (pNext - pThis) * delta / 2f // linear interpolation
    }.sum()
    val Z = (0 until waves.size - 1).map { i ->
        val delta = waves[i + 1] - waves[i]
        val pThis = samples[i] * getZ(waves[i])
        val pNext = samples[i + 1] * getZ(waves[i + 1])
        pThis * delta + (pNext - pThis) * delta / 2f // linear interpolation
    }.sum()

    return CIEXYZ(X, Y, Z)
}