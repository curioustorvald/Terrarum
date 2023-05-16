package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.*

/**
 * Created by minjaesong on 2022-07-15.
 */
class ItemSwingingDoorOak(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorOak") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_DOOR_OAK"
    override var baseMass = 20.0 // 360[L] * 0.1 * 0.56[SpecificGravity], rounded to the nearest integer
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override val makeFixture: () -> FixtureBase = {
        FixtureSwingingDoorOak()
    }

}

class ItemSwingingDoorEbony(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorEbony") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_DOOR_EBONY"
    override var baseMass = 30.0 // 360[L] * 0.1 * 0.82[SpecificGravity], rounded to the nearest integer
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override val makeFixture: () -> FixtureBase = {
        FixtureSwingingDoorEbony()
    }

}

class ItemSwingingDoorBirch(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorBirch") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_DOOR_BIRCH"
    override var baseMass = 17.0 // 360[L] * 0.1 * 0.48[SpecificGravity], rounded to the nearest integer
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override val makeFixture: () -> FixtureBase = {
        FixtureSwingingDoorBirch()
    }

}

class ItemSwingingDoorRosewood(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorRosewood") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_DOOR_ROSEWOOD"
    override var baseMass = 24.0 // 360[L] * 0.1 * 0.68[SpecificGravity], rounded to the nearest integer
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override val makeFixture: () -> FixtureBase = {
        FixtureSwingingDoorRosewood()
    }

}
