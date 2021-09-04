package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.Point2d
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.*

/**
 * Created by minjaesong on 2019-05-02.
 */
object BlockBase {

    /**
     * @param dontEncaseActors when set to true, blocks won't be placed where Actors are. You will want to set it false
     * for wire items, otherwise you want it to be true.
     */
    fun blockStartPrimaryUse(gameItem: GameItem, itemID: ItemID, delta: Float): Boolean {
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mousePoint = Point2d(Terrarum.mouseTileX.toDouble(), Terrarum.mouseTileY.toDouble())
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)

        // check for collision with actors (BLOCK only)
        // FIXME properly fix the collision detection: it OVERRIDES the tiki-torches which should not happen AT ALL
        // FIXME (h)IntTilewiseHitbox is badly defined
        // FIXME     actually it's this code: not recognising hitbox's starting point correctly. Use F9 for visualisation
        // FIXME the above issue is resolved by using intTilewise instead of hInt, but the hitbox itself is still
        // FIXME     badly defined
        
        if (gameItem.inventoryCategory == GameItem.Category.BLOCK) {
            var ret1 = true
            ingame.actorContainerActive.forEach {
                if (it is ActorWithBody && it.physProp.usePhysics && it.intTilewiseHitbox.intersects(mousePoint))
                    ret1 = false // return is not allowed here
            }
            if (!ret1) return ret1
        }

        // return false if the tile underneath is:
        // 0. same tile
        // 1. actorblock
        if (gameItem.inventoryCategory == GameItem.Category.BLOCK &&
            gameItem.dynamicID == ingame.world.getTileFromTerrain(mouseTile.x, mouseTile.y) ||
            gameItem.inventoryCategory == GameItem.Category.WALL &&
            gameItem.dynamicID == "wall@" + ingame.world.getTileFromWall(mouseTile.x, mouseTile.y) ||
            BlockCodex[ingame.world.getTileFromTerrain(mouseTile.x, mouseTile.y)].nameKey.contains("ACTORBLOCK_")
        )
            return false

        // filter passed, do the job
        // FIXME this is only useful for Player
        if (itemID.startsWith("wall@")) {
            ingame.world.setTileWall(
                    mouseTile.x,
                    mouseTile.y,
                    itemID.substring(5),
                    false
            )
        }
        else {
            ingame.world.setTileTerrain(
                    mouseTile.x,
                    mouseTile.y,
                    itemID,
                    false
            )
        }

        return true
    }

    fun blockEffectWhenEquipped(delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

    fun wireStartPrimaryUse(gameItem: GameItem, delta: Float): Boolean {
        val itemID = gameItem.originalID
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)

        // return false if the tile is already there
        if (ingame.world.getAllWiresFrom(mouseTile.x, mouseTile.y)?.searchFor(itemID) != null)
            return false

        // filter passed, do the job
        ingame.world.setTileWire(
                mouseTile.x,
                mouseTile.y,
                itemID,
                false
        )

        return true
    }

    fun wireEffectWhenEquipped(gameItem: GameItem, delta: Float) {
        val itemID = gameItem.originalID
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = WireCodex[itemID].renderClass
    }

    fun wireEffectWhenUnequipped(gameItem: GameItem, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}