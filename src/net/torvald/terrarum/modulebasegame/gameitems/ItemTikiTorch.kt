package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.inInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-05-16.
 */
class ItemTikiTorch(originalID: ItemID) : GameItem(originalID) {

    init {
        CommonResourcePool.addToLoadingList("sprites-fixtures-tiki_torch.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/tiki_torch.tga"), 16, 32, flipY = true)
        }
        CommonResourcePool.loadAll()
    }

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_TIKI_TORCH"
    override var baseMass = FixtureTikiTorch.MASS
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("sprites-fixtures-tiki_torch.tga").get(0,0)
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = inInteractableRange(actor) {
        val item = FixtureTikiTorch()

        item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY - item.blockBox.height + 1)
        // return true when placed, false when cannot be placed

    }

}