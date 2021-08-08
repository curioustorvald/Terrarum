package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter

class ItemLogicSignalEmitter(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_TIKI_TORCH"
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override var stackable = true
    override var inventoryCategory = Category.FIXTURE
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("basegame-sprites-fixtures-signal_source.tga")
    override var baseToolSize: Double? = baseMass

    init {
        CommonResourcePool.addToLoadingList("basegame-sprites-fixtures-signal_source.tga") {
            val t = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "sprites/fixtures/signal_source.tga")))
            t.flip(false, true)
            /*return*/t
        }
        CommonResourcePool.loadAll()

        equipPosition = EquipPosition.HAND_GRIP
    }

    override fun startPrimaryUse(delta: Float): Boolean {
        val item = FixtureLogicSignalEmitter { Lang[originalName] }

        return item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY)
        // return true when placed, false when cannot be placed
    }



}