package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-05-16.
 */
class ItemTikiTorch(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_TIKI_TORCH"
    override var baseMass = FixtureTikiTorch.MASS
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = (CommonResourcePool.getOrPut("sprites-fixtures-tiki_torch.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/tiki_torch.tga"), 16, 32, flipY = false)
        } as TextureRegionPack).get(0,0)
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

}