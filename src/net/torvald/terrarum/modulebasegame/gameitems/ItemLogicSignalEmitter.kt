package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter

class ItemLogicSignalEmitter(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_LOGIC_SIGNAL_EMITTER"
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override var stackable = true
    override var inventoryCategory = Category.MISC
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

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Boolean {
        val item = FixtureLogicSignalEmitter()

        return item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY)
        // return true when placed, false when cannot be placed
    }

    override fun effectWhenEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}