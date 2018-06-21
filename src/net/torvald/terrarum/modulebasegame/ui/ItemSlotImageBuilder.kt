package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameactors.ai.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 2016-07-20.
 */
object ItemSlotImageBuilder {

    // FIXME it leaks mem waaaaagh

    val colourBlack = Color(0x404040_FF)
    val colourWhite = Color(0xC0C0C0_FF.toInt())

    val slotImage = TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slots_atlas.tga"), 38, 38) // must have same w/h as slotLarge


    private val imageDict = HashMap<Long, Texture>()


    fun produce(isBlack: Boolean, number: Int = 10): TextureRegion {
        return slotImage.get(number, 0 or isBlack.toInt().shl(1))
    }

    fun produceLarge(isBlack: Boolean, number: Int = 10): TextureRegion {
        return slotImage.get(number, 1 or isBlack.toInt().shl(1))
    }


    fun dispose() {
        slotImage.dispose()
    }

}