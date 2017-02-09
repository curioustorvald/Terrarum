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
import org.newdawn.slick.*
import java.util.*

/**
 * Created by SKYHi14 on 2017-02-08.
 */
class PeripheralVideoCard(val globals: Globals, val termW: Int = 40, val termH: Int = 25) : Peripheral(globals, "ppu") {
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

    var fontRom = SpriteSheet("./assets/graphics/fonts/milky.tga", blockW, blockH)

    val CLUT = vram.CLUT
    val coloursCount = CLUT.size

    override fun loadLib() {
        super.loadLib()
        globals["ppu"]["setColor"] = SetColor(this)
        globals["ppu"]["getColor"] = GetColor(this)
        globals["ppu"]["emitChar"] = EmitChar(this)
    }

    fun render(g: Graphics) {
        g.drawImage(vram.background.image, 0f, 0f)
        vram.sprites.forEach {
            if (it.isBackground) {
                val spriteImage = it.data.image.getFlippedCopy(it.hFlip, it.vFlip)
                spriteImage.rotate(90f * it.rotation)
                g.drawImage(spriteImage, it.xpos.toFloat(), it.ypos.toFloat())
            }
        }
        g.drawImage(vram.foreground.image, 0f, 0f)
        vram.sprites.forEach {
            if (!it.isBackground) {
                val spriteImage = it.data.image.getFlippedCopy(it.hFlip, it.vFlip)
                spriteImage.rotate(90f * it.rotation)
                g.drawImage(spriteImage, it.xpos.toFloat(), it.ypos.toFloat())
            }
        }
    }

    private var currentColour = 49 // white
    fun getColor() = currentColour
    fun setColor(value: Int) { currentColour = value }

    fun drawChar(c: Char, x: Int, y: Int, col: Int = currentColour) {
        val glyph = fontRom.getSubImage(c.toInt() % 16, c.toInt() / 16)
        val color = CLUT[col]

        // software render
        for (gy in 0..blockH) {
            for (gx in 0..blockW) {
                val glyAlpha = glyph.getPixel(gx, gy)[3]

                if (glyAlpha > 0) {
                    vram.foreground.setRGBA(x * blockW + gx, y * blockH + gy, color.red, color.green, color.blue, 255)
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
            vram.foreground.rgba[i] = if (i % 4 == 3) 0xFF.toByte() else 0x00.toByte()
        }
    }

    fun clearAll() {
        for (i in 0..width * height - 1) {
            vram.background.rgba[i] = if (i % 4 == 3) 0xFF.toByte() else 0x00.toByte()
            vram.foreground.rgba[i] = if (i % 4 == 3) 0xFF.toByte() else 0x00.toByte()
        }
    }

    fun getSprite(index: Int) = vram.sprites[index]



    fun setTextRom(data: Array<BitSet>) {
        TODO("Not implemented")
    }

    fun resetTextRom() {
        fontRom = SpriteSheet("./assets/graphics/fonts/milky.tga", blockW, blockH)
    }


    class SetColor(val videoCard: PeripheralVideoCard) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            videoCard.setColor(arg.checkint())
            return LuaValue.NONE
        }
    }
    class GetColor(val videoCard: PeripheralVideoCard) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return videoCard.getColor().toLua()
        }
    }
    class EmitChar(val videoCard: PeripheralVideoCard) : ThreeArgFunction() {
        /** emitChar(char, x, y) */
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            videoCard.drawChar(arg1.checkint().toChar(), arg2.checkint(), arg3.checkint())
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

    var transparentKey: Int = 15 // black

    val CLUT = DecodeTapestry.colourIndices64


    fun setBackgroundPixel(x: Int, y: Int, color: Int) {
        val col = CLUT[color]
        background.setRGBA(x, y, col.red, col.green, col.blue, 255)
    }

    fun setForegroundPixel(x: Int, y: Int, color: Int) {
        val col = CLUT[color]
        background.setRGBA(x, y, col.red, col.green, col.blue, if (color == transparentKey) 0 else 255)
    }
}

class VSprite {
    private val width = 8
    private val height = 8

    val CLUT = DecodeTapestry.colourIndices64
    val data = ImageBuffer(width, height)

    var pal0 = 15 // black
    var pal1 = 56 // light cyan
    var pal2 = 19 // magenta
    var pal3 = 49 // white

    var transparentKey = 15 // black

    var xpos = 0
    var ypos = 0

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
        val col = getColourFromPalette(color)
        data.setRGBA(x, y, col.red, col.green, col.blue, if (color == transparentKey) 0 else 255)
    }

    fun setLine(y: Int, rowData: IntArray) {
        for (i in 0..width) {
            setPixel(i, y, rowData[i])
        }
    }

    fun setAll(data: IntArray) {
        for (i in 0..width * height) {
            setPixel(i % width, i / width, data[i])
        }
    }
}