package net.torvald.unsafe

/**
 * Created by minjaesong on 2024-07-22.
 */
class UnsafeInt2D(width: Int, height: Int) {

    val width = width.toLong()
    val height = height.toLong()

    private val ptr = UnsafeHelper.allocate(4L * width * height)

    private inline fun toIndex(y: Int, x: Int) = width * y + x

    operator fun set(y: Int, x: Int, value: Int) = ptr.setInt(toIndex(y, x), value)
    operator fun get(y: Int, x: Int) = ptr.getInt(toIndex(y, x))

    fun destroy() = ptr.destroy()
    val destroyed: Boolean
        get() = ptr.destroyed

}