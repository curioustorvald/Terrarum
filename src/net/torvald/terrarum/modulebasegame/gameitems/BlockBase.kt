package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2019-05-02.
 */
object BlockBase {

    /**
     * @param dontEncaseActors when set to true, blocks won't be placed where Actors are. You will want to set it false
     * for wire items, otherwise you want it to be true.
     */
    fun blockStartPrimaryUse(actor: ActorWithBody, gameItem: GameItem, itemID: ItemID, delta: Float) = mouseInInteractableRange(actor) {
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
            if (!ret1) return@mouseInInteractableRange -1L
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
            return@mouseInInteractableRange 1L

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

        1L
    }

    fun blockEffectWhenEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

    private fun Int.shiftByTwo() = this.shl(4).or(this).ushr(2).and(15)
    private fun connectedEachOther(one: Int, other: Int) = one.shiftByTwo() and other != 0

    fun wireStartPrimaryUse(actor: ActorWithBody, gameItem: GameItem, delta: Float) = mouseInInteractableRange(actor) {
        val itemID = gameItem.originalID
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTile = Terrarum.getMouseSubtile4()

        val thisTileWires = ingame.world.getAllWiresFrom(mouseTile.x, mouseTile.y)
        val otherTileWires = ingame.world.getAllWiresFrom(mouseTile.nx, mouseTile.ny)
        val thisTileWireCnx = ingame.world.getWireGraphOf(mouseTile.x, mouseTile.y, itemID)
        val otherTileWireCnx = ingame.world.getWireGraphOf(mouseTile.x, mouseTile.y, itemID)

        // cases:
        // * regardless of vector, this tile was not dragged-on
        //  -> if this tile is occupied (use thisTileWires?.searchFor(itemID) != null): return -1
        //     else: place the tile, then return 1
        // * regardless of vector, this tile was dragged-on, and the oldtile is neighbouring tile
        //  -> if this tile is occupied and connectivities are already set (use connectedEachOther(thisTileWireCnx, otherTileWireCnx)): return -1
        //     else if this tile is occupied: set connectivity, then return 0
        //     else: place the tile, set connectivity, then return 1
        // (dragged-on: let net.torvald.terrarum.Terrarum record the tile that the mouse button was just down,
        //  and the poll again later; if tile now != recorded tile, it is dragged-on)

        // TODO


        TODO()
    }

    fun wireEffectWhenEquipped(gameItem: GameItem, delta: Float) {
        val itemID = gameItem.originalID
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = WireCodex[itemID].renderClass
    }

    fun wireEffectWhenUnequipped(gameItem: GameItem, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}