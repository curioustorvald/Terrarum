package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 16-07-20.
 */
object ItemSlotImageBuilder {

    val colourBlack = Color(0x404040_FF)
    val colourWhite = Color(0xC0C0C0_FF.toInt())

    private val numberFont = TextureRegionPack(
            "./assets/graphics/fonts/numeric_small.tga", 5, 8
    )
    val slotImage = Pixmap(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slot.tga")) // must have same w/h as slotLarge
    val slotLarge = Pixmap(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slot_large.tga"))


    private val imageDict = HashMap<ImageDesc, Texture>()


    fun produce(isBlack: Boolean, number: Int = -1): Texture {
        val pixmap = Pixmap(slotImage.width, slotImage.height, Pixmap.Format.RGBA8888)
        val color = if (isBlack) colourBlack else colourWhite


        val desc = ImageDesc(color, number, false)
        if (imageDict.containsKey(desc))
            return imageDict[desc]!!


        pixmap.setColor(color)
        pixmap.drawPixmap(slotImage, 0, 0)


        /*if (number >= 0) {
            if (isBlack)
                pixmap.setColor(colourWhite)
            else
                pixmap.setColor(colourBlack)


            pixmap.drawPixmap(fontPixmap,
                    slotImage.width - 10,
                    slotImage.height - 13
            )
        }*/


        val retTex = Texture(pixmap)
        pixmap.dispose()
        imageDict.put(desc, retTex)
        return retTex
    }

    fun produceLarge(isBlack: Boolean, number: Int = -1): Texture {
        val pixmap = Pixmap(slotLarge.width, slotLarge.height, Pixmap.Format.RGBA8888)
        val color = if (isBlack) colourBlack else colourWhite


        val desc = ImageDesc(color, number, false)
        if (imageDict.containsKey(desc))
            return imageDict[desc]!!


        pixmap.setColor(color)
        pixmap.drawPixmap(slotLarge, 0, 0)


        /*if (number >= 0) {
            if (isBlack)
                pixmap.setColor(colourWhite)
            else
                pixmap.setColor(colourBlack)


            pixmap.drawPixmap(fontPixmap,
                    slotImage.width - 10,
                    slotImage.height - 13
            )
        }*/


        val retTex = Texture(pixmap)
        pixmap.dispose()
        imageDict.put(desc, retTex)
        return retTex
    }


    private data class ImageDesc(val color: Color, val number: Int, val isLarge: Boolean)

    fun dispose() {
        slotImage.dispose()
        slotLarge.dispose()
    }

}