package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRangeTools
import net.torvald.terrarum.gameparticles.createRandomBlockParticle
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.itemproperties.Item
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.AxeCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.AxeCore.TOOL_DURABILITY_BASE
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import org.dyn4j.geometry.Vector2
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-11-13.
 */
object AxeCore {

    fun startPrimaryUse(
        actor: ActorWithBody, delta: Float, item: GameItem?, mx: Int, my: Int,
        mw: Int = 1, mh: Int = 1, additionalCheckTags: List<String> = listOf()
    ) = mouseInInteractableRangeTools(actor, item) {
        val mh = 1

        // un-round the mx
        val ww = INGAME.world.width
        val hpww = ww * TerrarumAppConfiguration.TILE_SIZE / 2
        val apos = actor.centrePosPoint
        val mx = if (apos.x - mx * TerrarumAppConfiguration.TILE_SIZE < -hpww) mx - ww
        else if (apos.x - mx * TerrarumAppConfiguration.TILE_SIZE >= hpww) mx + ww
        else mx

        var xoff = -(mw / 2) // implicit flooring
        var yoff = -(mh / 2) // implicit flooring
        // if mw or mh is even number, make it closer toward the actor
        if (mw % 2 == 0 && apos.x > mx * TerrarumAppConfiguration.TILE_SIZE) xoff += 1
        if (mh % 2 == 0 && apos.y > my * TerrarumAppConfiguration.TILE_SIZE) yoff += 1

        var usageStatus = false

        val branchSize = if (item == null) 3 else 4

        for (oy in 0 until mh) for (ox in 0 until mw) {
            val x = mx + xoff + ox
            val y = my + yoff + oy

            val actorvalue = actor.actorValue
            val tile = INGAME.world.getTileFromTerrain(x, y)

            item?.using = true

            // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
            // return false if hitting actors
            // ** below is commented out -- don't make actors obstruct the way of digging **
            /*var ret1 = true
            INGAME.actorContainerActive.forEach {
                if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                    ret1 =  false // return is not allowed here
            }
            if (!ret1) return ret1*/


            // check if tile under mouse is leaves
            if (BlockCodex[tile].hasTag("LEAVES")) {
                val actionInterval = actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
                val swingDmgToFrameDmg = delta.toDouble() / actionInterval

                INGAME.world.inflictTerrainDamage(
                    x, y,
                    Calculate.hatchetPower(actor, item?.material) * swingDmgToFrameDmg
                ).let { tileBroken ->
                    // tile busted
                    if (tileBroken != null) {
                        removeLeaf(x, y, tileBroken.substringAfter(':').toInt() - 112)
                        PickaxeCore.makeDust(tile, x, y, 9)
                    }
                    // tile not busted
                    if (Math.random() < actionInterval) {
                        PickaxeCore.makeDust(tile, x, y, 1)
                    }
                }

                usageStatus = usageStatus or true
            }
            // check if tile under mouse is a tree
            else if (BlockCodex[tile].hasAllTag(listOf("TREE", "TREETRUNK") + additionalCheckTags)) {
                val actionInterval = actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
                val swingDmgToFrameDmg = delta.toDouble() / actionInterval

                INGAME.world.inflictTerrainDamage(
                    x, y,
                    Calculate.hatchetPower(actor, item?.material) * swingDmgToFrameDmg
                ).let { tileBroken ->
                    // tile busted
                    if (tileBroken != null) {
                        var upCtr = 0
                        var thisLeaf: ItemID? = null
                        val tileThereL = INGAME.world.getTileFromTerrain(x-1, y - upCtr)
                        val tileThereR = INGAME.world.getTileFromTerrain(x+1, y - upCtr)
                        val propThereL = BlockCodex[tileThereL]
                        val propThereR = BlockCodex[tileThereR]
                        val treeTrunkXoff = if (propThereL.hasAllTag(listOf("TREELARGE", "TREETRUNK"))) -1
                        else if (propThereR.hasAllTag(listOf("TREELARGE", "TREETRUNK"))) 1
                        else 0

                        if (treeTrunkXoff != 0) {
                            val tileThere = INGAME.world.getTileFromTerrain(x + treeTrunkXoff, y - upCtr)
                            val propThere = BlockCodex[tileThere]

                            INGAME.world.setTileTerrain(x + treeTrunkXoff, y - upCtr, Block.AIR, false)
                            PickaxeCore.dropItem(propThere.drop, x + treeTrunkXoff, y - upCtr)
                            PickaxeCore.makeDust(tile, x + treeTrunkXoff, y - upCtr, 2 + Math.random().roundToInt())
                        }

                        upCtr = 1
                        while (true) {
                            val tileHere = INGAME.world.getTileFromTerrain(x, y - upCtr)
                            val propHere = BlockCodex[tileHere]

                            if (propHere.hasAllTag(listOf("TREELARGE", "TREETRUNK"))) {
                                INGAME.world.setTileTerrain(x, y - upCtr, Block.AIR, false)
                                PickaxeCore.dropItem(propHere.drop, x, y - upCtr)
                                PickaxeCore.makeDust(tile, x, y - upCtr, 2 + Math.random().roundToInt())

                                if (treeTrunkXoff != 0) {
                                    val tileThere = INGAME.world.getTileFromTerrain(x + treeTrunkXoff, y - upCtr)
                                    val propThere = BlockCodex[tileThere]

                                    INGAME.world.setTileTerrain(x + treeTrunkXoff, y - upCtr, Block.AIR, false)
                                    PickaxeCore.dropItem(propThere.drop, x + treeTrunkXoff, y - upCtr)
                                    PickaxeCore.makeDust(tile, x + treeTrunkXoff, y - upCtr, 2 + Math.random().roundToInt())
                                }
                            }
                            else if (propHere.hasTag("TREETRUNK")) {
                                INGAME.world.setTileTerrain(x, y - upCtr, Block.AIR, false)
                                PickaxeCore.dropItem(propHere.drop, x, y - upCtr)
                                PickaxeCore.makeDust(tile, x, y - upCtr, 2 + Math.random().roundToInt())
                            }
                            else if (propHere.hasTag("LEAVES")) {
                                if (thisLeaf == null) thisLeaf = tileHere
                                // only remove leaves that matches the species
                                // scan horizontally left
                                for (l in -1 downTo -branchSize) {
                                    val tileBranch = INGAME.world.getTileFromTerrain(x + l, y - upCtr)
                                    if (tileBranch == thisLeaf)
                                        removeLeaf(x + l, y - upCtr, thisLeaf.substringAfter(':').toInt() - 112)
                                    else break
                                }
                                // scan horizontally right
                                for (l in 1 .. branchSize) {
                                    val tileBranch = INGAME.world.getTileFromTerrain(x + l, y - upCtr)
                                    if (tileBranch == thisLeaf)
                                        removeLeaf(x + l, y - upCtr, thisLeaf.substringAfter(':').toInt() - 112)
                                    else break
                                }
                                // deal with the current tile
                                val tileBranch = INGAME.world.getTileFromTerrain(x, y - upCtr)
                                if (tileBranch == thisLeaf)
                                    removeLeaf(x, y - upCtr, thisLeaf.substringAfter(':').toInt() - 112)
                                else break
                            }
                            else {
                                break
                            }

                            upCtr += 1
                        }
                        // drop the item under cursor
                        PickaxeCore.dropItem(BlockCodex[tileBroken].drop, x, y) // todo use log item if applicable
                        PickaxeCore.makeDust(tile, x, y, 9)
                        PickaxeCore.makeNoise(actor, tile)
                    }
                    // tile not busted
                    if (Math.random() < actionInterval) {
                        PickaxeCore.makeDust(tile, x, y, 1)
                    }
                }

                usageStatus = usageStatus or true
            }
            // return false if here's no tile
            else {
                usageStatus = usageStatus or false
                continue
            }
        }


        usageStatus
    }

