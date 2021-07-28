package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.ReferencingRanges.PREFIX_ACTORITEM
import net.torvald.terrarum.ReferencingRanges.PREFIX_DYNAMICITEM
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.CanBeAnItem
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import java.util.*

/**
 * ItemCodex holds information of every item in the game, including blocks despite the 'item' naming
 *
 * Created by minjaesong on 2016-03-15.
 */
object ItemCodex {

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= ACTORID_MIN
     */
    private val itemCodex = HashMap<ItemID, GameItem>()
    val dynamicItemDescription = HashMap<ItemID, GameItem>()
    val dynamicToStaticTable = HashMap<ItemID, ItemID>()

    val ACTORID_MIN = ReferencingRanges.ACTORS.first

    private val itemImagePlaceholder: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_24") // copper pickaxe

    /**
     * @param: dynamicID string of "dyn:<random id>"
     */
    fun registerNewDynamicItem(dynamicID: ItemID, item: GameItem) {
        if (AppLoader.IS_DEVELOPMENT_BUILD) {
            printdbg(this, "Registering new dynamic item $dynamicID (from ${item.originalID})")
        }
        dynamicItemDescription[dynamicID] = item
        dynamicToStaticTable[dynamicID] = item.originalID
    }

    /**
     * Returns the item in the Codex. If the item is static, its clone will be returned (you are free to modify the returned item).
     * However, if the item is dynamic, the item itself will be returned. Modifying the item will affect the game.
     */
    operator fun get(code: ItemID?): GameItem? {
        if (code == null) return null

        if (code.startsWith(PREFIX_DYNAMICITEM))
            return dynamicItemDescription[code]!!
        else if (code.startsWith(PREFIX_ACTORITEM)) {
            val a = (Terrarum.ingame!! as TerrarumIngame).getActorByID(code.substring(6).toInt()) // actor item
            if (a is CanBeAnItem) return a.itemData

            return null
            //throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
        else // generic item
            return itemCodex[code]?.clone() // from CSV
    }

    fun dynamicToStaticID(dynamicID: ItemID) = dynamicToStaticTable[dynamicID]!!

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

        // dynamic item
        if (itemID.startsWith(PREFIX_DYNAMICITEM)) {
            return getItemImage(dynamicToStaticID(itemID))
        }
        // item
        else if (itemID.startsWith("item@")) {
            return itemCodex[itemID]?.itemImage
        }
        // wires
        else if (itemID.startsWith("wire@")) {
            return itemCodex[itemID]?.itemImage
        }
        // wall
        else if (itemID.startsWith("wall@")) {
            val itemSheetNumber = AppLoader.tileMaker.tileIDtoItemSheetNumber(itemID.substring(5))
            return BlocksDrawer.tileItemWall.get(
                    itemSheetNumber % AppLoader.tileMaker.ITEM_ATLAS_TILES_X,
                    itemSheetNumber / AppLoader.tileMaker.ITEM_ATLAS_TILES_X
            )
        }
        // terrain
        else {
            val itemSheetNumber = AppLoader.tileMaker.tileIDtoItemSheetNumber(itemID)
            return BlocksDrawer.tileItemTerrain.get(
                    itemSheetNumber % AppLoader.tileMaker.ITEM_ATLAS_TILES_X,
                    itemSheetNumber / AppLoader.tileMaker.ITEM_ATLAS_TILES_X
            )
        }

    }

    fun hasItem(itemID: Int): Boolean = dynamicItemDescription.containsKey(itemID)
}