package net.torvald.terrarum.gameitem

import net.torvald.random.HQRNG
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.itemproperties.ItemPropCodex
import org.newdawn.slick.GameContainer

/**
 * Items that has some more information (like floppy disk that contains UUID)
 *
 * @param baseItemID ID of the item that this item is based on
 *
 * Created by minjaesong on 16-09-08.
 */
open class DynamicItem(val baseItemID: Int?, newMass: Double? = null, newScale: Double? = null)
    : InventoryItemAdapter() {

    /**
     * Internal ID of an Item, Long
     * 0-4096: Tiles
     * 4097-32767: Static items
     * 32768-16777215: Dynamic items
     * >= 16777216: Actor RefID
     */
    override val itemID: Int = generateUniqueDynamicItemID()

    override val equipPosition: Int = // default to HAND_GRIP if no baseItemID given
            if (baseItemID != null)
                ItemPropCodex.getProp(baseItemID).equipPosition
            else
                EquipPosition.HAND_GRIP


    private fun generateUniqueDynamicItemID(): Int {
        var ret: Int
        do {
            ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
        } while (ItemPropCodex.hasItem(ret) || ret < ItemPropCodex.ITEM_DYNAMIC_MIN || ret > ItemPropCodex.ITEM_DYNAMIC_MAX) // check for collision
        return ret
    }

    /**
     * Weight of the item
     */
    override var mass: Double
        get() = itemInfo.getAsDouble(ItemInfoKey.MASS)!!
        set(value) {
            itemInfo[ItemInfoKey.MASS] = value
        }
    /**
     * Scale of the item. Real mass: mass * (scale^3)
     *
     * For static item, it must be 1.0. If you tinkered the item to be bigger,
     * it must be re-assigned as Dynamic Item
     */
    override var scale: Double
        get() = itemInfo.getAsDouble(ItemInfoKey.SCALE) ?: 1.0
        set(value) {
            itemInfo[ItemInfoKey.SCALE] = value
        }

    val itemInfo = KVHashMap()

    init {
        // set mass to the value from item codex using baseItemID
        if (baseItemID == null) {
            mass = newMass!!
        }
        else {
            mass = newMass ?: ItemPropCodex.getProp(baseItemID).mass
        }

        if (baseItemID == null) {
            scale = newScale!!
        }
        else {
            scale = newScale ?: ItemPropCodex.getProp(baseItemID).scale
        }
    }
}