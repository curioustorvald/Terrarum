package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-12-04.
 */
class ItemSmelterBasic(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic") {

    init {
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/smelter_tall.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/smelter_tall.tga"), 48, 64)
        }
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/smelter_tall_emsv.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/smelter_tall_emsv.tga"), 48, 64)
        }
        CommonResourcePool.loadAll()
    }

    override var baseMass = 100.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/smelter_tall.tga").get(1, 0)
    override val itemImageEmissive: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/smelter_tall_emsv.tga").get(1, 0)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_SMELTER_SMALL"

}