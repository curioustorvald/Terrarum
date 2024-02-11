package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRangeTools
import net.torvald.terrarum.gameparticles.createRandomBlockParticle
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.TOOL_DURABILITY_BASE
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag
import org.dyn4j.geometry.Vector2
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2019-03-10.
 */
object PickaxeCore {
    private val hash = 10002L

    /**
     * @param mx centre position of the digging
     * @param my centre position of the digging
     * @param mw width of the digging
     * @param mh height of the digging
     */
    fun startPrimaryUse(
            actor: ActorWithBody, delta: Float, item: GameItem?, mx: Int, my: Int,
            dropProbability: Double = 1.0, mw: Int = 1, mh: Int = 1
    ) = mouseInInteractableRangeTools(actor, item) {
        // un-round the mx
        val ww = INGAME.world.width
        val hpww = ww * TILE_SIZE / 2
        val apos = actor.centrePosPoint
        val mx = if (apos.x - mx * TILE_SIZE< -hpww) mx - ww
                else if (apos.x - mx * TILE_SIZE >= hpww) mx + ww
                else mx

        var xoff = -(mw / 2) // implicit flooring
        var yoff = -(mh / 2) // implicit flooring
        // if mw or mh is even number, make it closer toward the actor
        if (mw % 2 == 0 && apos.x > mx * TILE_SIZE) xoff += 1
        if (mh % 2 == 0 && apos.y > my * TILE_SIZE) yoff += 1

        var usageStatus = false

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

            // return false if here's no tile
            if (Block.AIR == tile || BlockCodex[tile].isActorBlock) {
                usageStatus = usageStatus or false
                continue
            }
            // return false for punchable trees
            // non-punchable trees (= "log" tile placed by log item) must be mineable in order to make them useful as decor tiles
            if (BlockCodex[tile].hasAllTagOf("TREE", "TREETRUNK", "TREESMALL")) {
                usageStatus = usageStatus or false
                continue
            }

            // filter passed, do the job
            val actionInterval = actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
            val swingDmgToFrameDmg = delta.toDouble() / actionInterval
            val (oreOnTile, _) = INGAME.world.getTileFromOre(x, y)

            INGAME.world.inflictTerrainDamage(
                    x, y,
                    Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg
            ).let { tileBroken ->
                // tile busted
                if (tileBroken != null) {
                    if (Math.random() < dropProbability) {
                        val drop = if (oreOnTile != Block.AIR)
                            OreCodex[oreOnTile].item
                        else
                            BlockCodex[tileBroken].drop

                        if (drop.isNotBlank()) {
                            dropItem(drop, x, y)
                        }
                        makeDust(tile, x, y, 9)
                        makeNoise(actor, tile)
                    }


                    // temporary: spawn random record on prob 1/65536 when digging dirts
                    if (ItemCodex[tileBroken]?.hasTag("CULTIVABLE") == true && Math.random() < 1.0 / 65536.0) {
                        val drop = "item@basegame:${32769 + Math.random().times(9).toInt()}"
                        dropItem(drop, x, y)
                    }

                }
                // tile not busted
                if (Math.random() < actionInterval) {
                    makeDust(tile, x, y, 1)
                }
            }

            usageStatus = usageStatus or true
        }


