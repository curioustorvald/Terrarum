package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.ReferencingRanges.PREFIX_ACTORITEM
import net.torvald.terrarum.ReferencingRanges.PREFIX_DYNAMICITEM
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.BlockProp
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.CanBeAnItem
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.ITEM_ATLAS_TILES_X
import java.util.*

/**
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

    init {
        //val ingame = Terrarum.ingame!! as Ingame // WARNING you can't put this here, ExceptionInInitializerError


        println("[ItemCodex] recording item ID ")

        // test copper pickaxe
        /*itemCodex[ITEM_STATIC.first] = object : GameItem() {
            override val originalID = ITEM_STATIC.first
            override var dynamicID = originalID
            override val isUnique = false
            override val originalName = ""
            override var baseMass = 10.0
            override var baseToolSize: Double? = 10.0
            override var stackable = true
            override var maxDurability = 147//606
            override var durability = maxDurability.toFloat()
            override var equipPosition = EquipPosition.HAND_GRIP
            override var inventoryCategory = Category.TOOL
            override val isDynamic = true
            override val material = Material(0,0,0,0,0,0,0,0,1,0.0)

            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.PICK
                name = "Stone pickaxe"
            }

            override fun startPrimaryUse(delta: Float): Boolean {
                val mousePoint = Point2d(Terrarum.mouseTileX.toDouble(), Terrarum.mouseTileY.toDouble())
                val actorvalue = ingame.actorNowPlaying.actorValue


                using = true

                // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
                // return false if hitting actors
                ingame.actorContainerActive.forEach {
                    if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                        return false
                }

                // return false if there's no tile
                if (Block.AIR == ingame.world.getTileFromTerrain(Terrarum.mouseTileX, Terrarum.mouseTileY))
                    return false


                // filter passed, do the job
                val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!

                ingame.world.inflictTerrainDamage(
                        Terrarum.mouseTileX,
                        Terrarum.mouseTileY,
                        Calculate.pickaxePower(ingame.actorNowPlaying, material) * swingDmgToFrameDmg
                )
                return true
            }

            override fun endPrimaryUse(delta: Float): Boolean {
                using = false
                // reset action timer to zero
                ingame.actorNowPlaying.actorValue[AVKey.__ACTION_TIMER] = 0.0
                return true
            }
        }*/


        // test water bucket
        /*itemCodex[9000] = object : GameItem(9000) {

            override val isUnique: Boolean = true
            override val originalName: String = "Infinite Water Bucket"

            override var baseMass: Double = 1000.0
            override var baseToolSize: Double? = null

            override var inventoryCategory: String = "tool"
            override var stackable: Boolean = false

            override val isDynamic: Boolean = false
            override val material: Material = Material()

            init {
                equipPosition = EquipPosition.HAND_GRIP
            }

            override fun startPrimaryUse(delta: Float): Boolean {
                val ingame = Terrarum.ingame!! as TerrarumIngame // must be in here
                ingame.world.setFluid(Terrarum.mouseTileX, Terrarum.mouseTileY, Fluid.WATER, 4f)
                return true
            }
        }


        // test lava bucket
        itemCodex[9001] = object : GameItem(9001) {

            override val isUnique: Boolean = true
            override val originalName: String = "Infinite Lava Bucket"

            override var baseMass: Double = 1000.0
            override var baseToolSize: Double? = null

            override var inventoryCategory: String = "tool"
            override var stackable: Boolean = false

            override val isDynamic: Boolean = false
            override val material: Material = Material()

            init {
                equipPosition = EquipPosition.HAND_GRIP
            }

            override fun startPrimaryUse(delta: Float): Boolean {
                val ingame = Terrarum.ingame!! as TerrarumIngame // must be in here
                ingame.world.setFluid(Terrarum.mouseTileX, Terrarum.mouseTileY, Fluid.LAVA, 4f)
                return true
            }
        }*/


        // read from save (if applicable) and fill dynamicItemDescription



        println()
    }

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
            return itemCodex[code]!!.clone() // from CSV
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
            return getItemImage(itemID)
        }
        // TODO: wires
        // wall
        else if (itemID.startsWith("wall@")) {
            val itemSheetNumber = CreateTileAtlas.tileIDtoItemSheetNumber(itemID.substring(5))
            return BlocksDrawer.tileItemWall.get(
                    itemSheetNumber % ITEM_ATLAS_TILES_X,
                    itemSheetNumber / ITEM_ATLAS_TILES_X
            )
        }
        // terrain
        else {
            val itemSheetNumber = CreateTileAtlas.tileIDtoItemSheetNumber(itemID)
            return BlocksDrawer.tileItemTerrain.get(
                    itemSheetNumber % ITEM_ATLAS_TILES_X,
                    itemSheetNumber / ITEM_ATLAS_TILES_X
            )
        }

    }

    fun hasItem(itemID: Int): Boolean = dynamicItemDescription.containsKey(itemID)
}