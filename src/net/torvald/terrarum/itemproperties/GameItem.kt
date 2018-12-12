package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.graphics.Color
import net.torvald.random.HQRNG
import net.torvald.terrarum.ItemValue
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_DYNAMIC
import net.torvald.terrarum.langpack.Lang

typealias ItemID = Int

/**
 * Created by minjaesong on 2016-01-16.
 */
abstract class GameItem : Comparable<GameItem>, Cloneable {

    abstract var dynamicID: ItemID
    /**
     * if the ID is a Actor range, it's an actor contained in a pocket.
     */
    abstract val originalID: ItemID // WUT?! using init does not work!!


    /**
     *
     * e.g. Key Items (in a Pokémon sense); only the single instance of the item can exist in the pocket
     */
    abstract val isUnique: Boolean
    
    /**
     * OriginalName is always read from Language files.
     */
    abstract val originalName: String


    var newName: String = "I AM VITTUN PLACEHOLDER"
        private set

    var name: String
        set(value) {
            newName = value
            isCustomName = true
        }
        get() = if (isCustomName) newName else Lang[originalName]
    open var isCustomName = false // true: reads from lang

    var nameColour = Color.WHITE

    /** In kg */
    abstract var baseMass: Double

    /** In kg */
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

    abstract val material: Material

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
     * Set to zero (GameItem.DURABILITY_NA) if durability not applicable
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
    open fun effectWhileInPocket(delta: Float) { }

    /**
     * Effects applied immediately only once if picked up
     */
    open fun effectWhenPickedUp(delta: Float) { }

    /**
     * Apply effects (continuously or not) while primary button (usually left mouse button) is down.
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * @return true when used successfully, false otherwise
     *
     * note: DO NOT super() this!
     *
     * Consumption function is executed in net.torvald.terrarum.gamecontroller.IngameController,
     * in which the function itself is defined in net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
     */
    open fun startPrimaryUse(delta: Float): Boolean = false

    /**
     * Apply effects (continuously or not) while secondary button (usually right mouse button) is down
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * @return true when used successfully, false otherwise
     *
     * note: DO NOT super() this!
     */
    open fun startSecondaryUse(delta: Float): Boolean = false

    open fun endPrimaryUse(delta: Float): Boolean = false
    open fun endSecondaryUse(delta: Float): Boolean = false

    /**
     * Effects applied immediately only once if thrown from pocket
     */
    open fun effectWhenThrown(delta: Float) { }

    /**
     * Effects applied (continuously or not) when equipped (drawn)
     */
    open fun effectWhenEquipped(delta: Float) { }

    /**
     * Effects applied only once when unequipped
     */
    open fun effectOnUnequip(delta: Float) { }

    
    override fun toString(): String {
        return "$dynamicID/$originalID"
    }

    override fun hashCode(): Int {
        return dynamicID
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return dynamicID == (other as GameItem).dynamicID
    }

    fun unsetCustomName() {
        name = originalName
        isCustomName = false
        nameColour = Color.WHITE
    }

    override fun compareTo(other: GameItem): Int = (this.dynamicID - other.dynamicID).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    infix fun equipTo(actor: Pocketed) {
        if (equipPosition == EquipPosition.NULL)
            throw IllegalArgumentException("Item is not supposed to be equipped (equipPosition is NULL")

        actor.equipItem(this)
    }

    object EquipPosition {
        @JvmStatic val NULL = -1

        @JvmStatic val ARMOUR = 0
        // you can add alias to address something like LEGGINGS, BREASTPLATE, RINGS, NECKLACES, etc.
        @JvmStatic val BODY_BACK = 1 // wings, jetpacks, etc.
        @JvmStatic val BODY_BUFF2 = 2
        @JvmStatic val BODY_BUFF3 = 3
        @JvmStatic val BODY_BUFF4 = 4
        @JvmStatic val BODY_BUFF5 = 5
        @JvmStatic val BODY_BUFF6 = 6
        @JvmStatic val BODY_BUFF7 = 7
        @JvmStatic val BODY_BUFF8 = 8

        @JvmStatic val HAND_GRIP = 9
        @JvmStatic val HAND_GAUNTLET = 10
        @JvmStatic val HAND_BUFF2 = 11
        @JvmStatic val HAND_BUFF3 = 12
        @JvmStatic val HAND_BUFF4 = 13

        @JvmStatic val FOOTWEAR = 14

        @JvmStatic val HEADGEAR = 15

        @JvmStatic val INDEX_MAX = 15
    }

    object Category {
        @JvmStatic val WEAPON = "weapon"
        @JvmStatic val TOOL = "tool"
        @JvmStatic val ARMOUR = "armour"
        @JvmStatic val GENERIC = "generic"
        @JvmStatic val POTION = "potion"
        @JvmStatic val MAGIC = "magic"
        @JvmStatic val BLOCK = "block"
        @JvmStatic val WALL = "wall"
        @JvmStatic val WIRE = "wire"
        @JvmStatic val MISC = "misc"
    }

    override public fun clone(): GameItem {
        val clonedItem = super.clone()
        // properly clone ItemValue
        (clonedItem as GameItem).itemProperties = this.itemProperties.clone()

        return clonedItem
    }


    fun generateUniqueDynamicID(inventory: ActorInventory): GameItem {
        dynamicID = GameItem.generateUniqueDynamicID(inventory)
        return this
    }

    companion object {

        val DURABILITY_NA = 0

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
