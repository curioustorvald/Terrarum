package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.gameworld.GameWorld
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

    private val wireVectorChars = arrayOf(
            "· · · ·","· · · →","· · ↓ ·","· · ↓ →","· ← · ·","· ← · →","· ← ↓ ·","· ← ↓ →","↑ · · ·","↑ · · →","↑ · ↓ ·","↑ · ↓ →","↑ ← · ·","↑ ← · →","↑ ← ↓ ·","↑ ← ↓ →",
    )
    fun Int.toWireVectorBitsStr(): String = "[${wireVectorChars[this]}]($this)"
    fun Int.wireNodeMirror() = this.shl(4).or(this).ushr(2).and(15)
    fun wireNodesConnectedEachOther(oldToNewVector: Int, new: Int?, old: Int?): Boolean {
        return if (new == null || old == null || oldToNewVector == 0) false
        else {
            val newToOldVector = oldToNewVector.wireNodeMirror()
            //printdbg(this, "connected? ${one.and(15).toString(2).padStart(4, '0')} vs ${other.and(15).toString(2).padStart(4, '0')}")
            val p = oldToNewVector and old
            val q = newToOldVector and new
            if (p == 0 && q == 0) false
            else if (p > 0 && q > 0) true
            else throw IllegalStateException("oldToNewVector = ${oldToNewVector.toWireVectorBitsStr()}, new = ${new.toWireVectorBitsStr()}, old = ${old.toWireVectorBitsStr()}")
        }
    }

    private var initialMouseDownTileX = -1 // keeps track of the tile coord where the mouse was just down (not dragged-on)
    private var initialMouseDownTileY = -1

    private var oldTileX = -1
    private var oldTileY = -1

    private fun placeWirePieceTo(world: GameWorld, item: ItemID, x: Int, y: Int) {
        world.setTileWire(x, y, item, false, 0)
    }

    /**
     * This function assumes xy and oxy are neighboured and tiles are correctly placed
     */
    private fun setConnectivity(world: GameWorld, vec: Int, item: ItemID, x: Int, y: Int, ox: Int, oy: Int) {
        world.getWireGraphOf(x, y, item)!!.let { world.setWireGraphOf(x, y, item, vec.wireNodeMirror() or it) }
        world.getWireGraphOf(ox, oy, item)!!.let { world.setWireGraphOf(ox, oy, item, vec or it) }
    }

    private fun isNeighbouring(ww: Int, x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        // xy is always coerced into the world's dimension
        // i know the code below is lengthy and redundant but whatever
        return if (  x1 == 0     && x2 == ww-1 && y1 == y2) true
          else if (  x1 == ww-1  && x2 == 0    && y1 == y2) true
          else if ((x1 - x2).abs() == 1 && y1 == y2) true
          else if ((y1 - y2).abs() == 1 && x1 == x2) true
          else false
    }

    fun wireStartPrimaryUse(actor: ActorWithBody, gameItem: GameItem, delta: Float) = mouseInInteractableRange(actor) {

        val itemID = gameItem.originalID
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTileX = Terrarum.mouseTileX
        val mouseTileY = Terrarum.mouseTileY
        val ww = ingame.world.width

        if (Gdx.input.isButtonJustPressed(App.getConfigInt("config_mouseprimary")) ||
                !isNeighbouring(ww, mouseTileX, mouseTileY, oldTileX, oldTileY)) {
            initialMouseDownTileX = mouseTileX
            initialMouseDownTileY = mouseTileY
            oldTileX = mouseTileX
            oldTileY = mouseTileY
        }

        val thisTileWires = ingame.world.getAllWiresFrom(mouseTileX, mouseTileY)
        val oldTileWires = ingame.world.getAllWiresFrom(oldTileX, oldTileY)
        val thisTileWireCnx = ingame.world.getWireGraphOf(mouseTileX, mouseTileY, itemID)
        val oldTileWireCnx = ingame.world.getWireGraphOf(oldTileX, oldTileY, itemID)

        val thisTileOccupied = thisTileWires.first?.searchFor(itemID) != null
        val oldTileOccupied = oldTileWires.first?.searchFor(itemID) != null

        val oldToNewVector = if (mouseTileX == ww - 1 && oldTileX == 0) 4
                else if (mouseTileX == 0 && oldTileX == ww - 1) 1
                else if (mouseTileX - oldTileX == 1) 1
                else if (mouseTileX - oldTileX == -1) 4
                else if (mouseTileY - oldTileY == 1) 2
                else if (mouseTileY - oldTileY == -1) 8
                else 0 // if xy == oxy, the vector will be 0

        val connectedEachOther = wireNodesConnectedEachOther(oldToNewVector, thisTileWireCnx, oldTileWireCnx)
        val thisTileWasDraggedOn = initialMouseDownTileX != mouseTileX || initialMouseDownTileY != mouseTileY

        var ret = -1L

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
        if (!thisTileWasDraggedOn) {
            if (thisTileOccupied) return@mouseInInteractableRange -1
            else {
                placeWirePieceTo(ingame.world, itemID, mouseTileX, mouseTileY)
                ret = 1
            }
        }
        else {
            if (thisTileOccupied && connectedEachOther) {
                ret -1
            }
            else if (thisTileOccupied && oldTileOccupied) {
                setConnectivity(ingame.world, oldToNewVector, itemID, mouseTileX, mouseTileY, oldTileX, oldTileY)
                ret = 0
            }
            else {
                placeWirePieceTo(ingame.world, itemID, mouseTileX, mouseTileY)
                setConnectivity(ingame.world, oldToNewVector, itemID, mouseTileX, mouseTileY, oldTileX, oldTileY)
                ret = 1
            }
        }

        oldTileX = mouseTileX
        oldTileY = mouseTileY

        ret
    }

    fun wireEffectWhenEquipped(gameItem: GameItem, delta: Float) {
        val itemID = gameItem.originalID
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = WireCodex[itemID].renderClass
    }

    fun wireEffectWhenUnequipped(gameItem: GameItem, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}