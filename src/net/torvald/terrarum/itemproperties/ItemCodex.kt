package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.ReferencingRanges.PREFIX_ACTORITEM
import net.torvald.terrarum.ReferencingRanges.PREFIX_DYNAMICITEM
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitems.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.CanBeAnItem
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.serialise.SaveLoadError
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import java.io.File
import java.io.InvalidObjectException

typealias ItemRemapTable = java.util.HashMap<ItemID, ItemID>
typealias ItemTable = java.util.HashMap<ItemID, GameItem>

/**
 * ItemCodex holds information of every item in the game, including blocks despite the 'item' naming
 *
 * Created by minjaesong on 2016-03-15.
 */
class ItemCodex {

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= ACTORID_MIN
     */
    @Transient val itemCodex = ItemTable()
    val dynamicItemInventory = ItemTable()
    val dynamicToStaticTable = ItemRemapTable()

    @Transient val ACTORID_MIN = ReferencingRanges.ACTORS.first

    internal constructor()

    /**
     * Pair of <Fully Qualified class name for the Fixture, Corresponding ItemID of the spawner Item>
     */
    @Transient val fixtureToSpawnerItemID = HashMap<String, ItemID>()

    fun clear() {
        itemCodex.clear()
        dynamicItemInventory.clear()
        dynamicToStaticTable.clear()
    }

    /**
     * This method does not alter already-been-loaded itemCodex; only filles up dynamicitem-related fields
     *
     * Normally, the player's dynamicToStaticTable is what gets loaded first.
     */
    fun loadFromSave(savefile: File?, otherDynamicToStaticTable: ItemRemapTable, otherDynamicItemInventory: ItemTable) {
        otherDynamicToStaticTable.forEach { dynid, itemid ->
            printdbg(this, "Loadfromsave dynid $dynid ->> $itemid")
            dynamicToStaticTable[dynid]?.let {
                if (it != itemid) {
                    throw SaveLoadError(savefile, InvalidObjectException("Discrepancy detected -- currently loaded $dynid is mapped to $it, but ${savefile?.name} indicates it should map to $itemid"))
                }
            }
            dynamicToStaticTable[dynid] = itemid
        }
        otherDynamicItemInventory.forEach { dynid, item ->
            printdbg(this, "Loadfromsave dynitem $dynid ->> $item")
            dynamicItemInventory[dynid]?.let {
                if (it != item) {
                    throw SaveLoadError(savefile, InvalidObjectException("Discrepancy detected -- currently loaded $dynid is mapped to $it, but ${savefile?.name} indicates it should map to $item"))
                }
            }
            dynamicItemInventory[dynid] = item
        }
    }

    private val itemImagePlaceholder: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_24") // copper pickaxe

    /**
     * @param: dynamicID string of "dyn:<random id>"
     */
    fun registerNewDynamicItem(dynamicID: ItemID, item: GameItem) {
        printdbg(this, "Registering new dynamic item $dynamicID (from ${item.originalID})")
        dynamicItemInventory[dynamicID] = item
        dynamicToStaticTable[dynamicID] = item.originalID
    }

    /**
     * Returns the item in the Codex. If the item is static, its clone will be returned (you are free to modify the returned item).
     * However, if the item is dynamic, the item itself will be returned. Modifying the item will affect the game.
     */
    operator fun get(code: ItemID?): GameItem? {
        if (code == null) return null

        if (code.startsWith("$PREFIX_DYNAMICITEM:"))
            return dynamicItemInventory[code] ?: throw NullPointerException("No ItemProp with id $code")
        else if (code.startsWith("$PREFIX_ACTORITEM:")) {
            val a = (Terrarum.ingame!! as TerrarumIngame).getActorByID(code.substring(6).toInt()) // actor item
            if (a is CanBeAnItem) return a.itemData

            return null
            //throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
        else // generic item
            return itemCodex[code]?.clone() // from CSV
    }

    fun dynamicToStaticID(dynamicID: ItemID) = dynamicToStaticTable[dynamicID]!!
    fun fixtureToItemID(fixture: FixtureBase) = fixtureToSpawnerItemID[fixture.javaClass.canonicalName]!!

    /**
     * Mainly used by GameItemLoader
     */
    fun set(modname: String, code: Int, item: GameItem) {
        itemCodex["$modname:$code"] = item
    }

    operator fun set(code: ItemID, item: GameItem) {
        itemCodex[code] = item
    }

    fun getItemImage(item: GameItem?): TextureRegion? {
        if (item == null) return null

        return getItemImage(item.originalID)
    }

    fun getItemImage(itemID: ItemID?): TextureRegion? {
        if (itemID == null) return null

        if (itemID.isDynamic()) {
            return getItemImage(dynamicToStaticID(itemID))
        }
        else if (itemID.isItem()) {
            return itemCodex[itemID]?.itemImage
        }
        else if (itemID.isWire()) {
            return itemCodex[itemID]?.itemImage
        }
        else if (itemID.isWall()) {
            val itemSheetNumber = App.tileMaker.tileIDtoItemSheetNumber(itemID.substring(5))
            return BlocksDrawer.tileItemWall.get(
                    itemSheetNumber % App.tileMaker.TILES_IN_X,
                    itemSheetNumber / App.tileMaker.TILES_IN_X
            )
        }
        // else: terrain
        else {
            val itemSheetNumber = App.tileMaker.tileIDtoItemSheetNumber(itemID)
            return BlocksDrawer.tileItemTerrain.get(
                    itemSheetNumber % App.tileMaker.TILES_IN_X,
                    itemSheetNumber / App.tileMaker.TILES_IN_X
            )
        }

    }

    fun hasItem(itemID: ItemID): Boolean = dynamicItemInventory.containsKey(itemID)
}