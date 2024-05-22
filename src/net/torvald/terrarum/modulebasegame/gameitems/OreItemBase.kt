package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-10-11.
 */
open class OreItemBase(originalID: ItemID, private val registerToOreDict: Boolean = false) : GameItem(originalID) {
    override var baseMass = 10.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.GENERIC
    override val canBeDynamic = false
    override val materialId = "OORE"
    override var equipPosition = EquipPosition.HAND_GRIP

    override fun effectOnPickup(actor: ActorWithBody) {
        if (registerToOreDict) {
            val playerCodex = (actor.actorValue.getAsString(AVKey.ORE_DICT) ?: "").split(',').filter { it.isNotBlank() }
                .toMutableList()

            if (playerCodex.binarySearch(originalID) < 0) {
                playerCodex.add(originalID)
                playerCodex.sort()
                actor.actorValue[AVKey.ORE_DICT] = playerCodex.joinToString(",")
            }
        }
    }
}

/* Wooden Log is a block */
class ItemLogsOak(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_LOGS_OAK"
    override val materialId = "WOOD"
    override var calories = 1920.0
    override var smokiness = 0.2f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,10)
    }

    init { tags.add("ACTINGBLOCK") }
    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:72", delta)
    }
}
class ItemLogsEbony(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_LOGS_EBONY"
    override val materialId = "WOOD"
    override var calories = 1920.0
    override var smokiness = 0.2f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(1,10)
    }

    init { tags.add("ACTINGBLOCK") }
    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:73", delta)
    }
}
class ItemLogsBirch(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_LOGS_BIRCH"
    override val materialId = "WOOD"
    override var calories = 1920.0
    override var smokiness = 0.2f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(2,10)
    }

    init { tags.add("ACTINGBLOCK") }
    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:74", delta)
    }
}
class ItemLogsRosewood(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_LOGS_ROSEWOOD"
    override val materialId = "WOOD"
    override var calories = 1920.0
    override var smokiness = 0.2f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(3,10)
    }

    init { tags.add("ACTINGBLOCK") }
    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:75", delta)
    }
}



class OreCopper(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_MALACHITE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(1,6)
    }
}
class OreIron(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_HAEMATITE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(2,6)
    }
}
class OreCoal(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_COAL"
    override var calories = 4800.0
    override var smokiness = 0.3f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(3,6)
    }
}
class OreZinc(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_SPHALERITE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(4,6)
    }
}
class OreTin(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_CASSITERITE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(5,6)
    }
}
class OreGold(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_NATURAL_GOLD"
    override val materialId: String = "AURM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(6, 6)
    }
}
class OreSilver(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_NATURAL_SILVER"
    override val materialId: String = "ARGN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(7,6)
    }
}
class OreLead(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_GALENA"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(8,6)
    }
}

class GemQuartz(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_GEM_QUARTZ"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(13,6)
    }
}
class GemAmethyst(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_GEM_AMETHYST"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(14,6)
    }
}
class ItemRockSalt(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ROCK_SALT"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(10,6)
    }
}
class ItemNitre(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_NITRE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(15,6)
    }
}


class ItemCoalCoke(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_COAL_COKE"
    override var calories = 4800.0
    override var smokiness = 0.4f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(9,6)
    }
}
class ItemCharcoal(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_CHARCOAL"
    override var calories = 4800.0
    override var smokiness = 0.3f
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(12,6)
    }
}
class IngotCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_COPPER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(16,0)
    }
}
class IngotIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_IRON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(16,0)
    }
}
class IngotSteel(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_STEEL"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(18,0)
    }
}
class IngotZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ZINC"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(19,0)
    }
}
class IngotTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_TIN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(20,0)
    }
}
class IngotGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_GOLD"
    override val materialId: String = "AURM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(21, 0)
    }
}
class IngotSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER"
    override val materialId: String = "ARGN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(22,0)
    }
}
class IngotLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_LEAD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(23,0)
    }
}
class IngotBronze(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRONZE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(24,0)
    }
}
class IngotBrass(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRASS"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(25,0)
    }
}
class IngotElectrum(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ELECTRUM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(26,0)
    }
}
class IngotSilverBillon(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER_BILLON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(27,0)
    }
}
class IngotRosegold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ROSEGOLD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(28,0)
    }
}
class IngotSolder(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SOLDER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(29,0)
    }
}
class SheetCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_COPPER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(16,1)
    }
}
class SheetIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_IRON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(16,1)
    }
}
class SheetSteel(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_STEEL"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(18,1)
    }
}
class SheetZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ZINC"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(19,1)
    }
}
class SheetTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_TIN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(20,1)
    }
}
class SheetGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_GOLD"
    override val materialId: String = "AURM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(21,1)
    }
}
class SheetSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SILVER"
    override val materialId: String = "ARGN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(22,1)
    }
}
class SheetLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_LEAD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(23,1)
    }
}
class SheetBronze(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_BRONZE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(24,1)
    }
}
class SheetBrass(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_BRASS"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(25,1)
    }
}
class SheetElectrum(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ELECTRUM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(26,1)
    }
}
class SheetSilverBillon(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SILVER_BILLON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(27,1)
    }
}
class SheetRosegold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ROSEGOLD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(28,1)
    }
}
class SheetSolder(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SOLDER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(29,1)
    }
}