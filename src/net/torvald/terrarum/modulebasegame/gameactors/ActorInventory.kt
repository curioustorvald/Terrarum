package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.ui.UIQuickslotBar

/**
 * Created by minjaesong on 2016-03-15.
 */

class ActorInventory() : FixtureInventory() {

    // FIXME unless absolutely necessary, don't store full item object; only store its dynamicID

    @Transient lateinit var actor: Pocketed
        internal set

    constructor(actor: Pocketed, maxCapacity: Int, capacityMode: Int) : this() {
        this.actor = actor
        this.maxCapacity = maxCapacity
        this.capacityMode = capacityMode
    }

    /**
     * List of all equipped items (tools, armours, rings, necklaces, etc.)
     */
    val itemEquipped = Array<ItemID?>(GameItem.EquipPosition.INDEX_MAX) { null }

    val quickSlot = Array<ItemID?>(UIQuickslotBar.SLOT_COUNT) { null } // 0: Slot 1, 9: Slot 10


    override fun remove(itemID: ItemID, count: Int) = remove(ItemCodex[itemID]!!, count)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation */
    override fun remove(item: GameItem, count: Int) {
        super.remove(item, count) { existingItem ->
            // unequip, if applicable
            actor.unequipItem(existingItem.itm)
            // also unequip on the quickslot
            actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                setQuickBar(it, null)
            }
        }
    }

    fun setQuickBar(slot: Int, dynamicID: ItemID?) {
        quickSlot[slot] = dynamicID
    }

    fun getQuickslot(slot: Int): InventoryPair? = invSearchByDynamicID(quickSlot[slot])

    fun consumeItem(item: GameItem) {
        val actor = this.actor as Actor

        if (item.stackable && !item.isDynamic) {
            remove(item, 1)
        }
        else if (item.isUnique) {
            return // don't consume a bike!
        }
        else {
            val newItem: GameItem

            // unpack newly-made dynamic item (e.g. any weapon, floppy disk)
            if (item.isDynamic && item.originalID == item.dynamicID) {
                itemEquipped[item.equipPosition] = null
                remove(item, 1)


                newItem = item.clone()
                newItem.generateUniqueDynamicID(this)

                newItem.stackable = false
                add(newItem)
                itemEquipped[newItem.equipPosition] = newItem.dynamicID //invSearchByDynamicID(newItem.dynamicID)!!.item // will test if some sketchy code is written. Test fail: kotlinNullpointerException

                // update quickslot designation as the item is being unpacked (e.g. using fresh new pickaxe)
                actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                    setQuickBar(it, newItem.dynamicID)
                }

                // FIXME now damage meter (vital) is broken
            }
            else {
                newItem = item
            }



            // calculate damage value
            val baseDamagePerSwing = if (actor is ActorHumanoid)
                actor.avStrength / 1000.0
            else
                1.0 // TODO variable: scale, strength
            val swingDmgToFrameDmg = AppLoader.UPDATE_RATE.toDouble() / actor.actorValue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            // damage the item
            newItem.durability -= (baseDamagePerSwing * swingDmgToFrameDmg).toFloat()
            if (newItem.durability <= 0)
                remove(newItem, 1)

            //println("[ActorInventory] consumed; ${item.durability}")
        }
    }
}

