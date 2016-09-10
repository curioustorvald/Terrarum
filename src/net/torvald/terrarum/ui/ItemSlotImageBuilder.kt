package net.torvald.terrarum.ui

import net.torvald.terrarum.blendNormal
import org.newdawn.slick.Color
import org.newdawn.slick.Image
import org.newdawn.slick.SpriteSheet
import org.newdawn.slick.SpriteSheetFont

/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 16-07-20.
 */
object ItemSlotImageBuilder {

    const val COLOR_BLACK = 1
    const val COLOR_WHITE = 2

    private val colourBlack = Color(0x40, 0x40, 0x40, 0xEE)
    private val colourWhite = Color(0xC0, 0xC0, 0xC0, 0xEE)

    private val numberFont = SpriteSheetFont(
            SpriteSheet("./assets/graphics/fonts/numeric_small.png", 5, 8),
            '0'
    )
    val slotImage = Image("./assets/graphics/gui/quickbar/item_slot.png") // must have same w/h as slotLarge
    val slotLarge = Image("./assets/graphics/gui/quickbar/item_slot_large.png")
    private val canvas = Image(slotImage.width, slotImage.height)
    private val canvasLarge = Image(slotLarge.width, slotLarge.height)

    val slotImageSize = slotImage.width

    fun produce(color: Int, number: Int = -1): Image {
        canvas.graphics.clear()

        if (color == COLOR_BLACK)
            canvas.graphics.drawImage(slotImage, 0f, 0f, colourBlack)
        else if (color == COLOR_WHITE)
            canvas.graphics.drawImage(slotImage, 0f, 0f, colourWhite)

        if (number >= 0) {
            canvas.graphics.font = numberFont

            if (color == COLOR_BLACK)
                canvas.graphics.color = colourWhite
            else if (color == COLOR_WHITE)
                canvas.graphics.color = colourBlack

            canvas.graphics.drawString(number.mod(UIQuickBar.SLOT_COUNT).toString(),
                    slotImage.width - 10f,
                    slotImage.height - 13f
            )
        }


        return canvas
    }

    fun produceLarge(color: Int, number: Int = -1): Image {
        canvasLarge.graphics.clear()

        if (color == COLOR_BLACK)
            canvasLarge.graphics.drawImage(slotLarge, 0f, 0f, colourBlack)
        else if (color == COLOR_WHITE)
            canvasLarge.graphics.drawImage(slotLarge, 0f, 0f, colourWhite)

        if (number >= 0) {
            canvasLarge.graphics.font = numberFont

            if (color == COLOR_BLACK)
                canvasLarge.graphics.color = colourWhite
            else if (color == COLOR_WHITE)
                canvasLarge.graphics.color = colourBlack

            canvasLarge.graphics.drawString(number.mod(UIQuickBar.SLOT_COUNT).toString(),
                    slotLarge.width - 10f,
                    slotLarge.height - 13f
            )
        }

        return canvasLarge
    }

}