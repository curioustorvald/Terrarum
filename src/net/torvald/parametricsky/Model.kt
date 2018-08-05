package net.torvald.parametricsky

import net.torvald.dataclass.Matrix
import kotlin.math.pow

/**
 * This model is about the backdrop; to paint the background using realistic colour of the sky.
 * This model does not provide the global light in itself; It's up to you.
 *
 * J. Preetham, A & Shirley, Peter & E. Smits, Brian. (1999). *A Practical Analytic Model for Daylight*. Proceedings of ACM SIGGRAPH. 1999. 91-100. 10.1145/311535.311545.
 *
 * This implementation is NOT FOR realtime calculation, especially in video games.
 * You are advised to precalculate necessary colours and put the results as a colour map.
 *
 * Created by minjaesong on 2018-08-01.
 */

object Model {

    /** Skylight Distribution Coefficients and Zenith Values */
    data class DistributionCoeff(
            var A: Double, var B: Double, var C: Double, var D: Double, var E: Double
    )

    /**
     * @param theta_s solar angle from zenith in radians. 0 means at zenith; 0.5pi means at horizon, >0.5pi indicates below horizon
     * @param phi_s solar azimuth in radians. 0: East; 0.5pi: South; pi: West
     */
    data class SolarPosition(var theta_s: Double, var phi_s: Double)

    ///////////////////////////////////////////////////////////////////////////

    fun getFforLuma(theta: Double, gamma: Double, T: Double) = _getFbyPerez(theta, gamma, getLuminanceDistributionFun(T))
    fun getFforChromaX(theta: Double, gamma: Double, T: Double) = _getFbyPerez(theta, gamma, getChromaXDistributionFun(T))
    fun getFforChromaY(theta: Double, gamma: Double, T: Double) = _getFbyPerez(theta, gamma, getChromaYDistributionFun(T))

