package net.torvald.colourutil

import com.jme3.math.FastMath.*
import net.torvald.colourutil.OKHsvUtil.find_cusp
import net.torvald.colourutil.OKHsvUtil.get_Cs
import net.torvald.colourutil.OKHsvUtil.to_ST
import net.torvald.colourutil.OKHsvUtil.toe
import net.torvald.colourutil.OKHsvUtil.toe_inv
import kotlin.math.max
import kotlin.math.min


/**
 * This file contains translated code originally written by Björn Ottosson.
 *
 * Copyright (c) 2021 Björn Ottosson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Created by minjaesong on 2024-04-17.
 */

/**
 * @param h Hue in Radians
 * @param s Saturation `[0-1]`
 * @param v Value `[0-1]`
 */
data class OKHsv(var h: Float, var s: Float, var v: Float)
data class OKHsl(var h: Float, var s: Float, var l: Float)
data class OKLab(var L: Float, var a: Float, var b: Float)
data class OKLch(var H: Float, var C: Float, var L: Float)

fun OKLch.toOKLab() = OKLab(L, C * cos(H), C * sin(H))
fun OKLch.tosRGB() = this.toOKLab().toLinearRGB().unLinearise()

object OKHsvUtil {
    internal data class LC(var L: Float, var C: Float)
    internal data class ST(var S: Float, var T: Float)
    internal data class Cs(var C_0: Float, var C_mid: Float, var C_max: Float)

    private const val FLT_MAX = 1e+37f

    private fun clamp(x: Float, min: Float, max: Float): Float {
        if (x < min) return min
        if (x > max) return max

        return x
    }

    private fun sgn(x: Float): Float {
        return (if (0f < x) 1f else 0f) - (if (x < 0f) 1f else 0f)
    }


    // Finds the maximum saturation possible for a given hue that fits in sRGB
// Saturation here is defined as S = C/L
// a and b must be normalized so a^2 + b^2 == 1
    internal fun compute_max_saturation(a: Float, b: Float): Float {
        // Max saturation will be when one of r, g or b goes below zero.

        // Select different coefficients depending on which component goes below zero first

        val k0: Float
        val k1: Float
        val k2: Float
        val k3: Float
        val k4: Float
        val wl: Float
        val wm: Float
        val ws: Float

        if (-1.88170328f * a - 0.80936493f * b > 1) {
            // Red component
            k0 = +1.19086277f
            k1 = +1.76576728f
            k2 = +0.59662641f
            k3 = +0.75515197f
            k4 = +0.56771245f
            wl = +4.0767416621f
            wm = -3.3077115913f
            ws = +0.2309699292f
        }
        else if (1.81444104f * a - 1.19445276f * b > 1) {
            // Green component
            k0 = +0.73956515f
            k1 = -0.45954404f
            k2 = +0.08285427f
            k3 = +0.12541070f
            k4 = +0.14503204f
            wl = -1.2684380046f
            wm = +2.6097574011f
            ws = -0.3413193965f
        }
        else {
            // Blue component
            k0 = +1.35733652f
            k1 = -0.00915799f
            k2 = -1.15130210f
            k3 = -0.50559606f
            k4 = +0.00692167f
            wl = -0.0041960863f
            wm = -0.7034186147f
            ws = +1.7076147010f
        }

        // Approximate max saturation using a polynomial:
        var S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b

        // Do one step Halley's method to get closer
        // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
        // this should be sufficient for most applications, otherwise do two/three steps
        val k_l = +0.3963377774f * a + 0.2158037573f * b
        val k_m = -0.1055613458f * a - 0.0638541728f * b
        val k_s = -0.0894841775f * a - 1.2914855480f * b

        run {
            val l_ = 1f + S * k_l
            val m_ = 1f + S * k_m
            val s_ = 1f + S * k_s

            val l = l_ * l_ * l_
            val m = m_ * m_ * m_
            val s = s_ * s_ * s_

            val l_dS = 3f * k_l * l_ * l_
            val m_dS = 3f * k_m * m_ * m_
            val s_dS = 3f * k_s * s_ * s_

            val l_dS2 = 6f * k_l * k_l * l_
            val m_dS2 = 6f * k_m * k_m * m_
            val s_dS2 = 6f * k_s * k_s * s_

            val f = wl * l + wm * m + ws * s
            val f1 = wl * l_dS + wm * m_dS + ws * s_dS
            val f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2
            S = S - f * f1 / (f1 * f1 - 0.5f * f * f2)
        }

        return S
    }

