package net.torvald.terrarum.virtualcomputer.peripheral

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.DecodeTapestry
import net.torvald.terrarum.gameactors.ai.toLua
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.ThreeArgFunction
import org.newdawn.slick.*
import java.util.*

/**
 * Resolution: 640 x 200, non-square pixels
 *
 * Created by SKYHi14 on 2017-02-08.
 */
class PeripheralVideoCard(val host: TerrarumComputer, val termW: Int = 80, val termH: Int = 25) :
        Peripheral("ppu") {
    companion object {
        val blockW = 8 // MUST BE 8
        val blockH = 8 // MUST BE 8

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

    val spritesCount = 64

    val vram = VRAM(width, height, spritesCount)
    val frameBuffer = ImageBuffer(width, height)
    val frameBufferImage = frameBuffer.image

    // hard-coded 8x8
    var fontRom = Array<IntArray>(256, { IntArray(blockH) })

    var showCursor = true

    private var cursorBlinkOn = true
    private var cursorBlinkTimer = 0
    private val cursorBlinkTime = 250

    init {
        // build it for first time
        resetTextRom()

        frameBufferImage.filter = Image.FILTER_NEAREST
    }

    val CLUT = VRAM.CLUT
    val colorsCount = CLUT.size

    val luaSpriteTable = LuaTable()

    var color = 15 // black
        set(value) {
            if (value >= colorsCount || value < 0) {
                throw IllegalArgumentException("Unknown colour: $value")
            }
            else {
                field = value
            }
        }

    val cursorSprite = ImageBuffer(blockW, blockH * 2)
    val cursorImage: Image

    init {
        Arrays.fill(cursorSprite.rgba, 0xFF.toByte())
        cursorImage = cursorSprite.image

        fun composeSpriteObject(spriteIndex: Int) : LuaValue {
            val sprite = vram.sprites[spriteIndex]
            val t = LuaTable()

            t["getColFromPal"] = SpriteGetColFromPal(sprite)
            t["setPixel"] = SpriteSetPixel(sprite)
            t["setPalSet"] = SpriteSetPaletteSet(sprite)
            t["setLine"] = SpriteSetLine(sprite)
            t["setAll"] = SpriteSetAll(sprite)
            t["setRotation"] = SpriteSetRotation(sprite)
            t["setFlipH"] = SpriteSetFlipH(sprite)
            t["setFlipV"] = SpriteSetFlipV(sprite)

            return t
        }

        (0..spritesCount - 1).forEach { luaSpriteTable[it + 1] = composeSpriteObject(it) }
    }

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
        globals["ppu"]["setTextForeColor"] = SetTextForeColor(this)
        globals["ppu"]["getTextForeColor"] = GetTextForeColor(this)
        globals["ppu"]["setTextBackColor"] = SetTextBackColor(this)
        globals["ppu"]["getTextBackColor"] = GetTextBackColor(this)
        globals["ppu"]["setColor"] = SetDrawColor(this)
        globals["ppu"]["getColor"] = GetDrawColor(this)
        globals["ppu"]["emitChar"] = EmitChar(this)
        globals["ppu"]["clearAll"] = ClearAll(this)
        globals["ppu"]["clearBack"] = ClearBackground(this)
        globals["ppu"]["clearFore"] = ClearForeground(this)

        globals["ppu"]["getSpritesCount"] = GetSpritesCount(this)
        globals["ppu"]["width"] = GetWidth(this)
        globals["ppu"]["height"] = GetHeight(this)


        globals["ppu"]["getSprite"] = GetSprite(this)

        globals["ppu"]["drawRectBack"] = DrawRectBack(this)
        globals["ppu"]["drawRectFore"] = DrawRectFore(this)
        globals["ppu"]["fillRectBack"] = FillRectBack(this)
        globals["ppu"]["fillRectFore"] = FillRectFore(this)
        globals["ppu"]["drawString"] = DrawString(this)
    }

    private val spriteBuffer = ImageBuffer(VSprite.width * 2, VSprite.height)

    fun render(g: Graphics) {
        cursorBlinkTimer += Terrarum.UPDATE_DELTA
        if (cursorBlinkTimer > cursorBlinkTime) {
            cursorBlinkTimer -= cursorBlinkTime
            cursorBlinkOn = !cursorBlinkOn
        }

        fun VSprite.render() {
            if (this.isVisible) {
                val h = VSprite.height
                val w = VSprite.width
                if (rotation and 1 == 0) { // deg 0, 180
                    (if (rotation == 0 && !vFlip || rotation == 2 && vFlip) 0..h - 1 else h - 1 downTo 0).forEachIndexed { ordY, y ->
                        (if (rotation == 0 && !hFlip || rotation == 2 && hFlip) 0..w - 1 else w - 1 downTo 0).forEachIndexed { ordX, x ->
                            val pixelData = data[y].ushr(2 * x).and(0b11)
                            val col = getColourFromPalette(pixelData)

                            if (this.drawWide) {
                                spriteBuffer.setRGBA(ordX * 2, ordY, col.red, col.green, col.blue, col.alpha)
                                spriteBuffer.setRGBA(ordX * 2 + 1, ordY, col.red, col.green, col.blue, col.alpha)
                            }
                            else {
                                spriteBuffer.setRGBA(ordX, ordY, col.red, col.green, col.blue, col.alpha)
                            }
                        }
                    }
                }
                else { // deg 90, 270
                    (if (rotation == 3 && !hFlip || rotation == 1 && hFlip) 0..w - 1 else w - 1 downTo 0).forEachIndexed { ordY, y ->
                        (if (rotation == 3 && !vFlip || rotation == 1 && vFlip) h - 1 downTo 0 else 0..h - 1).forEachIndexed { ordX, x ->
                            val pixelData = data[y].ushr(2 * x).and(0b11)
                            val col = getColourFromPalette(pixelData)

                            if (this.drawWide) {
                                spriteBuffer.setRGBA(ordY * 2, ordX, col.red, col.green, col.blue, col.alpha)
                                spriteBuffer.setRGBA(ordY * 2 + 1, ordX, col.red, col.green, col.blue, col.alpha)
                            }
                            else {
                                spriteBuffer.setRGBA(ordY, ordX, col.red, col.green, col.blue, col.alpha)
                            }
                        }
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
        g.drawImage(img.getScaledCopy(blockW * termW, blockH * termH * 2), 0f, 0f)


        if (cursorBlinkOn && showCursor) {
            g.drawImage(
                    cursorImage,
                    host.term.cursorX * blockW.toFloat(),
                    (host.term as Terminal).cursorY * blockH * 2f
            )
        }


        // scanlines
        g.color = Color(0, 0, 0, 40)
        g.lineWidth = 1f
        for (i in 1..blockH * termH * 2 step 2) {
            g.drawLine(0f, i.toFloat(), blockW * termW - 1f, i.toFloat())
        }

        img.destroy()
    }

    private var textColorFore = 49 // white
    private var textColorBack = 64 // transparent

    fun drawChar(c: Char, x: Int, y: Int, colFore: Int = textColorFore, colBack: Int = textColorBack) {
        val glyph = fontRom[c.toInt()]

        // software render
        for (gy in 0..blockH - 1) { for (gx in 0..blockW - 1) {
            val glyAlpha = glyph[gy] and (1 shl gx)

            if (glyAlpha != 0) {
                vram.setForegroundPixel(x + gx, y + gy, colFore)
            }
            else {
                vram.setForegroundPixel(x + gx, y + gy, colBack)
            }
        }}
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

    fun drawRectBack(x: Int, y: Int, w: Int, h: Int, c: Int = color) {
        (0..w - 1).forEach {
            vram.setBackgroundPixel(x + it, y, c)
            vram.setBackgroundPixel(x + it, y + h - 1, c)
        }
        (1..h - 2).forEach {
            vram.setBackgroundPixel(x, y + it, c)
            vram.setBackgroundPixel(x + w - 1, y + it, c)
        }
    }

    fun fillRectBack(x: Int, y: Int, w: Int, h: Int, c: Int = color) {
        for (py in 0..h - 1) { for (px in 0..w - 1) {
            vram.setBackgroundPixel(x + px, y + py, c)
        }}
    }

    fun drawRectFore(x: Int, y: Int, w: Int, h: Int, c: Int = color) {
        (0..w - 1).forEach {
            vram.setForegroundPixel(x + it, y, c)
            vram.setForegroundPixel(x + it, y + h - 1, c)
        }
        (1..h - 2).forEach {
            vram.setForegroundPixel(x, y + it, c)
            vram.setForegroundPixel(x + w - 1, y + it, c)
        }
    }

    fun fillRectFore(x: Int, y: Int, w: Int, h: Int, c: Int = color) {
        for (py in 0..h - 1) { for (px in 0..w - 1) {
            vram.setForegroundPixel(x + px, y + py, c)
        }}
    }


    fun getSprite(index: Int) = vram.sprites[index]


    private fun ImageBuffer.softwareRender(other: ImageBuffer, posX: Int, posY: Int) {
        for (y in 0..other.height - 1) {
            for (x in 0..other.width - 1) {
                val ix = posX + x
                val iy = posY + y
                if (ix >= 0 && iy >= 0 && ix < this.width && iy < this.height) {
                    if (other.rgba[4 * (y * other.texWidth + x) + 3] != 0.toByte()) { // if not transparent
                        this.rgba[4 * (iy * this.texWidth + ix) + 0] = other.rgba[4 * (y * other.texWidth + x) + 0]
                        this.rgba[4 * (iy * this.texWidth + ix) + 1] = other.rgba[4 * (y * other.texWidth + x) + 1]
                        this.rgba[4 * (iy * this.texWidth + ix) + 2] = other.rgba[4 * (y * other.texWidth + x) + 2]
                        this.rgba[4 * (iy * this.texWidth + ix) + 3] = other.rgba[4 * (y * other.texWidth + x) + 3]
                    }
                }
            }
        }
    }

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
     * 00111111
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

    ///////////////////
    // Lua functions //
    ///////////////////

    class SetTextForeColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.textColorFore = arg.checkint()
            return LuaValue.NONE
        }
    }
    class GetTextForeColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.textColorFore.toLua()
        }
    }
    class SetTextBackColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.textColorBack = arg.checkint()
            return LuaValue.NONE
        }
    }
    class GetTextBackColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.textColorBack.toLua()
        }
    }
    class SetDrawColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.color = arg.checkint()
            return LuaValue.NONE
        }
    }
    class GetDrawColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.color.toLua()
        }
    }
    class GetWidth(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.width.toLua()
        }
    }
    class GetHeight(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.height.toLua()
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
    class GetSpritesCount(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.spritesCount.toLua()
        }
    }
    class GetSprite(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return videoCard.luaSpriteTable[arg.checkint() - 1]
        }
    }
    class DrawRectBack(val videoCard: PeripheralVideoCard) : FourArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue {
            videoCard.drawRectBack(arg1.checkint(), arg2.checkint(), arg3.checkint(), arg4.checkint())
            return LuaValue.NONE
        }
    }
    class FillRectBack(val videoCard: PeripheralVideoCard) : FourArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue {
            videoCard.fillRectBack(arg1.checkint(), arg2.checkint(), arg3.checkint(), arg4.checkint())
            return LuaValue.NONE
        }
    }
    class DrawRectFore(val videoCard: PeripheralVideoCard) : FourArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue {
            videoCard.drawRectFore(arg1.checkint(), arg2.checkint(), arg3.checkint(), arg4.checkint())
            return LuaValue.NONE
        }
    }
    class FillRectFore(val videoCard: PeripheralVideoCard) : FourArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue {
            videoCard.fillRectFore(arg1.checkint(), arg2.checkint(), arg3.checkint(), arg4.checkint())
            return LuaValue.NONE
        }
    }
    class DrawString(val videoCard: PeripheralVideoCard) : ThreeArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            val str = arg1.checkjstring()
            val x = arg2.checkint()
            val y = arg3.checkint()
            str.forEachIndexed { i, c ->
                videoCard.drawChar(c, x + blockW * i, y)
            }
            return LuaValue.NONE
        }
    }

    /////////////
    // Sprites //
    /////////////

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
    private class SpriteSetRotation(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            sprite.rotation = arg.checkint()
            return LuaValue.NONE
        }
    }
    private class SpriteSetFlipH(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            sprite.hFlip = arg.checkboolean()
            return LuaValue.NONE
        }
    }
    private class SpriteSetFlipV(val sprite: VSprite) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            sprite.vFlip = arg.checkboolean()
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
        foreground.setRGBA(x, y, col.red, col.green, col.blue, col.alpha)
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
        set(value) { field = value % 4 }

    var isBackground = false
    var isVisible = false
    var drawWide = false

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

    fun setPos(x: Int, y: Int) {
        posX = x
        posY = y
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


abstract class FourArgFunction : LibFunction() {

    abstract override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue

    override fun call(): LuaValue {
        return call(LuaValue.NIL, LuaValue.NIL, LuaValue.NIL, LuaValue.NIL)
    }

    override fun call(arg: LuaValue): LuaValue {
        return call(arg, LuaValue.NIL, LuaValue.NIL, LuaValue.NIL)
    }

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        return call(arg1, arg2, LuaValue.NIL, LuaValue.NIL)
    }

    override fun invoke(varargs: Varargs): Varargs {
        return call(varargs.arg1(), varargs.arg(2), varargs.arg(3), varargs.arg(4))
    }

}
