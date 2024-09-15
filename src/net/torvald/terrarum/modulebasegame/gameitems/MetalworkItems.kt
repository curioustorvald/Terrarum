package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID

class IngotCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_COPPER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(0,0)
    }
}
class IngotIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_IRON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(1,0)
    }
}
class IngotSteel(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_STEEL"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(2,0)
    }
}
class IngotZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ZINC"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(3,0)
    }
}
class IngotTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_TIN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(4,0)
    }
}
class IngotGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_GOLD"
    override val materialId: String = "AURM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(5, 0)
    }
}
class IngotSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER"
    override val materialId: String = "ARGN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(6,0)
    }
}
class IngotLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_LEAD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(7,0)
    }
}
class IngotBronze(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRONZE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(8,0)
    }
}
class IngotBrass(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_BRASS"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(9,0)
    }
}
class IngotElectrum(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ELECTRUM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(10,0)
    }
}
class IngotSilverBillon(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SILVER_BILLON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(11,0)
    }
}
class IngotRosegold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_ROSEGOLD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(12,0)
    }
}
class IngotSolder(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_INGOT_SOLDER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(13,0)
    }
}
class SheetCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_COPPER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(0,1)
    }
}
class SheetIron(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_IRON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(1,1)
    }
}
class SheetSteel(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_STEEL"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(2,1)
    }
}
class SheetZinc(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ZINC"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(3,1)
    }
}
class SheetTin(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_TIN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(4,1)
    }
}
class SheetGold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_GOLD"
    override val materialId: String = "AURM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(5,1)
    }
}
class SheetSilver(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SILVER"
    override val materialId: String = "ARGN"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(6,1)
    }
}
class SheetLead(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_LEAD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(7,1)
    }
}
class SheetBronze(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_BRONZE"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(8,1)
    }
}
class SheetBrass(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_BRASS"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(9,1)
    }
}
class SheetElectrum(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ELECTRUM"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(10,1)
    }
}
class SheetSilverBillon(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SILVER_BILLON"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(11,1)
    }
}
class SheetRosegold(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_ROSEGOLD"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(12,1)
    }
}
class SheetSolder(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_SHEET_SOLDER"
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.metals").get(13,1)
    }
}