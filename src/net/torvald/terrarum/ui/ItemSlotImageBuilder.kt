package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.gameactors.ai.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 16-07-20.
 */
object ItemSlotImageBuilder {

    // FIXME it leaks mem waaaaagh

    val colourBlack = Color(0x404040_FF)
    val colourWhite = Color(0xC0C0C0_FF.toInt())

    val slotImage = TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slot.tga"), 38, 38) // must have same w/h as slotLarge
    val slotLarge = TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slot_large.tga"), 38, 38)


    private val imageDict = HashMap<Long, Texture>()


    fun produce(isBlack: Boolean, number: Int = 10): TextureRegion {
        return slotImage.get(number, 0)
    }

    fun produceLarge(isBlack: Boolean, number: Int = 10): TextureRegion {
        return slotLarge.get(number, 0)
    }


    fun dispose() {
        slotImage.dispose()
        slotLarge.dispose()
    }

}