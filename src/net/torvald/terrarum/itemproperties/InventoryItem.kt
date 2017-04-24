package net.torvald.terrarum.itemproperties

import net.torvald.random.HQRNG
import net.torvald.terrarum.ItemValue
import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.Pocketed
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_DYNAMIC
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.langpack.Lang
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-01-16.
 */
abstract class InventoryItem : Comparable<InventoryItem>, Cloneable {

    abstract var dynamicID: Int
    /**
     * if the ID is a Actor range, it's an actor contained in a pocket.
     */
    abstract val originalID: Int // WUT?! using init does not work!!


    /**
     *
     * e.g. Key Items (in a Pokémon sense); only the single instance of the item can exist in the pocket
     */
    abstract val isUnique: Boolean
    
    /**
     * OriginalName is always read from Language files.
     */
    abstract protected val originalName: String

    private var newName: String = "I AM VITTUN PLACEHOLDER"

    var name: String
        set(value) {
            newName = value
            isCustomName = true
        }
        get() = if (isCustomName) newName else Lang[originalName]
    open var isCustomName = false // true: reads from lang

    var nameColour = Color.white


    abstract var baseMass: Double

    abstract var baseToolSize: Double?

    abstract var inventoryCategory: String // "weapon", "tool", "armor", etc. (all smallcaps)

    var itemProperties = ItemValue()

    /** Single-use then destroyed (e.g. Tiles), same as "consumable" */
    abstract var stackable: Boolean


    /**
     * DYNAMIC means the item ID should be generated on the fly whenever the item is created.
     *         This is to be used with weapons/armours/etc where multiple instances can exist, and
     *         each of them should be treated as different item. Because of this, new
     *         derivative instances (dynamically created items, e.g. used pickaxe) are not stackable.
     *
     *         ID Range: 32768..1048575 (ItemCodex.ITEM_DYNAMIC)
     *
     *         The opposite of this is called STATIC and their example is a Block.
     */
    abstract val isDynamic: Boolean

    /**
     * Where to equip the item
     */
    open val equipPosition: Int = EquipPosition.NULL

    open val material: Material? = null

    /**
     * Apparent mass of the item. (basemass * scale^3)
     */
    open var mass: Double
        get() = baseMass * scale * scale * scale
        set(value) { baseMass = value / (scale * scale * scale) }

    /**
     * Apparent tool size (or weight in kg). (baseToolSize  * scale^3)
     */
    open var toolSize: Double?
        get() = if (baseToolSize != null) baseToolSize!! * scale * scale * scale else null
        set(value) {
            if (value != null)
                if (baseToolSize != null)
                    baseToolSize = value / (scale * scale * scale)
                else
                    throw NullPointerException("baseToolSize is null; this item is not a tool or you're doing it wrong")
            else
                throw NullPointerException("null input; nullify baseToolSize instead :p")
        }

    /**
     * Scale of the item.
     *
     * For static item, it must be 1.0. If you tinkered the item to be bigger,
     * it must be re-assigned as Dynamic Item
     */
    open var scale: Double = 1.0

    /**
     * Set to zero if durability not applicable
     */
    open var maxDurability: Int = 0

    /**
     * Float. NOT A MISTAKE
     */
    open var durability: Float = 0f

    @Transient var using = false // Always false when loaded from savegame

    /**
     * Effects applied continuously while in pocket
     */
    open fun effectWhileInPocket(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied immediately only once if picked up
     */
    open fun effectWhenPickedUp(gc: GameContainer, delta: Int) { }

    /**
     * Apply effects (continuously or not) while primary button (usually left mouse button) is down.
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * @return true when used successfully, false otherwise
     *
     * note: DO NOT super(gc, g) this!
     *
     * Consumption function is executed in net.torvald.terrarum.gamecontroller.GameController,
     * in which the function itself is defined in net.torvald.terrarum.gameactors.ActorInventory
     */
    open fun primaryUse(gc: GameContainer, delta: Int): Boolean = false

    /**
     * Apply effects (continuously or not) while secondary button (usually right mouse button) is down
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * @return true when used successfully, false otherwise
     *
     * note: DO NOT super(gc, g) this!
     */
    open fun secondaryUse(gc: GameContainer, delta: Int): Boolean = false

    open fun endPrimaryUse(gc: GameContainer, delta: Int): Boolean = false
    open fun endSecondaryUse(gc: GameContainer, delta: Int): Boolean = false

    /**
     * Effects applied immediately only once if thrown from pocket
     */
    open fun effectWhenThrown(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied (continuously or not) when equipped (drawn)
     */
    open fun effectWhenEquipped(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied only once when unequipped
     */
    open fun effectOnUnequip(gc: GameContainer, delta: Int) { }

    
    override fun toString(): String {
        return "$dynamicID/$originalID"
    }

    override fun hashCode(): Int {
        return dynamicID
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return dynamicID == (other as InventoryItem).dynamicID
    }

    fun unsetCustomName() {
        name = originalName
        isCustomName = false
        nameColour = Color.white
    }

    override fun compareTo(other: InventoryItem): Int = (this.dynamicID - other.dynamicID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    infix fun equipTo(actor: Pocketed) {
        if (equipPosition == EquipPosition.NULL)
            throw IllegalArgumentException("Item is not supposed to be equipped (equipPosition is NULL")

        actor.equipItem(this)
    }

    object EquipPosition {
        const val NULL = -1

        const val ARMOUR = 0
        // you can add alias to address something like LEGGINGS, BREASTPLATE, RINGS, NECKLACES, etc.
        const val BODY_BACK = 1 // wings, jetpacks, etc.
        const val BODY_BUFF2 = 2
        const val BODY_BUFF3 = 3
        const val BODY_BUFF4 = 4
        const val BODY_BUFF5 = 5
        const val BODY_BUFF6 = 6
        const val BODY_BUFF7 = 7
        const val BODY_BUFF8 = 8

        const val HAND_GRIP = 9
        const val HAND_GAUNTLET = 10
        const val HAND_BUFF2 = 11
        const val HAND_BUFF3 = 12
        const val HAND_BUFF4 = 13

        const val FOOTWEAR = 14

        const val HEADGEAR = 15

        const val INDEX_MAX = 15
    }

    object Category {
        const val WEAPON = "weapon"
        const val TOOL = "tool"
        const val ARMOUR = "armour"
        const val GENERIC = "generic"
        const val POTION = "potion"
        const val MAGIC = "magic"
        const val BLOCK = "block"
        const val WALL = "wall"
        const val MISC = "misc"
    }

    override public fun clone(): InventoryItem {
        val clonedItem = super.clone()
        // properly clone ItemValue
        (clonedItem as InventoryItem).itemProperties = this.itemProperties.clone()

        return clonedItem
    }


    fun generateUniqueDynamicID(inventory: ActorInventory): InventoryItem {
        dynamicID = InventoryItem.generateUniqueDynamicID(inventory)
        return this
    }

    companion object {
        fun generateUniqueDynamicID(inventory: ActorInventory): Int {
            var ret: Int
            do {
                ret = ITEM_DYNAMIC.pickRandom()
            } while (inventory.contains(ret))
            return ret
        }
    }
}

fun IntRange.pickRandom() = HQRNG().nextInt(this.endInclusive - this.start + 1) + this.start // count() on 200 million entries? Se on vitun hyvää idea
fun IntArray.pickRandom(): Int = this[HQRNG().nextInt(this.size)]
fun DoubleArray.pickRandom(): Double = this[HQRNG().nextInt(this.size)]
