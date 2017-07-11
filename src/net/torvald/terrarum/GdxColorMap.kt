package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import javax.naming.OperationNotSupportedException

/**
 * Created by minjaesong on 2017-06-17.
 */

class GdxColorMap {

    constructor(imageFile: FileHandle) {
        val pixmap = Pixmap(imageFile)
        width = pixmap.width
        height = pixmap.height
        is2D = pixmap.height > 1

        data = kotlin.IntArray(pixmap.width * pixmap.height, {
            pixmap.getPixel(it % pixmap.width, it / pixmap.width)
        })


        println("[GdxColorMap] Loading colormap from ${imageFile.name()}; PixmapFormat: ${pixmap.format}; Dimension: $width x $height")


        pixmap.dispose()
    }

    constructor(color: Color) {
        data = intArrayOf(color.toIntBits())
        width = 1
        height = 1
        is2D = false
    }

    constructor(gradStart: Color, gradEnd: Color) {
        data = intArrayOf(gradStart.toIntBits(), gradEnd.toIntBits())
        width = 1
        height = 2
        is2D = true
    }

    private val data: IntArray
    val width: Int
    val height: Int
    val is2D: Boolean



    fun get(x: Int, y: Int): Color = Color(data[y * width + x])
    operator fun get(x: Int): Color = if (is2D) throw OperationNotSupportedException("This is 2D color map") else Color(data[x])

    fun getRaw(x: Int, y: Int): RGBA8888 = data[y * width + x]
    fun getRaw(x: Int): RGBA8888 = if (is2D) throw OperationNotSupportedException("This is 2D color map") else data[x]

}