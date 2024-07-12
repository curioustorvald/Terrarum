package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.MixerTrackProcessor
import net.torvald.terrarum.audio.audiobank.MusicContainer
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
    private val tooltipHash = 10002L
    private val soundPlayedForThisTick = HashMap<ActorWithBody, Long>()

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
        if (!soundPlayedForThisTick.containsKey(actor)) {
            soundPlayedForThisTick[actor] = 0L
        }
        val updateTimer = INGAME.WORLD_UPDATE_TIMER

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
            if (BlockCodex[tile].hasAllTagsOf("TREE", "TREETRUNK", "TREESMALL")) {
                usageStatus = usageStatus or false
                continue
            }

            // filter passed, do the job
            val actionInterval = actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
            val swingDmgToFrameDmg = delta.toDouble() / actionInterval

            // prevent double-playing of sound effects
            if (soundPlayedForThisTick[actor]!! < updateTimer - 4 &&
                updateTimer % 11 == (Math.random() * 3).toLong()) {

                makeNoiseTileTouching(actor, tile)
                soundPlayedForThisTick[actor] = updateTimer
            }

            INGAME.world.inflictTerrainDamage(
                    x, y,
                    Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg,
                false
            ).let { (tileBroken, oreBroken) ->

                // drop ore
                if (oreBroken != null) {
                    if (Math.random() < dropProbability) {
                        val drop = OreCodex[oreBroken].item
                        dropItem(drop, x, y)
                    }
                }
                // drop tile
                else if (tileBroken != null) {
                    if (Math.random() < dropProbability) {
                        val drop = BlockCodex[tileBroken].drop
                        dropItem(drop, x, y)
                    }

                    // temperary: drop random disc
                    val itemprop = ItemCodex[tileBroken]
                    if (Math.random() < 1.0 / 4096.0 &&
                        (itemprop?.hasTag("CULTIVABLE") == true ||
                        itemprop?.hasTag("SAND") == true ||
                        itemprop?.hasTag("GRAVEL") == true)
                    ) {
                        dropItem(getRandomDisc(), x, y)
                    }
                }

                // make dust
                if (tileBroken != null || oreBroken != null) {
                    makeDust(tile, x, y, 9)
                    makeNoiseTileBurst(actor, tile)
                }
                else if (Math.random() < actionInterval) {
                    makeDust(tile, x, y, 1)
                }
            }

            usageStatus = usageStatus or true
        }


        usageStatus
    }

    fun dropItem(item: ItemID, tx: Int, ty: Int) {
        if (item.isBlank()) return
        INGAME.queueActorAddition(
            DroppedItem(
                item,
                (tx + 0.5) * TerrarumAppConfiguration.TILE_SIZED,
                (ty + 1.0) * TerrarumAppConfiguration.TILE_SIZED
            )
        )
    }

    fun getRandomDisc() = "item@basegame:${32769 + Math.random().times(9).toInt()}"

    private fun makeDustIndices(amount: Int): List<Int> {
        val ret = ArrayList<Int>()
        repeat((amount / 9f).ceilToInt()) {
            ret.addAll((0..8).toList().shuffled())
        }
        return ret.subList(0, amount)
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

        val indices = makeDustIndices(density)
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

    fun makeNoiseTileTouching(actor: ActorWithBody, tile: ItemID) {
        Terrarum.audioCodex.getRandomMining(BlockCodex[tile].material)?.let {
            actor.startAudio(it, 1.0)
        }
    }

    fun makeNoiseTileBurst(actor: ActorWithBody, tile: ItemID) {
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

        val overlayUIopen = (INGAME as? TerrarumIngame)?.uiBlur?.isVisible ?: false
        var tooltipSet = false

        val tooltipWasShown = tooltipShowing[tooltipHash] ?: false

        mouseInInteractableRangeTools(actor, tool) {
            val tileUnderCursor = INGAME.world.getTileFromOre(mx, my).item
            val playerCodex = (actor.actorValue.getAsString(AVKey.ORE_DICT) ?: "").split(',').filter { it.isNotBlank() }

            if (tileUnderCursor != Block.AIR && !overlayUIopen) {
                val itemForOre = OreCodex[tileUnderCursor].item
                val tileName = if (playerCodex.binarySearch(itemForOre) >= 0)
                    Lang[ItemCodex[itemForOre]!!.originalName]
                else "???"
                if (App.getConfigBoolean("basegame:showpickaxetooltip")) {
                    INGAME.setTooltipMessage(tileName)
                    tooltipShowing[tooltipHash] = true
                }
                tooltipSet = true
            }

            // play sound cue
            val mvec = Vector2(Terrarum.mouseX, Terrarum.mouseY)
            val dist = distBetween(actor, mvec)
            val relX = relativeXposition(actor, mvec)
            val distFallOff = 1.3 * 128.0
            val pan = 1.3 * relX / distFallOff
            val vol = MixerTrackProcessor.getVolFun(dist / distFallOff).coerceAtLeast(0.0)
            if (!tooltipWasShown && tooltipSet) {
                App.playGUIsound(soundCue, 0.18 * vol, pan.toFloat())
            }

            true // just a placeholder
        }

        if (App.getConfigBoolean("basegame:showpickaxetooltip") && !tooltipSet) tooltipShowing[tooltipHash] = false
    }

    private val soundCue = MusicContainer(
        "pickaxe_sound_cue",
        ModMgr.getFile("basegame", "audio/effects/accessibility/pickaxe_valuable.ogg"),
        toRAM = false
    ).also {
        App.disposables.add(it)
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
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,0)
    }

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
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(1,0)
    }

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
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(2,0)
    }

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
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(8,4)
    }

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
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(11,4)
    }

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