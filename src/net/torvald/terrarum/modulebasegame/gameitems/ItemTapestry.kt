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

    constructor() : this("") // item that can be dynamic needs no-arg constructor, as the class gets serialised into the savegame under dynamicItemInventory.[dynamicID]

    override var dynamicID: ItemID = originalID
    override var baseMass = 6.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_TAPESTRY"

    @Transient override val makeFixture: () -> FixtureBase = {
        FixtureTapestry(
            // TODO use extra["fileRef"] (string) and extra["framingMaterial"] (string)
            Gdx.files.internal("assets/monkey_island").readBytes(),
            Block.PLANK_NORMAL
        )
    }

}