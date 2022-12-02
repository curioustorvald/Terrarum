package net.torvald.colourutil

import kotlin.math.atan2
import kotlin.math.pow

/**
 * OKLab is a colour space devised by Björn Ottosson. https://bottosson.github.io/posts/oklab/
 *
 * Created by minjaesong on 2022-12-02.
 */
object OKLabUtil {



}

/**
 * @param L Luminosity. Scale depends on the scale of the conversion (if source Lab had 0..100, this value will also be 0..100).
 * @param C Chrominance. Scale depends on the scale of the conversion.
 * @param h Hue in RADIANS (-pi..pi).
 */
data class OKLCh(var L: Float = 0f, val C: Float = 0f, val h: Float = 0f, val alpha: Float = 1f)

data class OKLab(var L: Float = 0f, val a: Float = 0f, val b: Float = 0f, val alpha: Float = 1f) {
    fun toOKLCh(): OKLCh {
        val c = (a*a + b*b).pow(0.5f)
        val h = atan2(a, b)

        return OKLCh(L, c, h, alpha)
    }

    fun toSRGB(): RGB {
        val l_ = L + 0.3963377774f * a + 0.2158037573f * b
        val m_ = L - 0.1055613458f * a - 0.0638541728f * b
        val s_ = L - 0.0894841775f * a - 1.2914855480f * b

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        val lrgb = RGB(
                +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
                -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
                -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
                alpha
        )

        return lrgb.unLinearise()
    }
}

fun CIEXYZ.toOKLab(): OKLab {
    val l = (0.8189330101f * this.X + 0.3618667424f * this.Y - 0.1288597137f * this.Z).pow(0.333333333333f)
    val m = (0.0329845436f * this.X + 0.9293118715f * this.Y + 0.0361456387f * this.Z).pow(0.333333333333f)
    val s = (0.0482003018f * this.X + 0.2643662691f * this.Y + 0.6338517070f * this.Z).pow(0.333333333333f)

    val L = 0.2104542553f*l + 0.7936177850f*m - 0.0040720468f*s
    val a = 1.9779984951f*l - 2.4285922050f*m + 0.4505937099f*s
    val b = 0.0259040371f*l + 0.7827717662f*m - 0.8086757660f*s

    return OKLab(L, a, b, alpha)
}

fun RGB.toOKLab(): OKLab {
    val c = this.linearise()

    val l = 0.4122214708f * c.r + 0.5363325363f * c.g + 0.0514459929f * c.b
    val m = 0.2119034982f * c.r + 0.6806995451f * c.g + 0.1073969566f * c.b
    val s = 0.0883024619f * c.r + 0.2817188376f * c.g + 0.6299787005f * c.b

    val l_ = l.pow(0.3333333333333f)
    val m_ = m.pow(0.3333333333333f)
    val s_ = s.pow(0.3333333333333f)


    val L = 0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_
    val a = 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_
    val b = 0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_

    return OKLab(L, a, b, c.alpha)
}