    // finds L_cusp and C_cusp for a given hue
// a and b must be normalized so a^2 + b^2 == 1
    internal fun find_cusp(a: Float, b: Float): LC {
        // First, find the maximum saturation (saturation S = C/L)
        val S_cusp = compute_max_saturation(a, b)

        // Convert to linear sRGB to find the first point where at least one of r,g or b >= 1:
        val rgb_at_max = OKLab(1f, S_cusp * a, S_cusp * b).toLinearRGB()
        val L_cusp = cbrt(1f / max(max(rgb_at_max.r, rgb_at_max.g), rgb_at_max.b))
        val C_cusp = L_cusp * S_cusp

        return LC(L_cusp, C_cusp)
    }

    // Finds intersection of the line defined by
// L = L0 * (1 - t) + t * L1;
// C = t * C1;
// a and b must be normalized so a^2 + b^2 == 1
    internal fun find_gamut_intersection(a: Float, b: Float, L1: Float, C1: Float, L0: Float, cusp: LC): Float {
        // Find the intersection for upper and lower half seprately
        var t: Float
        if (((L1 - L0) * cusp.C - (cusp.L - L0) * C1) <= 0f) {
            // Lower half

            t = cusp.C * L0 / (C1 * cusp.L + cusp.C * (L0 - L1))
        }
        else {
            // Upper half

            // First intersect with triangle

            t = cusp.C * (L0 - 1f) / (C1 * (cusp.L - 1f) + cusp.C * (L0 - L1))

            // Then one step Halley's method
            run {
                val dL = L1 - L0
                val dC = C1

                val k_l = +0.3963377774f * a + 0.2158037573f * b
                val k_m = -0.1055613458f * a - 0.0638541728f * b
                val k_s = -0.0894841775f * a - 1.2914855480f * b

                val l_dt = dL + dC * k_l
                val m_dt = dL + dC * k_m
                val s_dt = dL + dC * k_s


                // If higher accuracy is required, 2 or 3 iterations of the following block can be used:
                run {
                    val L = L0 * (1f - t) + t * L1
                    val C = t * C1

                    val l_ = L + C * k_l
                    val m_ = L + C * k_m
                    val s_ = L + C * k_s

                    val l = l_ * l_ * l_
                    val m = m_ * m_ * m_
                    val s = s_ * s_ * s_

                    val ldt = 3 * l_dt * l_ * l_
                    val mdt = 3 * m_dt * m_ * m_
                    val sdt = 3 * s_dt * s_ * s_

                    val ldt2 = 6 * l_dt * l_dt * l_
                    val mdt2 = 6 * m_dt * m_dt * m_
                    val sdt2 = 6 * s_dt * s_dt * s_

                    val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s - 1
                    val r1 = 4.0767416621f * ldt - 3.3077115913f * mdt + 0.2309699292f * sdt
                    val r2 = 4.0767416621f * ldt2 - 3.3077115913f * mdt2 + 0.2309699292f * sdt2

                    val u_r = r1 / (r1 * r1 - 0.5f * r * r2)
                    var t_r = -r * u_r

                    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s - 1
                    val g1 = -1.2684380046f * ldt + 2.6097574011f * mdt - 0.3413193965f * sdt
                    val g2 = -1.2684380046f * ldt2 + 2.6097574011f * mdt2 - 0.3413193965f * sdt2

                    val u_g = g1 / (g1 * g1 - 0.5f * g * g2)
                    var t_g = -g * u_g

                    val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s - 1
                    val b1 = -0.0041960863f * ldt - 0.7034186147f * mdt + 1.7076147010f * sdt
                    val b2 = -0.0041960863f * ldt2 - 0.7034186147f * mdt2 + 1.7076147010f * sdt2

                    val u_b = b1 / (b1 * b1 - 0.5f * b * b2)
                    var t_b = -b * u_b

                    t_r = if (u_r >= 0f) t_r else FLT_MAX
                    t_g = if (u_g >= 0f) t_g else FLT_MAX
                    t_b = if (u_b >= 0f) t_b else FLT_MAX
                    t += min(t_r, min(t_g, t_b))
                }
            }
        }

        return t
    }