        usageStatus
    }

    fun dropItem(item: ItemID, tx: Int, ty: Int) {
        INGAME.queueActorAddition(
            DroppedItem(
                item,
                (tx + 0.5) * TerrarumAppConfiguration.TILE_SIZED,
                (ty + 1.0) * TerrarumAppConfiguration.TILE_SIZED
            )
        )
    }

    private val pixelOffs = intArrayOf(2, 7, 12) // hard-coded assuming TILE_SIZE=16
    fun makeDust(tile: ItemID, x: Int, y: Int, density: Int = 9, drawCol: Color = Color.WHITE) {
        val pw = 3
        val ph = 3
        val xo = App.GLOBAL_RENDER_TIMER and 1
        val yo = App.GLOBAL_RENDER_TIMER.ushr(1) and 1

        val renderTag = App.tileMaker.getRenderTag(tile)
        val baseTilenum = renderTag.tileNumber
        val representativeTilenum = when (renderTag.maskType) {
            RenderTag.MASK_47 -> 17
            RenderTag.MASK_PLATFORM -> 7
            else -> 0
        }
        val tileNum = baseTilenum + representativeTilenum // the particle won't match the visible tile anyway because of the seasons stuff

        val indices = (0..8).toList().shuffled().subList(0, density)
        for (it in indices) {
            val u = pixelOffs[it % 3]
            val v = pixelOffs[it / 3]
            val pos = Vector2(
                TILE_SIZED * x + u + xo + 0.5,
                TILE_SIZED * y + v + yo + 2,
            )
            val veloMult = Vector2(
                1.0 * (if (Math.random() < 0.5) -1 else 1),
                (2.0 - (it / 3)) / 2.0 // 1, 0.5, 0
            )
            createRandomBlockParticle(tileNum, pos, veloMult, u, v, pw, ph).let {
                it.despawnUponCollision = true
                it.drawColour.set(drawCol)
                (Terrarum.ingame as TerrarumIngame).addParticle(it)
            }
        }
    }

    fun makeNoise(actor: ActorWithBody, tile: ItemID) {
        Terrarum.audioCodex.getRandomFootstep(BlockCodex[tile].material)?.let {
            actor.startAudio(it, 2.0)
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

    fun showOresTooltip(actor: ActorWithBody, tool: GameItem, mx: Int, my: Int): Unit {
        if (App.getConfigBoolean("basegame:showpickaxetooltip")) {

            val overlayUIopen = (INGAME as? TerrarumIngame)?.uiBlur?.isVisible ?: false
            var tooltipSet = false

            mouseInInteractableRangeTools(actor, tool) {
                val tileUnderCursor = INGAME.world.getTileFromOre(mx, my).item
                val playerCodex = (actor.actorValue.getAsString(AVKey.ORE_DICT) ?: "").split(',').filter { it.isNotBlank() }


                if (tileUnderCursor != Block.AIR && !overlayUIopen) {
                    val itemForOre = OreCodex[tileUnderCursor].item
                    val tileName = if (playerCodex.binarySearch(itemForOre) >= 0)
                        Lang[ItemCodex[itemForOre]!!.originalName]
                    else "???"
                    INGAME.setTooltipMessage(tileName)
                    tooltipShowing[hash] = true
                    tooltipSet = true
                }

                true // just a placeholder
            }

            if (!tooltipSet) tooltipShowing[hash] = false
        }
    }
}

/**
 * Created by minjaesong on 2017-07-17.
 */
class PickaxeCopper(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = true
    override val materialId = "CUPR"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("PICK")
        originalName = "ITEM_PICK_COPPER"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
            if (PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, this)
    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = PickaxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeIron(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = true
    override val materialId = "IRON"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("PICK")
        originalName = "ITEM_PICK_IRON"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
            if (PickaxeCore.startPrimaryUse(actor , delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, this)
    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = PickaxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeSteel(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = true
    override val materialId = "STAL"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,0)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("PICK")
        originalName = "ITEM_PICK_STEEL"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
            if (PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, this)
    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = PickaxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}

/**
 * Created by minjaesong on 2023-10-12.
 */
class PickaxeWood(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = true
    override val materialId = "WOOD"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8,4)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("PICK")
        originalName = "ITEM_PICK_WOODEN"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, this)
    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = PickaxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}

/**
 * Created by minjaesong on 2023-11-22.
 */
class PickaxeStone(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = true
    override val materialId = "ROCK"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11,4)

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        durability = maxDurability.toFloat()
        tags.add("PICK")
        originalName = "ITEM_PICK_STONE"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, this)
    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) = PickaxeCore.showOresTooltip(actor, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun effectOnUnequip(actor: ActorWithBody) { INGAME.setTooltipMessage(null) }

}