package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Make item slot image with number on bottom-right
 *
 * Created by minjaesong on 2016-07-20.
 */
object ItemSlotImageFactory {

    val TILE_WIDTH = 38
    val TILE_HEIGHT = 42

    val CELLCOLOUR_BLACK_OPAQUE = Color(0x404040_FF)
    val CELLCOLOUR_WHITE_OPAQUE = Color(0xC0C0C0_FF.toInt())

    /** Blend mode: normal */
    val CELLCOLOUR_BLACK = Color(0x404040_88)
    val CELLCOLOUR_WHITE = Color(0xC0C0C0_88.toInt())

    /** Blend mode: screen */
    val CELLCOLOUR_BLACK_ACTIVE = Color(0x282828ff)

    val slotImage = TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/quickbar/item_slots_atlas2.tga"), TILE_WIDTH, TILE_HEIGHT) // must have same w/h as slotLarge

    fun produce(isBlack: Boolean, number: Int?, sprite: TextureRegion?): ItemSlotImage {
        return ItemSlotImage(slotImage.get(number ?: 10, 0 or isBlack.toInt().shl(1)), sprite)
    }

    fun produceLarge(isBlack: Boolean, number: Int?, sprite: TextureRegion?, hasGauge: Boolean): ItemSlotImage {
        val y = if (hasGauge && isBlack) 9 else if (hasGauge && !isBlack) 8 else if (!hasGauge && isBlack) 3 else 1
        return ItemSlotImage(slotImage.get(number ?: 10, y), sprite)
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

        batch.draw(baseTex, cx - (baseTex.regionWidth).div(2).toFloat(), cy - (baseTex.regionWidth).div(2).toFloat())
        if (itemTex != null)
            batch.draw(itemTex, cx - (itemTex.regionWidth).div(2).toFloat(), cy - (itemTex.regionHeight).div(2).toFloat())

    }
}