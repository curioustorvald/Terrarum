package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Item
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTextSignCopper

/**
 * Created by minjaesong on 2024-03-20.
 */
class ItemTextSignCopper(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTextSignCopper") {

    constructor() : this("") // item that can be dynamic needs no-arg constructor, as the class gets serialised into the savegame under dynamicItemInventory.[dynamicID]

    override var dynamicID: ItemID = originalID
    override var baseMass = 10.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(14, 4)
    }
    override val itemImageEmissive: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(13, 4)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_COPPER_SIGN"

    init {
        stackable = false
    }

    @Transient override val makeFixture: () -> FixtureBase = {
        FixtureTextSignCopper(
            extra.getAsString("signContent") ?: "",
            extra.getAsInt("signPanelCount") ?: 2
        )
    }
}