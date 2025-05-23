package net.torvald.terrarum.gameitems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.ReferencingRanges.PREFIX_ACTORITEM
import net.torvald.terrarum.ReferencingRanges.PREFIX_DYNAMICITEM
import net.torvald.terrarum.ReferencingRanges.PREFIX_VIRTUALTILE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import org.dyn4j.geometry.Vector2
import kotlin.math.min

typealias ItemID = String

fun ItemID.getModuleName(): String = this.substringBefore(':').substringAfter('@')


/**
 * Instances of the GameItem (e.g. net.torvald.terrarum.modulebasegame.gameitems.PickaxeCopper) are preferably referenced
 * from the ```<module>/items/itemid.csv``` file only, and not from the actual game code.
 *
 * Created by minjaesong on 2016-01-16.
 */
abstract class GameItem(val originalID: ItemID) : TooltipListener(), Comparable<GameItem>, Cloneable, TaggedProp {

    constructor() : this("-uninitialised-")

    open fun getLumCol(): Cvec = Cvec(0)

    /**
     * For items with COMBUSTIBLE tag only
     *
     * If it burns for 80 seconds in the furnace, the calories value would be 80*60=4800, assuming tickrate of 60.
     * Assuming one item weighs 10 kilograms, 1 game-calories is about 5 watt-hours.
     */
    @Transient open var calories = 0.0

    /**
     * For items with COMBUSTIBLE tag only
     *
     * Lower=more smoky. How likely it would produce smokes when burned on a furnace. Smokiness = 0.25 means the furnace would
     * emit smoke particle every 0.25 seconds.
     */
    @Transient open var smokiness = Float.POSITIVE_INFINITY

    open var dynamicID: ItemID = originalID
    /**
     * if the ID is a Actor range, it's an actor contained in a pocket.
     */
    //abstract val originalID: ItemID // WUT?! using init does not work!!


    /**
     *
     * e.g. Key Items (in a Pokémon sense); only the single instance of the item can exist in the pocket
     */
    @Transient var isUnique: Boolean = false
    
    /**
     * OriginalName is always read from Language files.
     *
     * Syntax example:
     *
     * - `BLOCK_AIR` – Prints out `Lang.get("BLOCK_AIR")`
     * - `BLOCK_AIR>>=BLOCK_WALL_NAME_TEMPLATE` – Prints out `Formatter().format(Lang.get("BLOCK_WALL_NAME_TEMPLATE"), Lang.get("BLOCK_AIR")).toString()`
     */
    @Transient open var originalName: String = ""


    var newName: String = ""
        private set

    /**
     * If custom name is configured, its name (`newName`) is returned; otherwise the `originalName` is returned.
     *
     * Assigning value to this field will set the `newName`
     */
    var name: String
        set(value) {
            newName = value
            isCustomName = true
        }
        get() = if (isCustomName) newName else Lang[originalName]
    open var isCustomName = false // true: reads from lang

    var nameColour = Color.WHITE

    var nameSecondary: String = ""

    /** In kg. Weapon with different material must have different mass. In this case, you MUST use IRON as a reference (or default) material. */
    abstract var baseMass: Double

    /** In kg */
    abstract var baseToolSize: Double?

    abstract var inventoryCategory: String // "weapon", "tool", "armor", etc. (all smallcaps)

    var itemProperties = ItemValue()

    /** Single-use then destroyed (e.g. Tiles) */
    @Transient var stackable: Boolean = true
    val isConsumable: Boolean
        get() = stackable && !canBeDynamic


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
    abstract val canBeDynamic: Boolean

    val isCurrentlyDynamic: Boolean
        get() = originalID != dynamicID

    /**
     * Where to equip the item.
     */
    @Transient open var equipPosition: Int = EquipPosition.NULL

    internal val material: Material
        get() = MaterialCodex.getOrDefault(materialId)
    abstract val materialId: String

    @Transient private var itemImage0: TextureRegion? = null