    internal fun find_gamut_intersection(a: Float, b: Float, L1: Float, C1: Float, L0: Float): Float {
        // Find the cusp of the gamut triangle
        val cusp: LC = find_cusp(a, b)

        return find_gamut_intersection(a, b, L1, C1, L0, cusp)
    }

    internal fun gamut_clip_preserve_chroma(rgb: RGB): RGB {
        if (rgb.r < 1 && rgb.g < 1 && rgb.b < 1 && rgb.r > 0 && rgb.g > 0 && rgb.b > 0) return rgb

        val lab: OKLab = rgb.linearToOKLab()

        val L: Float = lab.L
        val eps = 0.00001f
        val C: Float = max(eps, sqrt(lab.a * lab.a + lab.b * lab.b))
        val a_: Float = lab.a / C
        val b_: Float = lab.b / C

        val L0 = clamp(L, 0f, 1f)

        val t = find_gamut_intersection(a_, b_, L, C, L0)
        val L_clipped = L0 * (1 - t) + t * L
        val C_clipped = t * C

        return OKLab(L_clipped, C_clipped * a_, C_clipped * b_).toLinearRGB()
    }

    internal fun gamut_clip_project_to_0_5(rgb: RGB): RGB {
        if (rgb.r < 1 && rgb.g < 1 && rgb.b < 1 && rgb.r > 0 && rgb.g > 0 && rgb.b > 0) return rgb

        val lab: OKLab = rgb.linearToOKLab()

        val L: Float = lab.L
        val eps = 0.00001f
        val C: Float = max(eps, sqrt(lab.a * lab.a + lab.b * lab.b))
        val a_: Float = lab.a / C
        val b_: Float = lab.b / C

        val L0 = 0.5.toFloat()

        val t = find_gamut_intersection(a_, b_, L, C, L0)
        val L_clipped = L0 * (1 - t) + t * L
        val C_clipped = t * C

        return OKLab(L_clipped, C_clipped * a_, C_clipped * b_).toLinearRGB()
    }

    internal fun gamut_clip_project_to_L_cusp(rgb: RGB): RGB {
        if (rgb.r < 1 && rgb.g < 1 && rgb.b < 1 && rgb.r > 0 && rgb.g > 0 && rgb.b > 0) return rgb

        val lab: OKLab = rgb.linearToOKLab()

        val L: Float = lab.L
        val eps = 0.00001f
        val C: Float = max(eps, sqrt(lab.a * lab.a + lab.b * lab.b))
        val a_: Float = lab.a / C
        val b_: Float = lab.b / C

        // The cusp is computed here and in find_gamut_intersection, an optimized solution would only compute it once.
        val cusp: LC = find_cusp(a_, b_)

        val L0: Float = cusp.L

        val t = find_gamut_intersection(a_, b_, L, C, L0)

        val L_clipped = L0 * (1 - t) + t * L
        val C_clipped = t * C

        return OKLab(L_clipped, C_clipped * a_, C_clipped * b_).toLinearRGB()
    }

