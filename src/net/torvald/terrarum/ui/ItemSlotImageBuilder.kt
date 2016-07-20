package net.torvald.terrarum.ui

import net.torvald.terrarum.setBlendNormal
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

    private val colourBlack = Color(0x404040)
    private val colourWhite = Color(0xC0C0C0)

    private val numberFont = SpriteSheetFont(
            SpriteSheet("./res/graphics/fonts/numeric_small.png", 5, 8),
            '0'
    )
    private val slotImage = Image("./res/graphics/gui/quickbar/item_slot.png")
    private val canvas = Image(slotImage.width, slotImage.height)

    fun produce(color: Int, number: Int = -1): Image {
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
                    slotImage.width - 6f,
                    slotImage.height - 10f
            )
        }

        return canvas
    }

}