    private fun removeLeaf(x: Int, y: Int, species: Int) {
        val tileBack = INGAME.world.getTileFromTerrain(x, y)
        INGAME.world.setTileTerrain(x, y, Block.AIR, false)
        // drop items
        when (Math.random()) {
            in 0.0..0.10 -> Item.TREE_STICK
            in 0.20..0.26 -> "item@basegame:${160+species}"
            else -> null
        }?.let {
            PickaxeCore.dropItem(it, x, y)
            PickaxeCore.makeDust(tileBack, x, y, 2 + Math.random().roundToInt())
        }
    }

    fun endPrimaryUse(actor: ActorWithBody, item: GameItem?): Boolean {
        item?.using = false
        // reset action timer to zero
        actor.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
        return true
    }

    const val BASE_MASS_AND_SIZE = 10.0 // of iron pick
    const val TOOL_DURABILITY_BASE = 350 // of iron pick

}

/**
 * Created by minjaesong on 2023-11-14.
 */
class AxeCopper(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
    override val materialId = "CUPR"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(3,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("AXE")
        originalName = "ITEM_HATCHET_COPPER"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (AxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = AxeCore.endPrimaryUse(actor, this)
//    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = AxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}
/**
 * Created by minjaesong on 2023-11-14.
 */
class AxeIron(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
    override val materialId = "IRON"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(4,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("AXE")
        originalName = "ITEM_HATCHET_IRON"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (AxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = AxeCore.endPrimaryUse(actor, this)
    //    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = AxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}
/**
 * Created by minjaesong on 2023-11-14.
 */
class AxeSteel(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
    override val materialId = "STAL"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(5,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("AXE")
        originalName = "ITEM_HATCHET_STEEL"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (AxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = AxeCore.endPrimaryUse(actor, this)
    //    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = AxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}
/**
 * Created by minjaesong on 2023-11-14.
 */
class AxeWood(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
    override val materialId = "WOOD"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(9,4)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("AXE")
        originalName = "ITEM_HATCHET_WOODEN"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (AxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = AxeCore.endPrimaryUse(actor, this)
    //    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = AxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}
/**
 * Created by minjaesong on 2023-11-22.
 */
class AxeStone(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
    override val materialId = "ROCK"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(12,4)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("AXE")
        originalName = "ITEM_HATCHET_STONE"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (AxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = AxeCore.endPrimaryUse(actor, this)
    //    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = AxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}