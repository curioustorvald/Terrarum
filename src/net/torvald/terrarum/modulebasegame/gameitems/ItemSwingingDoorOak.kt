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
    override var baseMass = 20.0 // 360[L] * 0.1 * 0.56[SpecificGravity], rounded to the nearest integer
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8,3)
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_DOOR_OAK"
}

class ItemSwingingDoorEbony(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorEbony") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 30.0 // 360[L] * 0.1 * 0.82[SpecificGravity], rounded to the nearest integer
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(9,3)
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_DOOR_EBONY"
}

class ItemSwingingDoorBirch(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorBirch") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 17.0 // 360[L] * 0.1 * 0.48[SpecificGravity], rounded to the nearest integer
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(10,3)
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_DOOR_BIRCH"
}

class ItemSwingingDoorRosewood(originalID: ItemID) :
    FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSwingingDoorRosewood") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 24.0 // 360[L] * 0.1 * 0.68[SpecificGravity], rounded to the nearest integer
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11,3)
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_DOOR_ROSEWOOD"
}
