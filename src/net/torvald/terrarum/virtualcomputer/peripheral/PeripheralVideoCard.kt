package net.torvald.terrarum.virtualcomputer.peripheral

import net.torvald.terrarum.gameactors.DecodeTapestry
import net.torvald.terrarum.gameactors.ai.toLua
import net.torvald.terrarum.getPixel
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*
import java.util.*

/**
 * Created by SKYHi14 on 2017-02-08.
 */
class PeripheralVideoCard(val termW: Int = 40, val termH: Int = 25) :
        Peripheral("ppu") {
    companion object {
        val blockW = 8
        val blockH = 8

        /**
         * Converts consecutive lua table indexed from 1 as IntArray.
         * The lua table must not contain any nils in the sequence.
         */
        fun LuaTable.toIntArray(): IntArray {
            val arr = IntArray(this.keyCount())
            var k = 1
            while (true) {
                if (this[k].isnil()) break

                arr[k - 1] = this[k].checkint()
                k += 1
            }

            return arr
        }
    }

    val width = termW * blockW
    val height = termH *  blockH

    val vram = VRAM(width, height, 64)
    val frameBuffer = ImageBuffer(width, height)
    val frameBufferImage = frameBuffer.image

    // hard-coded 8x8
    var fontRom = Array<IntArray>(256, { Array<Int>(blockH, { 0 }).toIntArray() })

    init {
        // build it for first time
        resetTextRom()

        frameBufferImage.filter = Image.FILTER_NEAREST
    }

    val CLUT = VRAM.CLUT
    val coloursCount = CLUT.size

    fun buildFontRom(ref: String) {
        // load font rom out of TGA
        val imageRef = Image(ref)
        val image = imageRef.texture.textureData
        val imageWidth = imageRef.width

        for (i in 0..255) {
            for (y in 0..blockH - 1) {
                // letter mirrored horizontally!
                var scanline = 0
                for (x in 0..blockW - 1) {
                    val subX = i % 16
                    val subY = i / 16
                    val bit = image[4 * ((subY * blockH + y) * imageWidth + blockW * subX + x) + 3] != 0.toByte()
                    if (bit) scanline = scanline or (1 shl x)
                }

                fontRom[i][y] = scanline
            }
        }
    }

    override fun loadLib(globals: Globals) {
        globals["ppu"] = LuaTable()
        globals["ppu"]["setForeColor"] = SetForeColor(this)
        globals["ppu"]["getForeColor"] = GetForeColor(this)
        globals["ppu"]["setBackColor"] = SetBackColor(this)
        globals["ppu"]["getBackColor"] = GetBackColor(this)
        globals["ppu"]["emitChar"] = EmitChar(this)
        globals["ppu"]["clearAll"] = ClearAll(this)
        globals["ppu"]["clearBack"] = ClearBackground(this)
        globals["ppu"]["clearFore"] = ClearForeground(this)
    }

    private val spriteBuffer = ImageBuffer(VSprite.width, VSprite.height)

    fun render(g: Graphics) {
        fun VSprite.render() {
            val h = VSprite.height
            val w = VSprite.width
            if (rotation and 1 == 0) { // deg 0, 180
                (if (rotation == 0 && !vFlip || rotation == 2 && vFlip) 0..h-1 else h-1 downTo 0).forEachIndexed { ordY, y ->
                    (if (rotation == 0 && !hFlip || rotation == 2 && hFlip) 0..w-1 else w-1 downTo 0).forEachIndexed { ordX, x ->
                        val pixelData = data[y].ushr(2 * x).and(0b11)
                        val col = getColourFromPalette(pixelData)
                        spriteBuffer.setRGBA(ordX, ordY, col.red, col.green, col.blue, col.alpha)
                    }
                }
            }
            else { // deg 90, 270
                (if (rotation == 3 && !hFlip || rotation == 1 && hFlip) 0..w-1 else w-1 downTo 0).forEachIndexed { ordY, y ->
                    (if (rotation == 3 && !vFlip || rotation == 1 && vFlip) h-1 downTo 0 else 0..h-1).forEachIndexed { ordX, x ->
                        val pixelData = data[y].ushr(2 * x).and(0b11)
                        val col = getColourFromPalette(pixelData)
                        spriteBuffer.setRGBA(ordY, ordX, col.red, col.green, col.blue, col.alpha)
                    }
                }
            }
        }


        System.arraycopy(vram.background.rgba, 0, frameBuffer.rgba, 0, vram.background.rgba.size)
        vram.sprites.forEach {
            if (it.isBackground) {
                it.render()
                frameBuffer.softwareRender(spriteBuffer, it.posX, it.posY)
            }
        }
        frameBuffer.softwareRender(vram.foreground, 0, 0)
        vram.sprites.forEach {
            if (!it.isBackground) {
                it.render()
                frameBuffer.softwareRender(spriteBuffer, it.posX, it.posY)
            }
        }


        val img = frameBuffer.image
        img.filter = Image.FILTER_NEAREST
        g.drawImage(img.getScaledCopy(2f), 0f, 0f)

        img.destroy()
    }

    fun ImageBuffer.softwareRender(other: ImageBuffer, posX: Int, posY: Int) {
        for (y in 0..other.height - 1) {
            for (x in 0..other.width - 1) {
                val ix = posX + x
                val iy = posY + y
                if (ix >= 0 && iy >= 0 && ix < this.width && iy < this.height) {
                    if (other.rgba[4 * (y * other.width + x) + 3] != 0.toByte()) { // if not transparent
                        this.rgba[4 * (iy * this.texWidth + ix) + 0] = other.rgba[4 * (y * other.texWidth + x) + 0]
                        this.rgba[4 * (iy * this.texWidth + ix) + 1] = other.rgba[4 * (y * other.texWidth + x) + 1]
                        this.rgba[4 * (iy * this.texWidth + ix) + 2] = other.rgba[4 * (y * other.texWidth + x) + 2]
                        this.rgba[4 * (iy * this.texWidth + ix) + 3] = other.rgba[4 * (y * other.texWidth + x) + 3]
                    }
                }
            }
        }
    }

    private var foreColor = 49 // white
    private var backColor = 64 // transparent

    fun drawChar(c: Char, x: Int, y: Int, colFore: Int = foreColor, colBack: Int = backColor) {
        val glyph = fontRom[c.toInt()]
        val fore = CLUT[colFore]
        val back = CLUT[colBack]

        // software render
        for (gy in 0..blockH - 1) {
            for (gx in 0..blockW - 1) {
                val glyAlpha = glyph[gy].ushr(gx).and(1)

                if (glyAlpha != 0) {
                    vram.foreground.setRGBA(x * blockW + gx, y * blockH + gy, fore.red, fore.green, fore.blue, fore.alpha)
                }
                else {
                    vram.foreground.setRGBA(x * blockW + gx, y * blockH + gy, back.red, back.green, back.blue, back.alpha)
                }
            }
        }
    }


    fun clearBackground() {
        for (i in 0..width * height - 1) {
            vram.background.rgba[i] = if (i % 4 == 3) 0xFF.toByte() else 0x00.toByte()
        }
    }

    fun clearForeground() {
        for (i in 0..width * height - 1) {
            vram.foreground.rgba[i] = 0x00.toByte()
        }
    }

    fun clearAll() {
        for (i in 0..width * height - 1) {
            vram.background.rgba[i] = if (i % 4 == 3) 0xFF.toByte() else 0x00.toByte()
            vram.foreground.rgba[i] = 0x00.toByte()
        }
    }

    fun getSprite(index: Int) = vram.sprites[index]


    /**
     * Array be like, in binary; notice that glyphs are flipped horizontally:
     * ...
     * 00011000
     * 00011100
     * 00011000
     * 00011000
     * 00011000
     * 00011000
     * 01111111
     * 00000000
     * 00111110
     * 01100011
     * 01100000
     * 00111111
     * 00000011
     * 00000011
     * 01111111
     * 00000000
     * ...
     */
    fun setTextRom(data: Array<Int>) {
        for (i in 0..255) {
            for (y in 0..blockH - 1) {
                // letter mirrored horizontally!
                fontRom[i][y] = data[blockH * i + y]
            }
        }
    }

    fun resetTextRom() {
        buildFontRom("./assets/graphics/fonts/milky.tga")
    }


    class SetForeColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.foreColor = arg.checkint()
            return LuaValue.NONE
        }
    }
    class GetForeColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.foreColor.toLua()
        }
    }
    class SetBackColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.backColor = arg.checkint()
            return LuaValue.NONE
        }
    }
    class GetBackColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.backColor.toLua()
        }
    }
    class EmitChar(val videoCard: PeripheralVideoCard) : ThreeArgFunction() {
        /** emitChar(char, x, y) */
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            videoCard.drawChar(arg1.checkint().toChar(), arg2.checkint(), arg3.checkint())
            return LuaValue.NONE
        }
    }
    class ClearAll(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            videoCard.clearAll()
            return LuaValue.NONE
        }
    }
    class ClearBackground(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            videoCard.clearBackground()
            return LuaValue.NONE
        }
    }
    class ClearForeground(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            videoCard.clearForeground()
            return LuaValue.NONE
        }
    }

    /////////////
    // Sprites //
    /////////////

    fun composeSpriteObject(spriteIndex: Int) : LuaValue {
        val sprite = vram.sprites[spriteIndex]
        val t = LuaTable()

        t["getColFromPal"] = SpriteGetColFromPal(sprite)
        t["setPixel"] = SpriteSetPixel(sprite)
        t["setPalSet"] = SpriteSetPaletteSet(sprite)
        t["setLine"] = SpriteSetLine(sprite)
        t["setAll"] = SpriteSetAll(sprite)

        return t
    }

    private class SpriteGetColFromPal(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return when (arg.checkint()) {
                0 -> sprite.pal0.toLua()
                1 -> sprite.pal1.toLua()
                2 -> sprite.pal2.toLua()
                3 -> sprite.pal3.toLua()
                else -> throw IndexOutOfBoundsException("Palette size: 4, input: ${arg.checkint()}")
            }
        }
    }

    private class SpriteSetPixel(val sprite: VSprite) : ThreeArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            sprite.setPixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            return LuaValue.NONE
        }
    }

    private class SpriteSetPaletteSet(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            sprite.setPaletteSet(arg(1).checkint(), arg(2).checkint(), arg(3).checkint(), arg(4).checkint())
            return LuaValue.NONE
        }
    }

    private class SpriteSetLine(val sprite: VSprite) : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            sprite.setLine(arg1.checkint(), arg2.checktable().toIntArray())
            return LuaValue.NONE
        }
    }

    private class SpriteSetAll(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            sprite.setAll(arg.checktable().toIntArray())
            return LuaValue.NONE
        }
    }
}

