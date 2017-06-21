package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import javax.naming.OperationNotSupportedException

/**
 * Created by minjaesong on 2017-06-17.
 */

typealias XRGB888 = Int

class GdxColorMap {

    constructor(imageFile: FileHandle) {
        val pixmap = Pixmap(imageFile)
        width = pixmap.width
        height = pixmap.height
        is2D = pixmap.height == 1

        data = kotlin.IntArray(pixmap.width * pixmap.height, {
            pixmap.getPixel(it % pixmap.width, it / pixmap.width)
        })

        pixmap.dispose()
    }

    constructor(xrgb888: XRGB888) {
        data = intArrayOf(xrgb888.shl(24) + xrgb888.shl(16) + xrgb888.shl(8) + 255)
        width = 1
        height = 1
        is2D = false
    }

    constructor(gradStart: XRGB888, gradEnd: XRGB888) {
        data = intArrayOf(gradStart.shl(24) + gradStart.shl(16) + gradStart.shl(8) + 255,
                gradEnd.shl(24) + gradEnd.shl(16) + gradEnd.shl(8) + 255)
        width = 1
        height = 2
        is2D = true
    }

    private val data: IntArray
    val width: Int
    val height: Int
    val is2D: Boolean



    fun get(x: Int, y: Int): Color = Color(data[y * width + x])
    operator fun get(x: Int): Color = if (!is2D) throw OperationNotSupportedException("This is 2D color map") else Color(data[x])

    fun getRaw(x: Int, y: Int): RGBA8888 = data[y * width + x]
    fun getRaw(x: Int): RGBA8888 = if (!is2D) throw OperationNotSupportedException("This is 2D color map") else data[x]

}