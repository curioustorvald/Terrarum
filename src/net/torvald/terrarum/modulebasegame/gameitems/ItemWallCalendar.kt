package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-08-08.
 */
class ItemWallCalendar(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWallCalendar") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 1.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/calendar.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_CALENDAR"

}