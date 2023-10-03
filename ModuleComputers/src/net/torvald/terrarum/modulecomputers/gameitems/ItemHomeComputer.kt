package net.torvald.terrarum.modulecomputers.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulecomputers.gameactors.FixtureHomeComputer

/**
 * Created by minjaesong on 2021-12-04.
 */
class ItemHomeComputer(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureHomeComputer") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 20.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = FixtureItemBase.getItemImageFromSheet("dwarventech", "sprites/fixtures/desktop_computer.tga", TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_DESKTOP_COMPUTER"
}