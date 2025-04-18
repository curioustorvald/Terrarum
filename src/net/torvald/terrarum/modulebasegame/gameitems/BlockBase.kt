package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase

/**
 * Created by minjaesong on 2019-05-02.
 */
object BlockBase {

    /**
     * @param dontEncaseActors when set to true, blocks won't be placed where Actors are. You will want to set it false
     * for wire items, otherwise you want it to be true.
     */
    fun blockStartPrimaryUse(actor: ActorWithBody, gameItem: GameItem, itemID: ItemID, delta: Float) =
        mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
            val ingame = Terrarum.ingame!! as TerrarumIngame
            val mousePoint = Point2d(mtx.toDouble(), mty.toDouble())
            val mouseTile = Point2i(mtx, mty)
            val terrainUnderCursor = ingame.world.getTileFromTerrain(mouseTile.x, mouseTile.y)
            val wallUnderCursor = ingame.world.getTileFromWall(mouseTile.x, mouseTile.y)
            val terrProp = BlockCodex[terrainUnderCursor]
            val wallProp = BlockCodex[wallUnderCursor]
            val heldTileisWall = itemID.isWall()
            val heldProp = BlockCodex[itemID]

            // check for collision with actors (solid terrain block only)
            if ((gameItem.inventoryCategory == GameItem.Category.BLOCK || gameItem.tags.contains("ACTINGBLOCK")) && heldProp.isSolid) {
                var ret1 = true
                ingame.actorContainerActive.filter { it is ActorWithBody }.forEach { val it = it as ActorWithBody
                    if ((it is FixtureBase || it.physProp.usePhysics) && it.intTilewiseHitbox.intersects(mousePoint))
                        ret1 = false // return is not allowed here
                }
                if (!ret1) return@mouseInInteractableRange -1L
            }


            // return false if there is a same tile already (including non-solid!)
            if (heldTileisWall && wallUnderCursor == itemID || !heldTileisWall && terrainUnderCursor == itemID)
                return@mouseInInteractableRange -1L

            // return false if there is a "solid" tile already
            if (heldTileisWall && wallProp.isSolid ||
                !heldTileisWall && !terrProp.hasTag("INCONSEQUENTIAL"))
                return@mouseInInteractableRange -1L

            // filter passed, do the job
            // FIXME this is only useful for Player
            if (heldTileisWall) {
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
            PickaxeCore.makeNoiseTileBurst(actor, itemID)

            1L
        }

    fun blockEffectWhenEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

    private val wireVectorChars = arrayOf(
        "· · · ·","· · · →","· · ↓ ·","· · ↓ →",
        "· ← · ·","· ← · →","· ← ↓ ·","· ← ↓ →",
        "↑ · · ·","↑ · · →","↑ · ↓ ·","↑ · ↓ →",
        "↑ ← · ·","↑ ← · →","↑ ← ↓ ·","↑ ← ↓ →",
    )
    fun Int.toWireVectorBitsStr(): String = "[${wireVectorChars[this]}]($this)"
    private val wireMirrorLUT = arrayOf(0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15)
//    fun Int.wireNodeMirror() = this.shl(4).or(this).ushr(2).and(15)
    fun Int.wireNodeMirror() = wireMirrorLUT[this]
    private val wireExtendLUT = arrayOf(0,5,10,15,5,5,15,15,10,15,10,15,15,15,15,15)
    fun Int.wireNodeExtend() = wireExtendLUT[this]
    fun Int.isOrthogonalTo(other: Int) = (this.wireNodeExtend() and other.wireNodeExtend() == 0)
    fun wireNodesConnectedEachOther(oldToNewVector: Int, new: Int?, old: Int?): Boolean {
        return if (new == null || old == null || oldToNewVector == 0) false
        else {
            val newToOldVector = oldToNewVector.wireNodeMirror()
            //printdbg(this, "connected? ${one.and(15).toString(2).padStart(4, '0')} vs ${other.and(15).toString(2).padStart(4, '0')}")
            val p = oldToNewVector and old
            val q = newToOldVector and new
            if (p == 0 && q == 0) false
            else if (p > 0 && q > 0) true
            else throw IllegalStateException("oldToNewVector = ${oldToNewVector.toWireVectorBitsStr()}, newToOldVector = ${newToOldVector.toWireVectorBitsStr()}; old = ${old.toWireVectorBitsStr()}, new = ${new.toWireVectorBitsStr()}")
        }
    }

    private var initialMouseDownTileX = -1 // keeps track of the tile coord where the mouse was just down (not dragged-on)
    private var initialMouseDownTileY = -1

    private var oldTileX = -1
    private var oldTileY = -1

