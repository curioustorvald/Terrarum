package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.AppLoader.IS_DEVELOPMENT_BUILD
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ModuleEntryPoint
import net.torvald.terrarum.Point2d
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.Material

/**
 * The entry point for the module "Basegame"
 *
 * Created by minjaesong on 2018-06-21.
 */
class EntryPoint : ModuleEntryPoint() {

    override fun invoke() {

        ModMgr.GameBlockLoader.invoke("basegame")
        ModMgr.GameItemLoader.invoke("basegame")
        ModMgr.GameLanguageLoader.invoke("basegame")


        /////////////////////////////////
        // load customised item loader //
        /////////////////////////////////

        printdbg(this, "recording item ID ")

        // blocks.csvs are loaded by ModMgr beforehand
        // block items (blocks and walls are the same thing basically)
        for (i in ItemCodex.ITEM_TILES + ItemCodex.ITEM_WALLS) {
            ItemCodex.itemCodex[i] = object : GameItem() {
                override val originalID = i
                override var dynamicID = i
                override val isUnique: Boolean = false
                override var baseMass: Double = BlockCodex[i].density / 1000.0
                override var baseToolSize: Double? = null
                override var equipPosition = EquipPosition.HAND_GRIP
                override val originalName = BlockCodex[i % ItemCodex.ITEM_WALLS.first].nameKey
                override var stackable = true
                override var inventoryCategory = if (i in ItemCodex.ITEM_TILES) Category.BLOCK else Category.WALL
                override var isDynamic = false
                override val material = Material(0,0,0,0,0,0,0,0,0,0.0)

                init {
                    if (IS_DEVELOPMENT_BUILD)
                        print("$originalID ")
                }

                override fun startPrimaryUse(delta: Float): Boolean {
                    val ingame = Terrarum.ingame!! as Ingame

                    val mousePoint = Point2d(Terrarum.mouseTileX.toDouble(), Terrarum.mouseTileY.toDouble())

                    // check for collision with actors (BLOCK only)
                    if (this.inventoryCategory == Category.BLOCK) {
                        ingame.actorContainer.forEach {
                            if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                                return false
                        }
                    }

                    // return false if the tile is already there
                    if (this.inventoryCategory == Category.BLOCK &&
                        this.dynamicID == ingame.world.getTileFromTerrain(Terrarum.mouseTileX, Terrarum.mouseTileY) ||
                        this.inventoryCategory == Category.WALL &&
                        this.dynamicID - ItemCodex.ITEM_WALLS.start == ingame.world.getTileFromWall(Terrarum.mouseTileX, Terrarum.mouseTileY) ||
                        this.inventoryCategory == Category.WIRE &&
                        this.dynamicID - ItemCodex.ITEM_WIRES.start == ingame.world.getTileFromWire(Terrarum.mouseTileX, Terrarum.mouseTileY)
                    )
                        return false

                    // filter passed, do the job
                    // FIXME this is only useful for Player
                    if (i in ItemCodex.ITEM_TILES) {
                        ingame.world.setTileTerrain(
                                Terrarum.mouseTileX,
                                Terrarum.mouseTileY,
                                i
                        )
                    }
                    else {
                        ingame.world.setTileWall(
                                Terrarum.mouseTileX,
                                Terrarum.mouseTileY,
                                i
                        )
                    }

                    return true
                }
            }
        }



        println("Welcome back!")
    }

}