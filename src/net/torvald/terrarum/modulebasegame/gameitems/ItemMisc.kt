package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-02-14.
 */
open class LightIngredientBase(originalID: ItemID) : GameItem(originalID) {
    override var baseMass = 1.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.GENERIC
    override val canBeDynamic = false
    override val materialId = "OORE"
    override var equipPosition = EquipPosition.HAND_GRIP
}



/**
 * Created by minjaesong on 2023-10-11.
 */
class OreStick(originalID: ItemID) : LightIngredientBase(originalID) {
    override var originalName = "ITEM_WOOD_STICK"
    override val materialId = "WOOD"
    override var calories = 600.0
    override var smokiness = 0.2f
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,6)
}

/**
 * Created by minjaesong on 2023-12-01.
 */
class ItemClayBall(originalID: ItemID) : LightIngredientBase(originalID) {
    override var originalName = "BLOCK_CLAY"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11,6)
}

/**
 * Created by minjaesong on 2024-02-14.
 */
class ItemGunpowder(originalID: ItemID) : LightIngredientBase(originalID) {
    override var originalName = "ITEM_GUNPOWDER"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,12)
}

/**
 * Created by minjaesong on 2024-02-15.
 */
class ItemTorch(originalID: ItemID) : LightIngredientBase(originalID) {
    override var baseMass = 0.8 // from blocks.csv
    override var inventoryCategory = Category.FIXTURE

    override var originalName = "BLOCK_TORCH"
    override val itemImage: TextureRegion
        get() = FixtureItemBase.getItemImageFromSingleImage("basegame", "items/torch.tga")
    override val itemImageEmissive: TextureRegion
        get() = FixtureItemBase.getItemImageFromSingleImage("basegame", "items/torch_emsv.tga")

    override fun getLumCol() = BlockCodex[Block.TORCH].getLumCol(0, 0)

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:176", delta)
    }

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        BlockBase.blockEffectWhenEquipped(actor, delta)
    }
}