    internal fun gamut_clip_adaptive_L0_0_5(rgb: RGB, alpha: Float): RGB /* alpha = 0.05f */ {
        if (rgb.r < 1 && rgb.g < 1 && rgb.b < 1 && rgb.r > 0 && rgb.g > 0 && rgb.b > 0) return rgb

        val lab: OKLab = rgb.linearToOKLab()

        val L: Float = lab.L
        val eps = 0.00001f
        val C: Float = max(eps, sqrt(lab.a * lab.a + lab.b * lab.b))
        val a_: Float = lab.a / C
        val b_: Float = lab.b / C

        val Ld = L - 0.5f
        val e1: Float = 0.5f + abs(Ld) + alpha * C
        val L0: Float = 0.5f * (1f + sgn(Ld) * (e1 - sqrt(e1 * e1 - 2f * abs(Ld))))

        val t = find_gamut_intersection(a_, b_, L, C, L0)
        val L_clipped = L0 * (1f - t) + t * L
        val C_clipped = t * C

        return OKLab(L_clipped, C_clipped * a_, C_clipped * b_).toLinearRGB()
    }

    internal fun gamut_clip_adaptive_L0_L_cusp(rgb: RGB, alpha: Float): RGB /* alpha = 0.05f */ {
        if (rgb.r < 1 && rgb.g < 1 && rgb.b < 1 && rgb.r > 0 && rgb.g > 0 && rgb.b > 0) return rgb

        val lab: OKLab = rgb.linearToOKLab()

        val L: Float = lab.L
        val eps = 0.00001f
        val C: Float = max(eps, sqrt(lab.a * lab.a + lab.b * lab.b))
        val a_: Float = lab.a / C
        val b_: Float = lab.b / C

        // The cusp is computed here and in find_gamut_intersection, an optimized solution would only compute it once.
        val cusp: LC = find_cusp(a_, b_)

        val Ld: Float = L - cusp.L
        val k: Float = 2f * (if (Ld > 0) 1f - cusp.L else cusp.L)

        val e1: Float = 0.5f * k + abs(Ld) + alpha * C / k
        val L0: Float = cusp.L + 0.5f * (sgn(Ld) * (e1 - sqrt(e1 * e1 - 2f * k * abs(Ld))))

        val t = find_gamut_intersection(a_, b_, L, C, L0)
        val L_clipped = L0 * (1f - t) + t * L
        val C_clipped = t * C

        return OKLab(L_clipped, C_clipped * a_, C_clipped * b_).toLinearRGB()
    }

    internal fun toe(x: Float): Float {
        val k_1 = 0.206f
        val k_2 = 0.03f
        val k_3 = (1f + k_1) / (1f + k_2)
        return 0.5f * (k_3 * x - k_1 + sqrt((k_3 * x - k_1) * (k_3 * x - k_1) + 4 * k_2 * k_3 * x))
    }

    internal fun toe_inv(x: Float): Float {
        val k_1 = 0.206f
        val k_2 = 0.03f
        val k_3 = (1f + k_1) / (1f + k_2)
        return (x * x + k_1 * x) / (k_3 * (x + k_2))
    }

    internal fun to_ST(cusp: OKHsvUtil.LC): ST {
        val L: Float = cusp.L
        val C: Float = cusp.C
        return ST(C / L, C / (1 - L))
    }

    // Returns a smooth approximation of the location of the cusp
// This polynomial was created by an optimization process
// It has been designed so that S_mid < S_max and T_mid < T_max
    internal fun get_ST_mid(a_: Float, b_: Float): ST {
        val S =
            0.11516993f + 1f / (+7.44778970f + 4.15901240f * b_ + a_ * (-2.19557347f + 1.75198401f * b_ + a_ * (-2.13704948f - 10.02301043f * b_
                    + a_ * (-4.24894561f + 5.38770819f * b_ + 4.69891013f * a_
                    )))
                    )

        val T = 0.11239642f + 1f / (+1.61320320f - 0.68124379f * b_
                + a_ * (+0.40370612f + 0.90148123f * b_ + a_ * (-0.27087943f + 0.61223990f * b_ + a_ * (+0.00299215f - 0.45399568f * b_ - 0.14661872f * a_
                ))))

        return ST(S, T)
    }

