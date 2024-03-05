package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.ui.UIQuickslotBar
import net.torvald.terrarum.sqr

/**
 * Created by minjaesong on 2016-03-15.
 */

class ActorInventory() : FixtureInventory() {

    // FIXME unless absolutely necessary, don't store full item object; only store its dynamicID

    @Transient lateinit var actor: Pocketed
        internal set

    constructor(actor: Pocketed, maxCapacity: Long, capacityMode: Int) : this() {
        this.actor = actor
        this.maxCapacity = maxCapacity
        this.capacityMode = capacityMode
    }

    override var maxCapacity: Long = 0
        get() = when (capacityMode) {
            CAPACITY_MODE_COUNT -> field
            CAPACITY_MODE_WEIGHT -> actor.actorValue.getAsInt(AVKey.ENCUMBRANCE)?.toLong() ?: field
            CAPACITY_MODE_NO_ENCUMBER -> 0x7FFFFFFFFFFFFFFFL
            else -> throw IllegalArgumentException()
        }
        set(value) {
            if (capacityMode == CAPACITY_MODE_COUNT)
                field = value
        }

    val maxCapacityByActor: Double
        get() = maxCapacity * ((actor.actorValue.getAsDouble(AVKey.SCALE) ?: 1.0) * (actor.actorValue.getAsDouble(AVKey.SCALEBUFF) ?: 1.0)).sqr()

    /**
     * How encumbered the actor is. 1.0 if weight of the items are exactly same as the capacity limit, >1.0 if encumbered.
     */
    override val encumberment: Double
        get() = if (capacityMode == CAPACITY_MODE_NO_ENCUMBER)
            0.0
        else if (capacityMode == CAPACITY_MODE_WEIGHT)
            capacity / maxCapacityByActor
        else
            0.0


    /**
     * List of all equipped items (tools, armours, rings, necklaces, etc.)
     *
     * It's your responsibility to make sure currently equipped item also exists in the `super.itemList`
     *
     * The ItemID must be `dynamicID`
     */
    val itemEquipped = Array<ItemID?>(GameItem.EquipPosition.INDEX_MAX) { null }

    /**
     * The ItemID must be `dynamicID`
     */
    val quickSlot = Array<ItemID?>(UIQuickslotBar.SLOT_COUNT) { null } // 0: Slot 1, 9: Slot 10


    override fun remove(itemID: ItemID, count: Long): Long = remove(ItemCodex[itemID]!!, count)
    /** Will check existence of the item using its Dynamic ID; careful with command order!
     *      e.g. re-assign after this operation */
    override fun remove(item: GameItem, count: Long): Long {
        return super.remove(item, count) { existingItem ->
            // unequip, if applicable
            actor.unequipItem(existingItem.itm)
            // also unequip on the quickslot
            actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                setQuickslotItem(it, null)
            }
        }
    }

    fun setQuickslotItem(slot: Int, dynamicID: ItemID?) {
        quickSlot[slot] = dynamicID
    }

    fun setQuickslotItemAtSelected(dynamicID: ItemID?) {
        actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
            setQuickslotItem(it, dynamicID)
        }
    }

    fun getQuickslotItem(slot: Int): InventoryPair? = searchByID(quickSlot[slot])

    fun consumeItem(item: GameItem, amount: Long = 1L) {
        val actor = this.actor as Actor

        if (amount < 0) throw IllegalArgumentException("Consuming negative amount of an item (expected >=0, got $amount)")

        if (item.isConsumable) {
            remove(item, amount)
        }
        else if (item.isUnique) {
            return // don't consume a bike!
        }
        else {
            val newItem: GameItem

            // unpack newly-made dynamic item (e.g. any weapon, floppy disk)
            if (item.canBeDynamic && !item.isCurrentlyDynamic) {
                itemEquipped[item.equipPosition] = null
                remove(item, 1)


                newItem = item.clone()
                newItem.generateUniqueDynamicID(this)

                newItem.stackable = false
                add(newItem)
                itemEquipped[newItem.equipPosition] = newItem.dynamicID //invSearchByDynamicID(newItem.dynamicID)!!.item // will test if some sketchy code is written. Test fail: kotlinNullpointerException

                // update quickslot designation as the item is being unpacked (e.g. using fresh new pickaxe)
                actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                    setQuickslotItem(it, newItem.dynamicID)
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
            val swingDmgToFrameDmg = App.UPDATE_RATE.toDouble() / actor.actorValue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            // damage the item
            newItem.durability -= (baseDamagePerSwing * swingDmgToFrameDmg / 1.5f).toFloat()
            if (newItem.durability <= 0) {
                remove(newItem, 1)

                // auto pull the same item if the player has one
                (actor as Pocketed).inventory.let { inv ->
                    inv.itemList.filter { ItemCodex[it.itm]?.originalID == newItem.originalID }.firstOrNull()?.let { (itm, qty) ->
                        printdbg(this, "AutoEquip item $itm")

                        actor.equipItem(itm)
                        // also unequip on the quickslot
                        actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                            setQuickslotItem(it, itm)
                        }
                    }
                }
            }

            //println("[ActorInventory] consumed; ${item.durability}")
        }
    }

    override fun clear(): List<InventoryPair> {
        val r = super.clear()
        itemEquipped.fill(null)
        quickSlot.fill(null)
        return r
    }
}

