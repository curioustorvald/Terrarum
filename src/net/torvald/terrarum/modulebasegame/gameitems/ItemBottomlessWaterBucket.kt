package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.GameItem.EquipPosition.HAND_GRIP
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-07-14.
 */
class ItemBottomlessWaterBucket(originalID: ItemID) : GameItem(originalID) {

    override var baseToolSize: Double? = PickaxeCore.BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = "CUPR"
    override var baseMass = 2.0
    override var equipPosition = HAND_GRIP
    override var originalName = "ITEM_BOTTOMLESS_WATER_BUCKET"

    init {
        stackable = false
        isUnique = true
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        val mx = Terrarum.mouseTileX; val my =Terrarum.mouseTileY
        if (!BlockCodex[INGAME.world.getTileFromTerrain(mx, my)].isSolid) {
            INGAME.world.setFluid(mx, my, Fluid.WATER, 1f)
            return 0L
        }
        else {
            return -1L
        }
    }
}

/**
 * Created by minjaesong on 2024-09-07.
 */
class ItemBottomlessLavaBucket(originalID: ItemID) : GameItem(originalID) {

    override var baseToolSize: Double? = PickaxeCore.BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = "CUPR"
    override var baseMass = 2.0
    override var equipPosition = HAND_GRIP
    override var originalName = "ITEM_BOTTOMLESS_LAVA_BUCKET"

    init {
        stackable = false
        isUnique = true
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        val mx = Terrarum.mouseTileX; val my =Terrarum.mouseTileY
        if (!BlockCodex[INGAME.world.getTileFromTerrain(mx, my)].isSolid) {
            INGAME.world.setFluid(mx, my, Fluid.CRUDE_OIL, 1f)
            return 0L
        }
        else {
            return -1L
        }
    }
}