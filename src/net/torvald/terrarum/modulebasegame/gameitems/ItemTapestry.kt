package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTapestry

/**
 * Created by minjaesong on 2022-02-28.
 */
class ItemTapestry(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureTapestry") {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_TAPESTRY"
    override var baseMass = 6.0
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
        FixtureTapestry(
                Gdx.files.internal("assets/monkey_island").readBytes(),
                Block.PLANK_NORMAL
        )
    }

}