    /*private*/ fun _getFbyPerez(theta: Double, gamma: Double, dc: DistributionCoeff): Double {
        val A = dc.A; val B = dc.B; val C = dc.C; val D = dc.D; val E = dc.E
        val e = Math.E
        return (1.0 + A * e.pow(B / cos(theta))) *
               (1.0 + C * e.pow(D * gamma) + E * cos2(gamma))
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param T turbidity
     */
    fun getLuminanceDistributionFun(T: Double): DistributionCoeff {
        val mat = Matrix(arrayOf(
                doubleArrayOf( 0.1787, -1.4630),
                doubleArrayOf(-0.3554,  0.4275),
                doubleArrayOf(-0.0227,  5.3251),
                doubleArrayOf( 0.1206, -2.5771),
                doubleArrayOf(-0.0670,  0.3703)
        ))
        val mat2 = Matrix(arrayOf(
                doubleArrayOf( T ),
                doubleArrayOf(1.0)
        ))
        val mat3 = mat * mat2
        return DistributionCoeff(
                mat3.data[0][0],
                mat3.data[1][0],
                mat3.data[2][0],
                mat3.data[3][0],
                mat3.data[4][0]
        )
    }

    /**
     * @param T turbidity
     */
    fun getChromaXDistributionFun(T: Double): DistributionCoeff {
        val mat = Matrix(arrayOf(
                doubleArrayOf(-0.0193, -0.2592),
                doubleArrayOf(-0.0665,  0.0008),
                doubleArrayOf(-0.0004,  0.2125),
                doubleArrayOf(-0.0641, -0.8989),
                doubleArrayOf(-0.0033,  0.0452)
        ))
        val mat2 = Matrix(arrayOf(
                doubleArrayOf( T ),
                doubleArrayOf(1.0)
        ))
        val mat3 = mat * mat2
        return DistributionCoeff(
                mat3.data[0][0],
                mat3.data[1][0],
                mat3.data[2][0],
                mat3.data[3][0],
                mat3.data[4][0]
        )
    }

    /**
     * @param T turbidity
     */
    fun getChromaYDistributionFun(T: Double): DistributionCoeff {
        val mat = Matrix(arrayOf(
                doubleArrayOf(-0.0167, -0.2608),
                doubleArrayOf(-0.0950,  0.0092),
                doubleArrayOf(-0.0079,  0.2102),
                doubleArrayOf(-0.0441, -1.6537),
                doubleArrayOf(-0.0109,  0.0529)
        ))
        val mat2 = Matrix(arrayOf(
                doubleArrayOf( T ),
                doubleArrayOf(1.0)
        ))
        val mat3 = mat * mat2
        return DistributionCoeff(
                mat3[0][0],
                mat3[1][0],
                mat3[2][0],
                mat3[3][0],
                mat3[4][0]
        )
    }

    /**
     * @param T turbidity
     * @param theta_s angle from zenith in radians
     * @return Luminance in candela per metre squared
     */
    fun getAbsoluteZenithLuminance(T: Double, theta_s: Double): Double {
        return (4.0453 * T - 4.9710) * tan((4.0 / 9.0 - T / 120.0) * (PI - 2 * theta_s)) - 0.2155 * T + 2.4192
    }

    /**
     * @param T turbidity
     * @param theta_s angle from zenith in radians
     * @return X value of the chroma in CIEYxy colour space
     */
    fun getZenithChromaX(T: Double, theta_s: Double): Double {
        val mat1 = Matrix(arrayOf(doubleArrayOf(T.pow(2), T, 1.0)))
        val mat2 = Matrix(arrayOf(
                doubleArrayOf( 0.00166, -0.00375,  0.00209, 0.0),
                doubleArrayOf(-0.02903,  0.06377, -0.03202, 0.00394),
                doubleArrayOf( 0.11693, -0.21196,  0.06052, 0.25886)
        ))
        val mat3 = Matrix(arrayOf(
                doubleArrayOf(theta_s.pow(3)),
                doubleArrayOf(theta_s.pow(2)),
                doubleArrayOf(theta_s),
                doubleArrayOf(1.0)
        ))

        return (mat1 * mat2 * mat3)[0][0]
    }

    /**
     * @param T turbidity
     * @param theta_s angle from zenith in radians
     * @return Y value of the chroma in CIEYxy colour space
     */
    fun getZenithChromaY(T: Double, theta_s: Double): Double {
        val mat1 = Matrix(arrayOf(doubleArrayOf(T.pow(2), T, 1.0)))
        val mat2 = Matrix(arrayOf(
                doubleArrayOf( 0.00275, -0.00610,  0.00317, 0.0),
                doubleArrayOf(-0.04214,  0.08970, -0.04153, 0.00516),
                doubleArrayOf( 0.15346, -0.26756,  0.06670, 0.26688)
        ))
        val mat3 = Matrix(arrayOf(
                doubleArrayOf(theta_s.pow(3)),
                doubleArrayOf(theta_s.pow(2)),
                doubleArrayOf(theta_s),
                doubleArrayOf(1.0)
        ))

        return (mat1 * mat2 * mat3)[0][0]
    }


    /** refractive index of air */
    private val RYL_n = 1.0003
    /** number of molecules per unit volume */
    private val RYL_N = 2.545 * (10 pow 25)
    /** depolarisation factor */
    private val RYL_p_n = 0.035

    /**
     * Rayleigh angular scattering
     */
    fun getBetaMofTheta(theta: Double, lambda: Double): Double {
        return (PI_SQR * (RYL_n.pow(2.0) - 1) pow 2.0) / (2 * RYL_N * lambda.pow(4.0)) *
               ((6.0 + 3.0 * RYL_p_n) / (6.0 - 7.0 * RYL_p_n)) * (1.0 + cos2(theta))
    }

    /**
     * Rayleigh total scattering
     */
    fun getBetaM(lambda: Double): Double {
        return (8 * PI_SQR * (RYL_n.pow(2.0) - 1) pow 2.0) / (3 * RYL_N * lambda.pow(4.0)) *
               ((6.0 + 3.0 * RYL_p_n) / (6.0 - 7.0 * RYL_p_n))
    }

    ///////////////////////////////////////////////////////////////////////////

    /** Table 1 */
    private val mieEtaLUT = arrayOf(
            doubleArrayOf(4.192, 4.193, 4.177, 4.147, 4.072),
            doubleArrayOf(3.311, 3.319, 3.329, 3.335, 3.339),
            doubleArrayOf(2.860, 2.868, 2.878, 2.883, 2.888),
            doubleArrayOf(2.518, 2.527, 2.536, 2.542, 2.547),
            doubleArrayOf(1.122, 1.129, 1.138, 1.142, 1.147),
            doubleArrayOf(0.3324, 0.3373, 0.3433, 0.3467, 0.3502),
            doubleArrayOf(0.1644, 0.1682, 0.1730, 0.1757, 0.1785),
            doubleArrayOf(0.1239, 0.1275, 0.1320, 0.1346, 0.1373),
            doubleArrayOf(0.08734, 0.09111, 0.09591, 0.09871, 0.10167),
            doubleArrayOf(0.08242, 0.08652, 0.09179, 0.09488, 0.09816),
            doubleArrayOf(0.08313, 0.08767, 0.09352, 0.09697, 0.10065),
            doubleArrayOf(0.09701, 0.1024, 0.1095, 0.1137, 0.1182),
            doubleArrayOf(0.1307, 0.1368, 0.1447, 0.1495, 0.1566)
    )
    private val mieEtaLUTThetas = doubleArrayOf(1.0, 4.0, 7.0, 10.0, 30.0, 60.0, 80.0, 90.0, 110.0, 120.0, 130.0, 150.0, 180.0)
    private val mieEtaLUTLambdas = doubleArrayOf(400.0, 450.0, 550.0, 650.0, 850.0)
    private val solarSpectralQuantities = arrayOf(
            /* K */doubleArrayOf(0.650393,0.653435,0.656387,0.657828,0.660644,0.662016,0.663365,0.665996,0.667276,0.668532,0.669765,0.670974,0.67216,0.673323,0.674462,0.675578,0.67667,0.677739,0.678784,0.678781,0.679802,0.6808,0.681775,0.681771,0.682722,0.683649,0.683646,0.68455,0.684546,0.685426,0.686282,0.686279,0.687112,0.687108,0.687917,0.687913,0.688699,0.688695,0.688691,0.689453,0.689449),
            /* S_0 */doubleArrayOf(63.4,65.8,94.8,104.8,105.9,96.8,113.9,125.6,125.5,121.3,121.3,113.5,113.1,110.8,106.5,108.8,105.3,104.4,100.0,96.0,95.1,89.1,90.5,90.3,88.4,84.0,85.1,81.9,82.6,84.9,81.3,71.9,74.3,76.4,63.3,71.7,77.0,65.2,47.7,68.6,65.0),
            /* S_1 */doubleArrayOf(38.5,35.0,43.4,46.3,43.9,37.1,36.7,35.9,32.6,27.9,24.3,20.1,16.2,13.2,8.6,6.1,4.2,1.9,0.0,-1.6,-3.5,-3.5,-5.8,-7.2,-8.6,-9.5,-10.9,-10.7,-12.0,-14.0,-13.6,-12.0,-13.3,-12.9,-10.6,-11.6,-12.2,-10.2,-7.8,-11.2,-10.4),
            /* S_2 */doubleArrayOf(3.0,1.2,-1.1,-0.5,-0.7,-1.2,-2.6,-2.9,-2.8,-2.6,-2.6,-1.8,-1.5,-1.3,-1.2,-1.0,-0.5,-0.3,0.0,0.2,0.5,2.1,3.2,4.1,4.7,5.1,6.7,7.3,8.6,9.8,10.2,8.3,9.6,8.5,7.0,7.6,8.0,6.7,5.2,7.4,6.8),
            /* Sun */doubleArrayOf(1655.90,1623.37,2112.75,2588.82,2582.91,2423.23,2676.05,2965.83,3054.54,3005.75,3066.37,2883.04,2871.21,2782.50,2710.06,2723.36,2636.13,2550.38,2506.02,2531.16,2535.59,2513.42,2463.15,2417.32,2368.53,2321.21,2282.77,2233.98,2197.02,2152.67,2109.79,2072.83,2024.04,1987.08,1942.72,1907.24,1862.89,1825.92,0.0,0.0,0.0),
            /* k_o */doubleArrayOf(0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.003,0.006,0.009,0.014,0.021,0.030,0.040,0.048,0.063,0.075,0.085,0.103,0.120,0.120,0.115,0.125,0.120,0.105,0.090,0.079,0.067,0.057,0.048,0.036,0.028,0.023,0.018,0.014,0.011,0.010,0.009,0.007,0.004,0.000),
            /* k_wa */doubleArrayOf(0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.00000,0.01600,0.02400,0.01250,1.00000,0.87000,0.06100,0.00100,0.00001,0.00001,0.00060),
            /* K_g */doubleArrayOf(0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,3.00,0.21,0.00)
    )
    private val mieKLUT = solarSpectralQuantities[0]
    private val mieKLUTLambdas = doubleArrayOf( // also doubles as lambda for solar spectral quantities
            380.0, 390.0, 400.0, 410.0, 420.0, 430.0, 440.0, 450.0, 460.0, 470.0, 480.0, 490.0, 500.0, 510.0, 520.0, 530.0, 540.0, 550.0, 560.0, 570.0, 580.0, 590.0, 600.0, 610.0, 620.0, 630.0, 640.0, 650.0, 660.0, 670.0, 680.0, 690.0, 700.0, 710.0, 720.0, 730.0, 740.0, 750.0, 760.0, 770.0, 780.0
    )
    /**
     * e.g.
     *
     * 0 2 4 5 7 , find 3
     *
     * will return (1, 2), which corresponds value (2, 4) of which input value 3 is in between.
     */
    private fun binarySearchInterval(value: Double, array: DoubleArray): Pair<Int, Int> {
        var low: Int = 0
        var high: Int = array.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midVal = array[mid]

            if (value < midVal)
                high = mid - 1
            else if (value > midVal)
                low = mid + 1
            else
                return Pair(mid, mid)
        }

        val first = Math.max(high, 0)
        val second = Math.min(low, array.size - 1)
        return Pair(first, second)
    }
    /** Just a table lookup with linear interpolation */
    private fun getMieEta(theta: Double, lambda: Double): Double {
        val lambdaIndex = binarySearchInterval(lambda, mieEtaLUTLambdas)
        val lambdaInterval = Pair(mieEtaLUTLambdas[lambdaIndex.first], mieEtaLUTLambdas[lambdaIndex.second])
        val thetaIndex = binarySearchInterval(theta, mieEtaLUTThetas)
        val thetaInterval = Pair(mieEtaLUTThetas[thetaIndex.first], mieEtaLUTThetas[thetaIndex.second])

        val lambdaStep = (lambda - mieEtaLUTLambdas[0]) / (lambdaInterval.second - lambdaInterval.first)
        val thetaStep = (theta - mieEtaLUTThetas[0]) / (thetaInterval.second - thetaInterval.first)

        return interpolateBilinear(lambdaStep, thetaStep,
                mieEtaLUT[thetaIndex.first][lambdaIndex.first],
                mieEtaLUT[thetaIndex.first][lambdaIndex.second],
                mieEtaLUT[thetaIndex.second][lambdaIndex.first],
                mieEtaLUT[thetaIndex.second][lambdaIndex.second]
        )
    }

