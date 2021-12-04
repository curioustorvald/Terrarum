package net.torvald.terrarum.modulecomputers.gameitems

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.inInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulecomputers.gameactors.FixtureHomeComputer

/**
 * Created by minjaesong on 2021-12-04.
 */
class ItemHomeComputer(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "Computer"
    override var baseMass = 20.0
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("dwarventech-sprites-fixtures-desktop_computer.tga")
    override var baseToolSize: Double? = baseMass


    init {
        CommonResourcePool.addToLoadingList("dwarventech-sprites-fixtures-desktop_computer.tga") {
//            val t = TextureRegion(Texture(ModMgr.getGdxFile("dwarventech", "nonexisting_file!!!")))
            val t = TextureRegion(Texture(ModMgr.getGdxFile("dwarventech", "sprites/fixtures/desktop_computer.tga")))
            t.flip(false, true)
            /*return*/t
        }
        CommonResourcePool.loadAll()

        equipPosition = EquipPosition.HAND_GRIP
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = inInteractableRange(actor) {
        val item = FixtureHomeComputer()

        item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY - item.blockBox.height + 1)
        // return true when placed, false when cannot be placed
    }
}