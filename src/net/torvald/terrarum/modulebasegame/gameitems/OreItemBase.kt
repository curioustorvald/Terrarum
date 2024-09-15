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
class OreUranium(originalID: ItemID) : OreItemBase(originalID, true) {
    override var originalName = "ITEM_ORE_PITCHBLENDE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,7)
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