    private fun getMieK(lambda: Double): Double {
        val lambdaIndex = binarySearchInterval(lambda, mieKLUTLambdas)
        val lambdaInterval = Pair(mieKLUTLambdas[lambdaIndex.first], mieKLUTLambdas[lambdaIndex.second])

        val lambdaStep = (lambda - mieKLUTLambdas[0]) / (lambdaInterval.second - lambdaInterval.first)

        return interpolateLinear(lambdaStep, mieKLUT[lambdaIndex.first], mieKLUT[lambdaIndex.second])
    }

    /**
     * Mie angular scattering
     * @param T turbidity
     * @param theta angle from zenith in radians
     * @param lambda monochromatic light, valid between [400,850] nm
     */
    fun getBetaPofTheta(T: Double, theta: Double, lambda: Double): Double {
        val c = (0.6544 * T - 0.6510) * (10.0 pow -16.0)
        return 0.434 * c * (TWOPI / lambda).pow(2.0) * 0.5 * getMieEta(theta, lambda)
    }

    /**
     * Mie total scattering
     * @param T turbidity
     * @param lambda monochromatic light, valid between [380,780] nm
     */
    fun getBetaP(T: Double, lambda: Double): Double {
        val c = (0.6544 * T - 0.6510) * (10.0 pow -16.0)
        return 0.434 * c * (TWOPI / lambda).pow(2.0) * getMieK(lambda)
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get a solar time (t)
     *
     * @param t_s standard time in decimal hours
     * @param J Julian date [1..365]
     * @param SM standard meridian for the time zone in radians
     * @param L obresver's longitude in radians
     */
    fun t_solarTime(t_s: Double, J: Double, SM: Double = 0.0, L: Double = 0.0): Double {
        return t_s + 0.170 * sin(FOURPI * (J - 80) / 373.0) - 0.129 * sin(TWOPI * (J - 8) / 355.0) + (12 * (SM - L) / PI)
    }

    /**
     * Approximated solar declination
     *
     * @param J Julian date [1..365]
     */
    fun delta_solarDeclination(J: Double): Double {
        return 0.4093 * sin(TWOPI * (J - 81) / 368.0)
    }

    /**
     * @param l obresver's latitude in radians
     * @param delta solar declination in radians
     * @param t solar time in decimal hours
     */
    fun getSolarPosition(l: Double, delta: Double, t: Double): SolarPosition {
        return SolarPosition(
                HALFPI - arcsin(sin(l) * sin(delta) - cos(l) * cos(delta) * cos(PI * t / 12)),
                arctan((-cos(delta) * sin(PI * t / 12)) / (cos(l) * sin(delta) - sin(l) * cos(delta) * cos(PI * t / 12)))
        )
    }

    ///////////////////////////////////////////////////////////////////////////

    private val E = Math.E
    private val HALFPI = 0.5 * Math.PI
    private val PI = Math.PI
    private val TWOPI = 2.0 * Math.PI
    private val FOURPI = 4.0 * Math.PI
    private val PI_SQR = PI * PI
    private fun sin(a: Double) = Math.sin(a)
    private fun cos(a: Double) = Math.cos(a)
    private fun tan(a: Double) = Math.tan(a)
    private fun arcsin(a: Double) = Math.asin(a)
    private fun arctan(a: Double) = Math.atan(a)
    private fun cos2(a: Double) = 0.5 * (1.0 + cos(2 * a))
    private infix fun Double.pow(other: Double) = Math.pow(this, other)
    private infix fun Int.pow(exp: Int): Int {
        var exp = exp
        var base = this
        var result = 1
        while (true) {
            if (exp and 1 != 0)
                result *= base
            exp = exp shr 1
            if (exp.inv() != 0)
                break
            base *= base
        }

        return result
    }
    private fun interpolateLinear(step: Double, start: Double, end: Double): Double {
        if (start == end) {
            return start
        }
        if (step <= 0f) {
            return start
        }
        return if (step >= 1f) {
            end
        }
        else (1f - step) * start + step * end
    }
    /** X and Y starts at top left; X goes down, Y goes right as value increases positively */
    private fun interpolateBilinear(stepX: Double, stepY: Double, topLeft: Double, topRight: Double, bottomLeft: Double, bottomRight: Double): Double {
        val val1 = interpolateLinear(stepX, topLeft, topRight)
        val val2 = interpolateLinear(stepX, bottomLeft, bottomRight)
        return interpolateLinear(stepY, val1, val2)
    }
}