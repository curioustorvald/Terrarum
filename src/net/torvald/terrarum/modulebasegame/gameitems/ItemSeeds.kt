package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID

class ItemSeedOak(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.SaplingOak") {
    override var originalName = "ITEM_SEED_OAK"
    override var baseMass = 1.0
    override val materialId = "OOZE"
    override var inventoryCategory = Category.GENERIC
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,11)
}
class ItemSeedEbony(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.SaplingEbony") {
    override var originalName = "ITEM_SEED_EBONY"
    override var baseMass = 1.0
    override val materialId = "OOZE"
    override var inventoryCategory = Category.GENERIC
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,11)
}
class ItemSeedBirch(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.SaplingBirch") {
    override var originalName = "ITEM_SEED_BIRCH"
    override var baseMass = 1.0
    override val materialId = "OOZE"
    override var inventoryCategory = Category.GENERIC
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,11)
}
class ItemSeedRosewood(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.SaplingRosewood") {
    override var originalName = "ITEM_SEED_ROSEWOOD"
    override var baseMass = 1.0
    override val materialId = "OOZE"
    override var inventoryCategory = Category.GENERIC
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,11)
}