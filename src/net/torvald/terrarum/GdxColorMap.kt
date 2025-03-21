package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import net.torvald.gdx.graphics.Cvec

/**
 * Created by minjaesong on 2017-06-17.
 */

class GdxColorMap {

    constructor(imageFile: FileHandle) {
        App.printdbg(this, "Loading colormap from ${imageFile.name()}")
        printStackTrace(this)

        val pixmap = Pixmap(imageFile)
        width = pixmap.width
        height = pixmap.height
        is2D = pixmap.height > 1

        dataRaw = kotlin.IntArray(pixmap.width * pixmap.height) {
            pixmap.getPixel(it % pixmap.width, it / pixmap.width)
        }
        dataGdxColor = dataRaw.map { Color(it) }.toTypedArray()
        dataCvec = dataRaw.map { Cvec(it) }.toTypedArray()

        pixmap.dispose()
    }

    constructor(pixmap: Pixmap, disposePixmap: Boolean = true) {
        width = pixmap.width
        height = pixmap.height
        is2D = pixmap.height > 1

        dataRaw = kotlin.IntArray(pixmap.width * pixmap.height) {
            pixmap.getPixel(it % pixmap.width, it / pixmap.width)
        }
        dataGdxColor = dataRaw.map { Color(it) }.toTypedArray()
        dataCvec = dataRaw.map { Cvec(it) }.toTypedArray()

        if (disposePixmap) pixmap.dispose()
    }

    constructor(color: Color) {
        dataRaw = intArrayOf(color.toIntBits())
        dataGdxColor = dataRaw.map { Color(it) }.toTypedArray()
        dataCvec = dataRaw.map { Cvec(it) }.toTypedArray()
        width = 1
        height = 1
        is2D = false
    }

    constructor(width: Int, height: Int, vararg colours: Color) {
        dataRaw = colours.map { it.toIntBits() }.toIntArray()
        dataGdxColor = dataRaw.map { Color(it) }.toTypedArray()
        dataCvec = dataRaw.map { Cvec(it) }.toTypedArray()
        this.width = width
        this.height = height
        is2D = (height > 1)
    }

    constructor(width: Int, height: Int, colours: List<Color>) {
        dataRaw = colours.map { it.toIntBits() }.toIntArray()
        dataGdxColor = dataRaw.map { Color(it) }.toTypedArray()
        dataCvec = dataRaw.map { Cvec(it) }.toTypedArray()
        this.width = width
        this.height = height
        is2D = (height > 1)
    }

    private val dataRaw: IntArray
    private val dataGdxColor: Array<Color>
    private val dataCvec: Array<Cvec>
    val width: Int
    val height: Int
    val is2D: Boolean



    fun get(x: Int, y: Int): Color = dataGdxColor[y * width + x]
    operator fun get(x: Int): Color = if (is2D) throw UnsupportedOperationException("This is 2D color map") else dataGdxColor[x]

    fun getCvec(x: Int, y: Int): Cvec = dataCvec[y * width + x]
    fun getCvec(x: Int): Cvec = if (is2D) throw UnsupportedOperationException("This is 2D color map") else dataCvec[x]


    fun getRaw(x: Int, y: Int): RGBA8888 = dataRaw[y * width + x]
    fun getRaw(x: Int): RGBA8888 = if (is2D) throw UnsupportedOperationException("This is 2D color map") else dataRaw[x]

    //fun getAsCvec(x: Int, y: Int): Cvec = dataCvec[y * width + x]

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append("ColorMap ${width}x$height:\n")

        var yi = 0
        var xi = 0
        for (y in ((0 until height).take(2) + (0 until height).toList().takeLast(2)).distinct()) {

            if (y - yi > 1) {
                sb.append(when (width) {
                    in 1..4 -> ".......... ".repeat(width) + '\n'
                    else -> ".......... .......... ... .......... .......... \n"
                }
                )
            }

            for (x in ((0 until width).take(2) + (0 until width).toList().takeLast(2)).distinct()) {
                if (x - xi > 1) {
                    sb.append("... ")
                }

                sb.append("0x")
                sb.append(getRaw(x, y).toLong().and(0xFFFFFFFF).toString(16).toUpperCase().padStart(8, '0'))
                sb.append(' ')

                xi = x
            }

            sb.append('\n')

            yi = y
        }

        return sb.toString()
    }
}