    /**
     * DO NOT READ FROM THIS VALUE: USE `ItemCodex.getItemImage(item)`;
     * this hack is needed to avoid the unsolvable issue regarding the ItemImage of the tiles, of which they
     * cannot be assigned because of this old note:
     *
     * Don't assign! Create getter -- there's inevitable execution order fuckup on ModMgr,
     * where it simultaneously wanted to be called before and after the Mod's EntryPoint if you assign value to it on init block.
     *
     *
     * Note to future adventurers:
     *
     * the following code did not solved the issue
     *
     * file: net.torvald.terrarum.modulebasegame.EntryPoint
     *
     * ```
     * override val itemImage: TextureRegion
     *     get() {
     *         val itemSheetNumber = App.tileMaker.tileIDtoItemSheetNumber(originalID)
     *         val bucket =  if (isWall) BlocksDrawer.tileItemWall else BlocksDrawer.tileItemTerrain
     *         return bucket.get(
     *                 itemSheetNumber % App.tileMaker.ITEM_ATLAS_TILES_X,
     *                 itemSheetNumber / App.tileMaker.ITEM_ATLAS_TILES_X
     *         )
     *     }
     * ```
     *
     */
    var itemImage: TextureRegion?
        get() = itemImage0
        set(textureRegion) {
            itemImage0 = textureRegion
            textureRegion?.let {
                val texdata = textureRegion.texture.textureData.also {
                    if (!it.isPrepared) it.prepare()
                }
                itemImagePixmap = Pixmap(textureRegion.regionWidth, textureRegion.regionHeight, texdata.format).also {
                    it.drawPixmap(
                        texdata.consumePixmap(),
                        0, 0, textureRegion.regionX, textureRegion.regionY, textureRegion.regionWidth, textureRegion.regionHeight
                    )
                    App.disposables.add(it)
                }
            }
        }


    fun setItemImage(pixmap: Pixmap) {
        val texture = TextureRegion(Texture(pixmap))
        App.disposables.add(texture.texture)
        this.itemImage0 = texture
        this.itemImagePixmap = pixmap
    }

    @Transient open var itemImagePixmap: Pixmap? = null; internal set
    @Transient open val itemImageGlow: TextureRegion? = null
    @Transient open val itemImageEmissive: TextureRegion? = null

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
    @Transient open var maxDurability: Int = 0

    /**
     * Float. NOT A MISTAKE
     */
    open var durability: Float = 0f

    @Transient var using = false // Always false when loaded from savegame

    /**
     * Tags defined by the basegame
     */
    @Transient var tags = HashSet<String>()

    /**
     * Tags added/removed by dynamic items (e.g. enchanting)
     */
    var modifiers = HashSet<String>()

    /**
     * Used to hold extra data. Keys are case-insensitive and gets converted to lowercase.
     */
    open val extra = Codex()

    @Transient open val disallowToolDragging = false

    /* called when the instance of the dynamic is loaded from the save; one may use this function to "re-sync" some values,
     * for the purpose of savegame format update, defence against rogue savegame manipulation, etc.
     */
    open fun reload() { }

    /**
     * Effects applied continuously while in pocket
     */
    open fun effectWhileInPocket(actor: ActorWithBody, delta: Float) { }

    /**
     * Effects applied immediately only once when picked up
     */
    open fun effectOnPickup(actor: ActorWithBody) { }

    /**
     * Apply effects (continuously or not) while primary button is down.
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * Primary button refers to all of these:
     * - Left mouse button (configurable)
     * - Screen tap
     * - XBox controller RT (fixed; LT is for jumping)
     *
     * @return amount of the item to remove from the inventory -- 0 or greater when used successfully, -1 when failed
     *
     * note: DO NOT super() this!
     *
     * Consumption function is executed in net.torvald.terrarum.gamecontroller.IngameController,
     * in which the function itself is defined in net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
     */
    open fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long = -1

