package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.itemproperties.GameItem

/**
 * Cross section of two inventory cell types
 *
 * Created by minjaesong on 2017-10-22.
 */
abstract class UIItemInventoryCellBase(
        parentUI: UIInventoryFull,
        override var posX: Int,
        override var posY: Int,
        open var item: GameItem?,
        open var amount: Int,
        open var itemImage: TextureRegion?,
        open var quickslot: Int? = null,
        open var equippedSlot: Int? = null
) : UIItem(parentUI) {
    abstract override fun update(delta: Float)
    abstract override fun render(batch: SpriteBatch, camera: Camera)
}