    internal fun get_Cs(L: Float, a_: Float, b_: Float): Cs {
        val cusp: LC = find_cusp(a_, b_)

        val C_max = find_gamut_intersection(a_, b_, L, 1f, L, cusp)
        val ST_max: ST = to_ST(cusp)

        // Scale factor to compensate for the curved part of gamut shape:
        val k: Float = C_max / min((L * ST_max.S), (1 - L) * ST_max.T)

        var C_mid: Float
        run {
            val ST_mid: ST = get_ST_mid(a_, b_)
            // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
            val C_a: Float = L * ST_mid.S
            val C_b: Float = (1f - L) * ST_mid.T
            C_mid = 0.9f * k * sqrt(sqrt(1f / (1f / (C_a * C_a * C_a * C_a) + 1f / (C_b * C_b * C_b * C_b))))
        }

        var C_0: Float
        run {
            // for C_0, the shape is independent of hue, so ST are constant. Values picked to roughly be the average values of ST.
            val C_a = L * 0.4f
            val C_b = (1f - L) * 0.8f

            // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
            C_0 = sqrt(1f / (1f / (C_a * C_a) + 1f / (C_b * C_b)))
        }

        return Cs(C_0, C_mid, C_max)
    }
}

fun RGB.linearToOKLab(): OKLab {
    val l = 0.4122214708f * this.r + 0.5363325363f * this.g + 0.0514459929f * this.b
    val m = 0.2119034982f * this.r + 0.6806995451f * this.g + 0.1073969566f * this.b
    val s = 0.0883024619f * this.r + 0.2817188376f * this.g + 0.6299787005f * this.b

    val l_ = cbrt(l)
    val m_ = cbrt(m)
    val s_ = cbrt(s)

    return OKLab(
        0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
        1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
        0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_
    )
}

fun OKLab.toLinearRGB(): RGB {
    val l_: Float = this.L + 0.3963377774f * this.a + 0.2158037573f * this.b
    val m_: Float = this.L - 0.1055613458f * this.a - 0.0638541728f * this.b
    val s_: Float = this.L - 0.0894841775f * this.a - 1.2914855480f * this.b

    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_

    return RGB(
        +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    )
}

fun OKHsl.tosRGB(): RGB {
    val h: Float = this.h
    val s: Float = this.s
    val l: Float = this.l

    if (l == 1.0f) {
        return RGB(1f, 1f, 1f)
    }
    else if (l == 0f) {
        return RGB(0f, 0f, 0f)
    }

    val a_: Float = cos(h)
    val b_: Float = sin(h)
    val L = toe_inv(l)

    val cs: OKHsvUtil.Cs = get_Cs(L, a_, b_)
    val C_0: Float = cs.C_0
    val C_mid: Float = cs.C_mid
    val C_max: Float = cs.C_max

    val mid = 0.8f
    val mid_inv = 1.25f

    val C: Float
    val t: Float
    val k_0: Float
    val k_1: Float
    val k_2: Float

    if (s < mid) {
        t = mid_inv * s

        k_1 = mid * C_0
        k_2 = (1f - k_1 / C_mid)

        C = t * k_1 / (1f - k_2 * t)
    }
    else {
        t = (s - mid) / (1 - mid)

        k_0 = C_mid
        k_1 = (1f - mid) * C_mid * C_mid * mid_inv * mid_inv / C_0
        k_2 = (1f - (k_1) / (C_max - C_mid))

        C = k_0 + t * k_1 / (1f - k_2 * t)
    }

    val rgb = OKLab(L, C * a_, C * b_).toLinearRGB()
    return rgb.unLinearise()
}

