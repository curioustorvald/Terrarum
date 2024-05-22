package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-03-03.
 */
class ItemTable(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTable") {
    override var dynamicID: ItemID = originalID
    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = "WOOD"
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/table_1.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_TABLE_OAK"
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class ItemTableEbony(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTableEbony") {
    override var dynamicID: ItemID = originalID
    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = "WOOD"
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/table_2.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_TABLE_EBONY"
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class ItemTableBirch(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTableBirch") {
    override var dynamicID: ItemID = originalID
    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = "WOOD"
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/table_3.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_TABLE_BIRCH"
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class ItemTableRosewood(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTableRosewood") {
    override var dynamicID: ItemID = originalID
    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = "WOOD"
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/table_4.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_TABLE_ROSEWOOD"
}