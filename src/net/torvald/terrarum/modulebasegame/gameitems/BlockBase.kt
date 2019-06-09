package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.Point2d
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.realestate.LandUtil

/**
 * Created by minjaesong on 2019-05-02.
 */
object BlockBase {

    /**
     * @param dontEncaseActors when set to true, blocks won't be placed where Actors are. You will want to set it false
     * for wire items, otherwise you want it to be true.
     */
    fun blockStartPrimaryUse(gameItem: GameItem, itemID: Int, delta: Float): Boolean {
        val ingame = Terrarum.ingame!! as Ingame
        val mousePoint = Point2d(Terrarum.mouseTileX.toDouble(), Terrarum.mouseTileY.toDouble())
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)

        // check for collision with actors (BLOCK only)
        // FIXME properly fix the collision detection: it OVERRIDES the tiki-torches which should not happen AT ALL
        // FIXME (h)IntTilewiseHitbox is badly defined
        // FIXME    actually it's this code: not recognising hitbox's starting point correctly. Use F9 for visualisation
        if (gameItem.inventoryCategory == GameItem.Category.BLOCK) {
            var ret1 = true
            ingame.actorContainerActive.forEach {
                if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                    ret1 = false // return is not allowed here
            }
            if (!ret1) return ret1
        }

        // return false if the tile is already there
        if (gameItem.inventoryCategory == GameItem.Category.BLOCK &&
            gameItem.dynamicID == ingame.world.getTileFromTerrain(mouseTile.x, mouseTile.y) ||
            gameItem.inventoryCategory == GameItem.Category.WALL &&
            gameItem.dynamicID - ItemCodex.ITEM_WALLS.start == ingame.world.getTileFromWall(mouseTile.x, mouseTile.y)
            )
            return false

        // filter passed, do the job
        // FIXME this is only useful for Player
        if (itemID in ItemCodex.ITEM_TILES) {
            ingame.world.setTileTerrain(
                    mouseTile.x,
                    mouseTile.y,
                    itemID
            )
        }
        else {
            ingame.world.setTileWall(
                    mouseTile.x,
                    mouseTile.y,
                    itemID
            )
        }

        return true
    }

    fun blockEffectWhenEquipped(delta: Float) {
        IngameRenderer.selectedWireBitToDraw = 0
    }

    fun wireStartPrimaryUse(gameItem: GameItem, wireTypeBit: Int, delta: Float): Boolean {
        val ingame = Terrarum.ingame!! as Ingame
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)

        // return false if the tile is already there
        if (ingame.world.getWiringBlocks(mouseTile.x, mouseTile.y) and wireTypeBit != 0)
            return false

        // filter passed, do the job
        // FIXME this is only useful for Player
        ingame.world.addNewConduitTo(
                mouseTile.x,
                mouseTile.y,
                GameWorld.WiringNode(
                        LandUtil.getBlockAddr(ingame.world, mouseTile.x, mouseTile.y),
                        wireTypeBit,
                        0f
                )
        )

        return true
    }

    fun wireEffectWhenEquipped(typebit: Int, delta: Float) {
        IngameRenderer.selectedWireBitToDraw = typebit
    }
    
}