    /**
     * Apply effects (continuously or not) while secondary button is down
     * The item will NOT be consumed, so you will want to consume it yourself by inventory.consumeItem(item)
     *
     * NOTE: please refrain from using this; secondary button is currently undefined in the gamepad and touch input.
     *
     * @return true when used successfully, false otherwise
     *
     * note: DO NOT super() this!
     */
    open fun startSecondaryUse(actor: ActorWithBody, delta: Float): Long = -1

    open fun endPrimaryUse(actor: ActorWithBody, delta: Float): Boolean = false
    open fun endSecondaryUse(actor: ActorWithBody, delta: Float): Boolean = false

    /**
     * Effects applied immediately only once when thrown (discarded) from pocket
     */
    open fun effectOnThrow(actor: ActorWithBody) { }

    /**
     * Effects applied (continuously or not) while being equipped (drawn/pulled out)
     */
    open fun effectWhileEquipped(actor: ActorWithBody, delta: Float) { }

    /**
     * Effects applied only once when unequipped
     */
    open fun effectOnUnequip(actor: ActorWithBody) { }

    
    override fun toString(): String {
        return "GameItem(dynID:$dynamicID,origID:$originalID)"
    }

    override fun hashCode(): Int {
        return dynamicID.hashCode()
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

    override fun compareTo(other: GameItem): Int = (this.dynamicID.substring(4).toInt() - other.dynamicID.substring(4).toInt()).sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0

    infix fun equipTo(actor: Pocketed) {
        if (equipPosition == EquipPosition.NULL)
            throw IllegalArgumentException("Item is not supposed to be equipped (equipPosition is NULL")

        actor.equipItem(this)
    }

    object EquipPosition {
        @JvmStatic val NULL = -1

        // you can add alias to address something like LEGGINGS, BREASTPLATE, RINGS, NECKLACES, etc.

        @JvmStatic val BODY_ARMOUR = 0
        @JvmStatic val BODY_FOOTWEAR = 1 // wings, jetpacks, etc.

        @JvmStatic val HEADGEAR = 2
        @JvmStatic val HAND_GAUNTLET = 3

        @JvmStatic val HAND_GRIP = 4
        @JvmStatic val TOOL_HOOKSHOT = 5

        @JvmStatic val BODY_BUFF1 = 6
        @JvmStatic val BODY_BUFF2 = 8
        @JvmStatic val BODY_BUFF3 = 10

        @JvmStatic val HAND_BUFF1 = 7
        @JvmStatic val HAND_BUFF2 = 9
        @JvmStatic val HAND_BUFF3 = 11

        // invisible from the inventory UI
        // intended for semi-permanant (de)buff (e.g. lifetime achivement, curse)
        // can be done with actorvalue and some more code, but it's easier to just make
        // such (de)buffs as an item.
        @JvmStatic val STIGMA_1 = 12
        @JvmStatic val STIGMA_2 = 13
        @JvmStatic val STIGMA_3 = 14
        @JvmStatic val STIGMA_4 = 15
        @JvmStatic val STIGMA_5 = 16
        @JvmStatic val STIGMA_6 = 17
        @JvmStatic val STIGMA_7 = 18
        @JvmStatic val STIGMA_8 = 19

        @JvmStatic val INDEX_MAX = 19
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
        @JvmStatic val FIXTURE = "fixture"
        @JvmStatic val MISC = "misc"
    }

    public override fun clone(): GameItem {
        val clonedItem = super.clone()
        // properly clone ItemValue
        (clonedItem as GameItem).itemProperties = this.itemProperties.clone()

        return clonedItem
    }

    fun makeDynamic(inventory: ActorInventory): GameItem {
        return this.clone().also {
            it.generateUniqueDynamicID(inventory)
            it.stackable = false
        }
    }




    private fun generateUniqueDynamicID(inventory: ActorInventory): GameItem {
        dynamicID = "$PREFIX_DYNAMICITEM:${Companion.generateUniqueDynamicID(inventory)}"
        ItemCodex.registerNewDynamicItem(dynamicID, this)
        return this
    }

    override fun hasTag(s: String) = tags.contains(s)

    companion object {

        val DURABILITY_NA = 0

        fun generateUniqueDynamicID(inventory: ActorInventory): Int {
            var ret: Int
            do {
                ret = (1..999999999).random()
            } while (inventory.contains("$PREFIX_DYNAMICITEM:$ret"))

            return ret
        }
    }

}

/**
 * When the mouse cursor is within the reach of the actor, make the given action happen.
 *
 * The reach is calculated using the actor's reach, reach buff actorvalue and the actor's scale.
 *
 * @param actor actor to check the reach
 * @param action (Double, Double, Int, Int) to Long; takes `Terrarum.mouseX`, `Terrarum.mouseY`, `Terrarum.mouseTileX`, `Terrarum.mouseTileY` as an input and returns non-negative integer if the action was successfully performed
 * @return an amount to remove from the inventory (>= 0); -1 if the action failed or not in interactable range
 */
fun mouseInInteractableRange(actor: ActorWithBody, action: (Double, Double, Int, Int) -> Long): Long {
    val mousePos1 = Vector2(Terrarum.mouseX, Terrarum.mouseY)
    val distMax = actor.actorValue.getAsDouble(AVKey.REACH)!! * (actor.actorValue.getAsDouble(AVKey.REACHBUFF) ?: 1.0) * actor.scale // perform some error checking here
    return if (distBetween(actor, mousePos1) <= distMax) action(Terrarum.mouseX, Terrarum.mouseY, Terrarum.mouseTileX, Terrarum.mouseTileY) else -1
}

/**
 * When the mouse cursor is within the reach of the actor, make the given action happen.
 *
 * The reach is calculated using the actor's reach, reach buff actorvalue and the actor's scale as well as the tool-material's reach bonus.
 *
 * @param actor actor to check the reach
 * @param item the item that represents the tool
 * @param reachMultiplierInTiles optional: a function that modifies the calculated reach
 * @param action returns boolean if the action was successfully performed
 * @return true if the action was successful, false if the action failed or the mouse is not in interactable range
 */
fun mouseInInteractableRangeTools(actor: ActorWithBody, item: GameItem?, reachMultiplierInTiles: (Int) -> Double = { it.toDouble() }, action: () -> Boolean): Boolean {
    val mousePos1 = Vector2(Terrarum.mouseX, Terrarum.mouseY)
    val reachBonus = (actor.actorValue.getAsDouble(AVKey.REACHBUFF) ?: 1.0) * actor.scale
    val distMax = actor.actorValue.getAsDouble(AVKey.REACH)!! * reachBonus // perform some error checking here
    val toolDistMax = (TILE_SIZED * reachMultiplierInTiles(item?.material?.toolReach ?: Int.MAX_VALUE)) * reachBonus
    return if (distBetween(actor, mousePos1) <= min(toolDistMax, distMax)) action() else false
}
//fun IntRange.pickRandom() = HQRNG().nextInt(this.last - this.first + 1) + this.first // count() on 200 million entries? Se on vitun hyvää idea
//fun IntArray.pickRandom(): Int = this[HQRNG().nextInt(this.size)]
//fun DoubleArray.pickRandom(): Double = this[HQRNG().nextInt(this.size)]

fun ItemID.isItem() = this.startsWith("item@")
fun ItemID.isWire() = this.startsWith("wire@")
fun ItemID.isDynamic() = this.startsWith("$PREFIX_DYNAMICITEM:") // aka module name 'dyn'
fun ItemID.isActor() = this.startsWith("$PREFIX_ACTORITEM@")
fun ItemID.isVirtual() = this.startsWith("$PREFIX_VIRTUALTILE@")
fun ItemID.isBlock() = !this.contains('@') && !this.isDynamic()
fun ItemID.isWall() = this.startsWith("wall@")
fun ItemID.isFluid() = this.startsWith("fluid@")
fun ItemID.isOre() = this.startsWith("ores@")

/**
 * Created by minjaesong on 2024-03-06.
 */
interface FixtureInteractionBlocked
