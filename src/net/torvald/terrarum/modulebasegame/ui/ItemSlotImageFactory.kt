package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameactors.ai.toInt
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 2016-07-20.
 */
object ItemSlotImageFactory {

    val CELLCOLOUR_BLACK_OPAQUE = Color(0x404040_FF)
    val CELLCOLOUR_WHITE_OPAQUE = Color(0xC0C0C0_FF.toInt())

    /** Blend mode: normal */
    val CELLCOLOUR_BLACK = Color(0x404040_88)
    val CELLCOLOUR_WHITE = Color(0xC0C0C0_88.toInt())

    /** Blend mode: screen */
    val CELLCOLOUR_BLACK_ACTIVE = Color(0x282828ff)

    val slotImage = TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slots_atlas.tga"), 38, 38) // must have same w/h as slotLarge

    fun produce(isBlack: Boolean, number: Int = 10, item: GameItem?): ItemSlotImage {
        return ItemSlotImage(slotImage.get(number, 0 or isBlack.toInt().shl(1)), ItemCodex.getItemImage(item))
    }

    fun produceLarge(isBlack: Boolean, number: Int = 10, item: GameItem?): ItemSlotImage {
        return ItemSlotImage(slotImage.get(number, 1 or isBlack.toInt().shl(1)), ItemCodex.getItemImage(item))
    }


    fun dispose() {
        slotImage.dispose()
    }

}

data class ItemSlotImage(val baseTex: TextureRegion, val itemTex: TextureRegion?) {
    /**
     * batch.begin() must be called beforehand.
     *
     * @param batch a spritebatch
     * @param cx centre-x position of the draw
     * @param cy centre-y position of the draw
     */
    fun draw(batch: SpriteBatch, cx: Int, cy: Int) {
        // just draws two image on the centre

        batch.draw(baseTex, cx - (baseTex.regionWidth).div(2).toFloat(), cy - (baseTex.regionHeight).div(2).toFloat())
        if (itemTex != null)
            batch.draw(itemTex, cx - (itemTex.regionWidth).div(2).toFloat(), cy - (itemTex.regionHeight).div(2).toFloat())

    }
}