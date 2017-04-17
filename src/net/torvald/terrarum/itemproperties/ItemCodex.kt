package net.torvald.terrarum.itemproperties

import net.torvald.point.Point2d
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithSprite
import net.torvald.terrarum.gamecontroller.mouseTileX
import net.torvald.terrarum.gamecontroller.mouseTileY
import net.torvald.terrarum.itemproperties.IVKey
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.mapdrawer.TilesDrawer
import net.torvald.terrarum.mapdrawer.TilesDrawer.wallOverlayColour
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TileCodex
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Image
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemCodex {

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= 16777216
     */
    private val itemCodex = HashMap<Int, InventoryItem>()
    private val dynamicItemDescription = HashMap<Int, KVHashMap>()

    val ITEM_TILES = 0..GameWorld.TILES_SUPPORTED - 1
    val ITEM_WALLS = GameWorld.TILES_SUPPORTED..GameWorld.TILES_SUPPORTED * 2 - 1
    val ITEM_WIRES = GameWorld.TILES_SUPPORTED * 2..GameWorld.TILES_SUPPORTED * 2 + 255
    val ITEM_STATIC = ITEM_WIRES.endInclusive + 1..32767
    val ITEM_DYNAMIC = 32768..1048575
    val ACTOR_ID_MIN = ITEM_DYNAMIC.endInclusive + 1


    private val itemImagePlaceholder = Image("./assets/item_kari_24.tga")


    init {
        // tile items (blocks and walls are the same thing basically)
        for (i in ITEM_TILES + ITEM_WALLS) {
            itemCodex[i] = object : InventoryItem() {
                override val id: Int = i
                override val isUnique: Boolean = false
                override var baseMass: Double = TileCodex[i].density / 1000.0
                override var baseToolSize: Double? = null
                override var equipPosition = EquipPosition.HAND_GRIP
                override val originalName = TileCodex[i % ITEM_WALLS.first].nameKey
                override var consumable = true
                override var inventoryCategory = Category.BLOCK

                init {
                    itemProperties[IVKey.ITEMTYPE] = if (i in ITEM_TILES)
                        IVKey.ItemType.BLOCK
                    else
                        IVKey.ItemType.WALL
                }

                override fun primaryUse(gc: GameContainer, delta: Int): Boolean {
                    return false
                    // TODO base punch attack
                }

                override fun secondaryUse(gc: GameContainer, delta: Int): Boolean {
                    val mousePoint = Point2d(gc.mouseTileX.toDouble(), gc.mouseTileY.toDouble())
                    // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
                    Terrarum.ingame!!.actorContainer.forEach {
                        if (it is ActorWithSprite && it.tilewiseHitbox.intersects(mousePoint))
                            return false
                    }

                    // return false if the tile is already there
                    if (this.id == Terrarum.ingame!!.world.getTileFromTerrain(gc.mouseTileX, gc.mouseTileY))
                        return false

                    // filter passed, do the job
                    // FIXME this is only useful for Player
                    if (i in ITEM_TILES) {
                        Terrarum.ingame!!.world.setTileTerrain(
                                gc.mouseTileX,
                                gc.mouseTileY,
                                i
                        )
                    }
                    else {
                        Terrarum.ingame!!.world.setTileWall(
                                gc.mouseTileX,
                                gc.mouseTileY,
                                i
                        )
                    }

                    return true
                }
            }
        }

        // test copper pickaxe
        itemCodex[ITEM_STATIC.first] = object : InventoryItem() {
            override val id = ITEM_STATIC.first
            override val isUnique = false
            override val originalName = "Test Pick"
            override var baseMass = 10.0
            override var baseToolSize: Double? = 10.0
            override var consumable = false
            override var maxDurability = 606 // this much tiles before breaking
            override var durability = maxDurability.toFloat()
            override var equipPosition = EquipPosition.HAND_GRIP
            override var inventoryCategory = Category.TOOL

            private val testmaterial = Material(
                    0,0,0,0,0,0,0,0,14,0.0 // quick test material Steel
            )

            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.PICK
                name = "Steel pickaxe"
            }

            override fun primaryUse(gc: GameContainer, delta: Int): Boolean {
                val mousePoint = Point2d(gc.mouseTileX.toDouble(), gc.mouseTileY.toDouble())
                val actorvalue = Terrarum.ingame!!.player!!.actorValue


                using = true

                // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
                Terrarum.ingame!!.actorContainer.forEach {
                    if (it is ActorWithSprite && it.tilewiseHitbox.intersects(mousePoint))
                        return false
                }

                // return false if there's no tile
                if (Tile.AIR == Terrarum.ingame!!.world.getTileFromTerrain(gc.mouseTileX, gc.mouseTileY))
                    return false


                // filter passed, do the job
                val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!

                return Terrarum.ingame!!.world.inflctTerrainDamage(
                        gc.mouseTileX,
                        gc.mouseTileY,
                        Calculate.pickaxePower(testmaterial) * swingDmgToFrameDmg.toFloat()
                )
            }

            override fun endPrimaryUse(gc: GameContainer, delta: Int): Boolean {
                using = false
                // reset action timer to zero
                Terrarum.ingame!!.player!!.actorValue[AVKey.__ACTION_TIMER] = 0.0
                return true
            }
        }

        // TODO read prop in Lua and fill itemCodex

        // read from save (if applicable) and fill dynamicItemDescription
    }

    operator fun get(code: Int): InventoryItem {
        if (code <= ITEM_STATIC.endInclusive) // generic item
            return itemCodex[code]!!.clone() // from CSV
        else if (code <= ITEM_DYNAMIC.endInclusive) {
            TODO("read from dynamicitem description (JSON)")
        }
        else {
            val a = Terrarum.ingame!!.getActorByID(code) // actor item
            if (a is CanBeAnItem) return a.itemData

            throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
    }

    fun getItemImage(code: Int): Image {
        if (code <= ITEM_TILES.endInclusive)
            return TilesDrawer.tilesTerrain.getSubImage((code % 16) * 16, code / 16)
        else if (code <= ITEM_WALLS.endInclusive) {
            val img = TilesDrawer.tilesTerrain.getSubImage((code % 16) * 16, code / 16)
            img.setImageColor(wallOverlayColour.r, wallOverlayColour.g, wallOverlayColour.b)
            return img
        }
        else if (code <= ITEM_WIRES.endInclusive)
            return TilesDrawer.tilesWire.getSubImage((code % 16) * 16, code / 16)
        else
            return itemImagePlaceholder
    }

    fun hasItem(itemID: Int): Boolean = dynamicItemDescription.containsKey(itemID)
}