fun RGB.sRGBtoOKHsl(): OKHsl {
    val lab: OKLab = this.linearise().linearToOKLab()

    val C: Float = sqrt(lab.a * lab.a + lab.b * lab.b)
    val a_: Float = lab.a / C
    val b_: Float = lab.b / C

    val L: Float = lab.L
    val h: Float = 0.5f + 0.5f * atan2(-lab.b, -lab.a) / PI

    val cs: OKHsvUtil.Cs = get_Cs(L, a_, b_)
    val C_0: Float = cs.C_0
    val C_mid: Float = cs.C_mid
    val C_max: Float = cs.C_max

    // Inverse of the interpolation in okhsl_to_srgb:
    val mid = 0.8f
    val mid_inv = 1.25f

    val s: Float
    if (C < C_mid) {
        val k_1 = mid * C_0
        val k_2 = (1f - k_1 / C_mid)

        val t = C / (k_1 + k_2 * C)
        s = t * mid
    }
    else {
        val k_0 = C_mid
        val k_1 = (1f - mid) * C_mid * C_mid * mid_inv * mid_inv / C_0
        val k_2 = (1f - (k_1) / (C_max - C_mid))

        val t = (C - k_0) / (k_1 + k_2 * (C - k_0))
        s = mid + (1f - mid) * t
    }

    val l = toe(L)
    return OKHsl(h, s, l)
}


fun OKHsv.tosRGB(): RGB {
    val h = this.h
    val s = this.s
    val v = this.v

    val a_: Float = cos(h)
    val b_: Float = sin(h)

    val cusp: OKHsvUtil.LC = find_cusp(a_, b_)
    val ST_max: OKHsvUtil.ST = to_ST(cusp)
    val S_max: Float = ST_max.S
    val T_max: Float = ST_max.T
    val S_0 = 0.5f
    val k = 1 - S_0 / S_max

    // first we compute L and V as if the gamut is a perfect triangle:

    // L, C when v==1:
    val L_v = 1 - s * S_0 / (S_0 + T_max - T_max * k * s)
    val C_v = s * T_max * S_0 / (S_0 + T_max - T_max * k * s)

    var L = v * L_v
    var C = v * C_v

    // then we compensate for both toe and the curved top part of the triangle:
    val L_vt = toe_inv(L_v)
    val C_vt = C_v * L_vt / L_v

    val L_new = toe_inv(L)
    C = C * L_new / L
    L = L_new

    val rgb_scale = OKLab(L_vt, a_ * C_vt, b_ * C_vt).toLinearRGB()
    val scale_L = cbrt(1f / max(max(rgb_scale.r, rgb_scale.g), max(rgb_scale.b, 0f)))

    L = L * scale_L
    C = C * scale_L

    val rgb = OKLab(L, C * a_, C * b_).toLinearRGB()
    return rgb.unLinearise()
}

fun RGB.sRGBtoOKHsv(): OKHsv {
    val lab: OKLab = this.linearise().linearToOKLab()

    var C: Float = sqrt(lab.a * lab.a + lab.b * lab.b)
    val a_: Float = lab.a / C
    val b_: Float = lab.b / C

    var L: Float = lab.L
    val h: Float = 0.5f + 0.5f * atan2(-lab.b, -lab.a) / PI

    val cusp: OKHsvUtil.LC = find_cusp(a_, b_)
    val ST_max: OKHsvUtil.ST = to_ST(cusp)
    val S_max: Float = ST_max.S
    val T_max: Float = ST_max.T
    val S_0 = 0.5f
    val k = 1 - S_0 / S_max

    // first we find L_v, C_v, L_vt and C_vt
    val t = T_max / (C + L * T_max)
    val L_v = t * L
    val C_v = t * C

    val L_vt = toe_inv(L_v)
    val C_vt = C_v * L_vt / L_v

    // we can then use these to invert the step that compensates for the toe and the curved top part of the triangle:
    val rgb_scale = OKLab(L_vt, a_ * C_vt, b_ * C_vt).toLinearRGB()
    val scale_L = cbrt(1f / max(max(rgb_scale.r, rgb_scale.g), max(rgb_scale.b, 0f)))

    L = L / scale_L
    C = C / scale_L

    C = C * toe(L) / L
    L = toe(L)

    // we can now compute v and s:
    val v = L / L_v
    val s = (S_0 + T_max) * C_v / ((T_max * S_0) + T_max * k * C_v)

    return OKHsv(h, s, v)
}