class VRAM(pxlWidth: Int, pxlHeight: Int, nSprites: Int) {
    val sprites = Array(nSprites, { VSprite() })

    val background = ImageBuffer(pxlWidth, pxlHeight)
    val foreground = ImageBuffer(pxlWidth, pxlHeight) // text mode glyphs rendered here

    companion object {
        val CLUT = DecodeTapestry.colourIndices64 + Color(0, 0, 0, 0)
    }


    fun setBackgroundPixel(x: Int, y: Int, color: Int) {
        val col = CLUT[color]
        background.setRGBA(x, y, col.red, col.green, col.blue, 255)
    }

    fun setForegroundPixel(x: Int, y: Int, color: Int) {
        val col = CLUT[color]
        background.setRGBA(x, y, col.red, col.green, col.blue, col.alpha)
    }
}

class VSprite {
    companion object {
        val width = 8
        val height = 8
    }

    internal val CLUT = VRAM.CLUT
    internal val data = IntArray(height)

    var pal0 = 64 // transparent
    var pal1 = 56 // light cyan
    var pal2 = 19 // magenta
    var pal3 = 49 // white

    var posX = 0
    var posY = 0

    var hFlip = false
    var vFlip = false
    var rotation = 0

    var isBackground = false
    var isVisible = false

    fun setPaletteSet(col0: Int, col1: Int, col2: Int, col3: Int) {
        pal0 = col0
        pal1 = col1
        pal2 = col2
        pal3 = col3
    }

    fun getColourFromPalette(swatchNumber: Int): Color {
        val clutIndex = when (swatchNumber) {
            0 -> pal0
            1 -> pal1
            2 -> pal2
            3 -> pal3
            else -> throw IndexOutOfBoundsException("Palette size: 4, input: $swatchNumber")
        }
        return CLUT[clutIndex]
    }

    fun setPixel(x: Int, y: Int, color: Int) {
        data[y] = data[y] xor data[y].and(3 shl (2 * x)) // mask off desired area to 0b00
        data[y] = data[y] or (color shl (2 * x))
    }

    fun setLine(y: Int, rowData: IntArray) {
        for (i in 0..width - 1) {
            setPixel(i, y, rowData[i])
        }
    }

    fun setAll(data: IntArray) {
        for (i in 0..width * height - 1) {
            setPixel(i % width, i / width, data[i])
        }
    }
}