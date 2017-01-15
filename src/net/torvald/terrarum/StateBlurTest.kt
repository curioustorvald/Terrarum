package net.torvald.terrarum

import net.torvald.terrarum.gameactors.floor
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameworld.toUint
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.nio.ByteOrder


/**
 * Software rendering test for blur
 *
 * Created by SKYHi14 on 2017-01-12.
 */
class StateBlurTest : BasicGameState() {

    /** Warning: the image must have a bit depth of 32! (use 32-bit PNG or TGA) */
    private val testImage = Image("./assets/testimage_resized.png")
    private val bluredImage = ImageBuffer(testImage.width, testImage.height)

    override fun init(gc: GameContainer, sbg: StateBasedGame) {
        testImage.flushPixelData()

        System.arraycopy(
                testImage.texture.textureData, 0,
                bluredImage.rgba, 0, testImage.texture.textureData.size
        )
        kotlin.repeat(3, { fastBoxBlur(bluredImage, 3) })
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")

        /*System.arraycopy(
                testImage.texture.textureData, 0,
                bluredImage.rgba, 0, testImage.texture.textureData.size
        )*/
        //fastBoxBlur(testImage, bluredImage, 3)
        //fastBoxBlur(bluredImage, 3)
        //fastBoxBlur(bluredImage, 3)
    }

    override fun getID() = Terrarum.STATE_ID_TEST_BLUR

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        g.background = Color(0x404040)
        g.drawImage(bluredImage.image,
                Terrarum.WIDTH.minus(testImage.width).div(2f).floor(),
                Terrarum.HEIGHT.minus(testImage.height).div(2f).floor()
        )
        g.flush()
    }

    private val isLE: Boolean
        get() = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    /** three iterations of box blur \simeq gaussian blur */
    fun fastBoxBlur(from: Image, to: ImageBuffer, radius: Int) {

        /** 0xRRGGBBAA */
        fun getPixelData(index: Int): Int {
            val r = from.texture.textureData[4 * index + if (isLE) 0 else 2].toUint()
            val g = from.texture.textureData[4 * index + 1].toUint()
            val b = from.texture.textureData[4 * index + if (isLE) 2 else 0].toUint()

            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                return r.shl(16) or g.shl(8) or b
            }
            else {
                return b.shl(16) or g.shl(8) or r
            }
        }

        /** alpha will be passed through */
        fun setPixelData(index: Int, value: Int) {
            val r = value.ushr(24).and(0xff)
            val g = value.ushr(16).and(0xff)
            val b = value.ushr(8).and(0xff)

            to.rgba[4 * index + if (isLE) 0 else 2] = r.toByte()
            to.rgba[4 * index + 1] = g.toByte()
            to.rgba[4 * index + if (isLE) 2 else 0] = b.toByte()
        }

        if (radius < 1) {
            return
        }
        val w = to.texWidth
        val h = to.texHeight
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var p1: Int
        var p2: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        val vmax = IntArray(Math.max(w, h))

        //img.getPixels(pix, 0, w, 0, 0, w, h)

        val dv = IntArray(256 * div)
        i = 0
        while (i < 256 * div) {
            dv[i] = i / div
            i++
        }

        yi = 0
        yw = yi

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            i = -radius
            while (i <= radius) {
                p = getPixelData(yi + Math.min(wm, Math.max(i, 0)))
                rsum += p and 0xff0000 shr 16
                gsum += p and 0x00ff00 shr 8
                bsum += p and 0x0000ff
                i++
            }
            x = 0
            while (x < w) {

                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                    vmax[x] = Math.max(x - radius, 0)
                }
                p1 = getPixelData(yw + vmin[x])
                p2 = getPixelData(yw + vmax[x])

                rsum += (p1 and 0xff0000) - (p2 and 0xff0000) shr 16
                gsum += (p1 and 0x00ff00) - (p2 and 0x00ff00) shr 8
                bsum += (p1 and 0x0000ff) - (p2 and 0x0000ff)
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
                i++
            }
            yi = x
            y = 0
            while (y < h) {
                setPixelData(yi, dv[rsum].shl(24) or dv[gsum].shl(16) or dv[bsum].shl(8))

                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * w
                    vmax[y] = Math.max(y - radius, 0) * w
                }
                p1 = x + vmin[y]
                p2 = x + vmax[y]

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]

                yi += w
                y++
            }
            x++
        }
    }

    fun fastBoxBlur(img: ImageBuffer, radius: Int) {

        /** 0xRRGGBBAA */
        fun getPixelData(index: Int): Int {
            val r = img.rgba[4 * index].toUint()
            val g = img.rgba[4 * index + 1].toUint()
            val b = img.rgba[4 * index + 2].toUint()

            return r.shl(16) or g.shl(8) or b
        }

        /** alpha will be passed through */
        fun setPixelData(index: Int, value: Int) {
            val r = value.ushr(24).and(0xff)
            val g = value.ushr(16).and(0xff)
            val b = value.ushr(8).and(0xff)

            img.rgba[4 * index] = r.toByte()
            img.rgba[4 * index + 1] = g.toByte()
            img.rgba[4 * index + 2] = b.toByte()
        }

        if (radius < 1) {
            return
        }
        val w = img.texWidth
        val h = img.texHeight
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var p1: Int
        var p2: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        val vmax = IntArray(Math.max(w, h))

        //img.getPixels(pix, 0, w, 0, 0, w, h)

        val dv = IntArray(256 * div)
        i = 0
        while (i < 256 * div) {
            dv[i] = i / div
            i++
        }

        yi = 0
        yw = yi

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            i = -radius
            while (i <= radius) {
                p = getPixelData(yi + Math.min(wm, Math.max(i, 0)))
                rsum += p and 0xff0000 shr 16
                gsum += p and 0x00ff00 shr 8
                bsum += p and 0x0000ff
                i++
            }
            x = 0
            while (x < w) {

                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                    vmax[x] = Math.max(x - radius, 0)
                }
                p1 = getPixelData(yw + vmin[x])
                p2 = getPixelData(yw + vmax[x])

                rsum += (p1 and 0xff0000) - (p2 and 0xff0000) shr 16
                gsum += (p1 and 0x00ff00) - (p2 and 0x00ff00) shr 8
                bsum += (p1 and 0x0000ff) - (p2 and 0x0000ff)
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
                i++
            }
            yi = x
            y = 0
            while (y < h) {
                setPixelData(yi, dv[rsum].shl(24) or dv[gsum].shl(16) or dv[bsum].shl(8))

                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * w
                    vmax[y] = Math.max(y - radius, 0) * w
                }
                p1 = x + vmin[y]
                p2 = x + vmax[y]

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]

                yi += w
                y++
            }
            x++
        }
    }
}