    private fun placeWirePieceTo(world: GameWorld, item: ItemID, x: Int, y: Int, cnx: Int = 0) {
        world.setTileWire(x, y, item, false, cnx)
    }

    /**
     * This function assumes xy and oxy are neighboured and tiles are correctly placed
     *
     * @param branching 0: no branching, no bend, 1: no branching, yes bend, 2: tee-only, 3: cross-only, 4: tee and cross
     */
    private fun setConnectivity(branching: Int, world: GameWorld, vec: Int, item: ItemID, x: Int, y: Int, ox: Int, oy: Int) {
        when (branching) {
            0 -> {
                // vec is guaranteed to be 5 or 10
                world.setWireGraphOf(x, y, item, vec)
                world.setWireGraphOf(ox, oy, item, vec)
            }
            else -> {
                world.getWireGraphOf(x, y, item)!!.let { world.setWireGraphOf(x, y, item, vec.wireNodeMirror() or it) }
                world.getWireGraphOf(ox, oy, item)!!.let { world.setWireGraphOf(ox, oy, item, vec or it) }
            }
        }
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

    fun wireStartPrimaryUse(actor: ActorWithBody, gameItem: GameItem, delta: Float) = mouseInInteractableRange(actor) { mx, my, mtx, mty ->

        val itemID = gameItem.originalID
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val ww = ingame.world.width

        val wirePlaceMode = WireCodex[itemID].branching

        if (Gdx.input.isButtonJustPressed(App.getConfigInt("config_mouseprimary")) ||
                !isNeighbouring(ww, mtx, mty, oldTileX, oldTileY)) {
            initialMouseDownTileX = mtx
            initialMouseDownTileY = mty
            oldTileX = mtx
            oldTileY = mty
        }

        val thisTileWires = ingame.world.getAllWiresFrom(mtx, mty)
        val oldTileWires = ingame.world.getAllWiresFrom(oldTileX, oldTileY)
        val thisTileWireCnx = ingame.world.getWireGraphOf(mtx, mty, itemID)
        val oldTileWireCnx = ingame.world.getWireGraphOf(oldTileX, oldTileY, itemID)

        val thisTileOccupied = thisTileWires.first?.searchFor(itemID) != null
        val oldTileOccupied = oldTileWires.first?.searchFor(itemID) != null

        val oldToNewVector = when (wirePlaceMode) {
            0 -> {
                // determine new vector by dividing the block cell in X-shape
                val mxt = mx - mtx * TILE_SIZE
                val myt = my - mty * TILE_SIZE
                // f(y)=myt
                // g(y)=-myt+TILE_SIZE
                // check if ( f(y) < x < g(y) OR f(y) > x > g(y) )
                // or, check if mouse is in the hourglass-shaped 🮚 area
                if ((myt < mxt && mxt < -myt+TILE_SIZE) || (myt > mxt && mxt > -myt+TILE_SIZE))
                    10
                else
                    5
            }
            else -> {
                if (mtx == ww - 1 && oldTileX == 0) 4
                else if (mtx == 0 && oldTileX == ww - 1) 1
                else if (mtx - oldTileX == 1) 1
                else if (mtx - oldTileX == -1) 4
                else if (mty - oldTileY == 1) 2
                else if (mty - oldTileY == -1) 8
                else 0 // if xy == oxy, the vector will be 0
            }
        }
        val connectedEachOther = when (wirePlaceMode) {
            0 -> thisTileOccupied && oldTileOccupied
            else -> wireNodesConnectedEachOther(oldToNewVector, thisTileWireCnx, oldTileWireCnx)
        }
        val thisTileWasDraggedOn = initialMouseDownTileX != mtx || initialMouseDownTileY != mty

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
                placeWirePieceTo(ingame.world, itemID, mtx, mty, oldToNewVector)
                ret = 1
            }
        }
        else {
            if (thisTileOccupied && connectedEachOther) {
                ret -1
            }
            else if (thisTileOccupied && oldTileOccupied) {
                setConnectivity(wirePlaceMode, ingame.world, oldToNewVector, itemID, mtx, mty, oldTileX, oldTileY)
                ret = 0
            }
            else {
                placeWirePieceTo(ingame.world, itemID, mtx, mty)
                setConnectivity(wirePlaceMode, ingame.world, oldToNewVector, itemID, mtx, mty, oldTileX, oldTileY)
                ret = 1
            }
        }

        oldTileX = mtx
        oldTileY = mty

        ret
    }

    fun wireEffectWhenEquipped(gameItem: GameItem, delta: Float) {
        val itemID = gameItem.originalID
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = WireCodex[itemID].renderClass
    }

    fun wireEffectWhenUnequipped(gameItem: GameItem) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}