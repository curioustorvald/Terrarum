package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import net.torvald.UnsafeHelper
import net.torvald.UnsafePtr
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.CommonResourcePool

/**
 * Blit is a display adapter that operates in pixel-space, with resolution of 224x320 with bit depth of one.
 *
 * This Blit is inspired by the real-world Blit terminal from 1982 by Bell Labs.
 *
 * @link https://en.wikipedia.org/wiki/Blit_(computer_terminal)
 *
 * Created by minjaesong on 2019-07-22.
 */
class BLIT {

    private val framebuffer = UnsafeHelper.allocate(W.toLong() * H)

    var scrollOffsetX = 0
    var scrollOffsetY = 0
    var textCursorPos = 0

    // each pixel is a byte. I know, 7 bits wasted, but whatever.

    // any conversion to texture/GDX pixmap/etc must be done by other system.
    // at least you can memcpy() them using UnsafeHelper
    // TODO test memcpy() over pixmap and native memory

    private fun toAddr(x: Int, y: Int) = (W * y + x) % framebuffer.size

    fun drawPict(x: Int, y: Int, bytes: ByteArray, width: Int) {
        for (yy in 0L until bytes.size / width) {
            val writeAddr = toAddr(x, y)

            UnsafeHelper.memcpyRaw(
                    bytes, UnsafeHelper.getArrayOffset(bytes) + yy * width,
                    null, framebuffer.ptr + writeAddr,
                    width.toLong()
            )
        }
    }

    fun drawLetter(px: Int, py: Int, char: Int) {
        for (yy in 0L until 13L) {
            UnsafeHelper.memcpy(
                    fontRom.ptr + (FONTROMW * (char / FONTROMCOLS) * FONTH * yy) + (char % FONTROMCOLS) * FONTW,
                    toAddr(px, py),
                    FONTW.toLong()
            )
        }
    }


    /**
     * Notes:
     *
     * - The font ROM will not be redefine-able. Just draw your shape on the pixel space.
     */
    companion object {
        const val W = 240
        const val H = 320

        const val FONTW = 6
        const val FONTH = 10

        const val FONTROMW = 192
        const val FONTROMH = 80

        const val FONTROMCOLS = FONTROMW / FONTW
        const val FONTROMROWS = FONTROMH / FONTH

        const val TEXT_OFFSET_X = 0 // hand-calculated value
        const val TEXT_OFFSET_Y = 4 // hand-calculated value
        // so, y=0..3 and y=317..320 won't be touched by the text drawing

        init {
            // load common font rom
            CommonResourcePool.addToLoadingList("dwarventech.computers.blit.fontrom") {
                // TODO
            }
        }

        private val fontRom = CommonResourcePool.getAs<UnsafePtr>("dwarventech.computers.blit.fontrom")
    }
}