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
open class OreItemBase(originalID: ItemID) : GameItem(originalID) {
    override var baseMass = 10.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.GENERIC
    override val isDynamic = false
    override val materialId = "OORE"
    override var equipPosition = EquipPosition.HAND_GRIP

    override fun effectOnPickup(actor: ActorWithBody) {
        val playerCodex = (actor.actorValue.getAsString(AVKey.ORE_DICT) ?: "").split(',').filter { it.isNotBlank() }.toMutableList()

        if (playerCodex.binarySearch(originalID) < 0) {
            playerCodex.add(originalID)
            playerCodex.sort()
            actor.actorValue[AVKey.ORE_DICT] = playerCodex.joinToString(",")
        }
    }
}

/* Wooden Log is a block */
class ItemLogsOak(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_LOGS_OAK"
    override val materialId = "WOOD"
    override var calories = 1920.0
    override var smokiness = 0.2f
    override var smeltingProduct: ItemID? = "item@basegame:29"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,10)

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
    override var smeltingProduct: ItemID? = "item@basegame:29"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,10)

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
    override var smeltingProduct: ItemID? = "item@basegame:29"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,10)

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
    override var smeltingProduct: ItemID? = "item@basegame:29"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,10)

    init { tags.add("ACTINGBLOCK") }
    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.blockStartPrimaryUse(actor, this, "basegame:75", delta)
    }
}



class ItemSeedOak(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SEED_OAK"
    override val materialId = "OOZE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,11)
}
class ItemSeedEbony(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SEED_EBONY"
    override val materialId = "OOZE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,11)
}
class ItemSeedBirch(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SEED_BIRCH"
    override val materialId = "OOZE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,11)
}
class ItemSeedRosewood(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SEED_ROSEWOOD"
    override val materialId = "OOZE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,11)
}



class OreStick(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_WOOD_STICK"
    override val materialId = "WOOD"
    override var calories = 600.0
    override var smokiness = 0.2f
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,6)
}
class OreCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_MALACHITE"
    override var smeltingProduct: ItemID? = "item@basegame:112"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,6)
}
class OreIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_HAEMATITE"
    override var smeltingProduct: ItemID? = "item@basegame:113"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,6)
}
class OreCoal(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_COAL"
    override var smeltingProduct: ItemID? = "item@basegame:114"
    override var calories = 4800.0
    override var smokiness = 0.3f
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,6)
}
class OreZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_SPHALERITE"
    override var smeltingProduct: ItemID? = "item@basegame:115"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(4,6)
}
class OreTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_CASSITERITE"
    override var smeltingProduct: ItemID? = "item@basegame:116"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(5,6)
}
class OreGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_NATURAL_GOLD"
    override var smeltingProduct: ItemID? = "item@basegame:117"
    override val materialId: String = "AURM"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(6, 6)
}
class OreSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_NATURAL_SILVER"
    override var smeltingProduct: ItemID? = "item@basegame:118"
    override val materialId: String = "ARGN"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(7,6)
}
class OreLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_GALENA"
    override var smeltingProduct: ItemID? = "item@basegame:119"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8,6)
}

class ItemCoalCoke(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_COAL_COKE"
    override var calories = 4800.0
    override var smokiness = 0.4f
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(9,6)
}
class ItemCharcoal(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_CHARCOAL"
    override var calories = 4800.0
    override var smokiness = 0.3f
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(12,6)
}
class IngotCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_COPPER"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,5)
}
class IngotIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_IRON"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,5)
}
class IngotSteel(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_STEEL"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,5)
}
class IngotZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ZINC"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(4,5)
}
class IngotTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_TIN"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(5,5)
}
class IngotGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_GOLD"
    override val materialId: String = "AURM"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(6, 5)
}
class IngotSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER"
    override val materialId: String = "ARGN"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(7,5)
}
class IngotLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_LEAD"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8,5)
}
class IngotBronze(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRONZE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(9,5)
}
class IngotBrass(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRASS"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(10,5)
}
class IngotElectrum(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ELECTRUM"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11,5)
}
class IngotSilverBillon(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER_BILLON"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(12,5)
}
class IngotRosegold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ROSEGOLD"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(13,5)
}
class IngotSolder(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SOLDER"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(14,5)
}


class ItemClayBall(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "BLOCK_CLAY"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11,6)
}