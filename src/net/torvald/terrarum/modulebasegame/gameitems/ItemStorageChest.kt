package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.FixtureStorageChest
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2019-07-08.
 */
class ItemStorageChest(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureStorageChest") {
    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureTikiTorch.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/storage_chest.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_STORAGE_CHEST"
}

/**
 * Created by minjaesong on 2024-03-02.
 */
class ItemStorageChestEbony(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureStorageChestEbony") {
    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureTikiTorch.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/storage_chest_2.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_STORAGE_CHEST"
}

/**
 * Created by minjaesong on 2024-03-02.
 */
class ItemStorageChestBirch(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureStorageChestBirch") {
    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureTikiTorch.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/storage_chest_3.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_STORAGE_CHEST"
}

/**
 * Created by minjaesong on 2024-03-02.
 */
class ItemStorageChestRosewood(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureStorageChestRosewood") {
    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureTikiTorch.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/storage_chest_4.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_STORAGE_CHEST"
}