package net.torvald.unsafe

import kotlin.math.absoluteValue

/**
 * Created by minjaesong on 2024-07-22.
 */
class UnsafeLong2D(width: Int, height: Int) {

    val width = width.toLong()
    val height = height.toLong()

    private val WORDSIZE = 8L

    private val ptr = UnsafeHelper.allocate(WORDSIZE * width * height)

    private inline fun toIndex(y: Int, x: Int) = width * y + x

    operator fun set(y: Int, x: Int, value: Long) = ptr.setLong(toIndex(y, x), value)
    operator fun get(y: Int, x: Int) = ptr.getLong(toIndex(y, x))

    fun destroy() = ptr.destroy()
    val destroyed: Boolean
        get() = ptr.destroyed

    /**
     * The contents of the "outer" region are undefined.
     *
     * If any of the displacement is equal to or larger than the dimension, nothing will be done.
     *
     * @param xoff positive values to shift to right, negative values to shift to left
     * @param yoff positive values to shift to down, negative values to shift to up
     *
     * @return `true` if the displacement is smaller than the size, `false` otherwise
     */
    fun shift(xoff: Int, yoff: Int): Boolean {
        val xoff = xoff.toLong()
        val yoff = yoff.toLong()

        val xsize = width - xoff.absoluteValue
        val ysize = height - yoff.absoluteValue

        if (xsize >= width || ysize >= height) return false

        val ys = if (yoff < 0) 0 until height - ysize else height-1 downTo yoff
        val linesize = WORDSIZE * width
        val unsafe = UnsafeHelper.unsafe
        val linebuf = UnsafeHelper.allocate(WORDSIZE * xsize)
        for (y in ys) {
            // copy
            unsafe.copyMemory(
                ptr.ptr + (y - yoff) * linesize - xoff.coerceAtMost(0L) * WORDSIZE,
                linebuf.ptr,
                WORDSIZE * xsize
            )
            // paste
            unsafe.copyMemory(
                linebuf.ptr,
                ptr.ptr + (y) * linesize + xoff.coerceAtLeast(0L) * WORDSIZE,
                WORDSIZE * xsize
            )
        }
        return true
    }

}