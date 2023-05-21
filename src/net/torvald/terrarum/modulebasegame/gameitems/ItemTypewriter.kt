package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2022-08-26.
 */
class ItemTypewriter(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTypewriter") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_TYPEWRITER"
    override var baseMass = FixtureTikiTorch.MASS
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSheet("basegame", "sprites/fixtures/typewriter.tga", 32, 16)

    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

}