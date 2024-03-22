package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-09-20.
 */
class ItemWorkbench(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWorkbench") {


    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/workbench.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_WORKBENCH"

}

/**
 * Created by minjaesong on 2024-03-14.
 */
class ItemElectricWorkbench(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureElectricWorkbench") {


    override var baseMass = 40.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSheet("basegame", "sprites/fixtures/electric_workbench.tga", 32, 32, 0, 1)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_ELECTRIC_WORKBENCH"

}

/**
 * Created by minjaesong on 2024-03-22.
 */
class ItemEngravingWorkbench(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureEngravingWorkbench") {


    override var baseMass = 40.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSheet("basegame", "sprites/fixtures/engraving_workbench.tga", 32, 32, 0, 0)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_ENGRAVING_WORKBENCH"

}
