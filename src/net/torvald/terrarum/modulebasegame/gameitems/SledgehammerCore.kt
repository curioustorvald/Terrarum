package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRangeTools
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.SledgehammerCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.SledgehammerCore.TOOL_DURABILITY_BASE
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-09-05.
 */
object SledgehammerCore {
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

        for (oy in 0 until mh) for (ox in 0 until mw) {
            val x = mx + xoff + ox
            val y = my + yoff + oy

            val mousePoint = Point2d(x.toDouble(), y.toDouble())
            val actorvalue = actor.actorValue
            val tile = INGAME.world.getTileFromWall(x, y)
            val tileTerrain = INGAME.world.getTileFromTerrain(x, y)

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

            // return false if here's no tile, or if the wall is covered by a solid tile
            if (Block.AIR == tile || BlockCodex[tile].isActorBlock || BlockCodex[tileTerrain].isSolid) {
                usageStatus = usageStatus or false
                continue
            }

            // filter passed, do the job
            val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            INGAME.world.inflictWallDamage(
                x, y,
                Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg
            )?.let { tileBroken ->
                if (Math.random() < dropProbability) {
                    val drop = BlockCodex[tileBroken].drop
                    if (drop.isNotBlank()) {
                        INGAME.queueActorAddition(DroppedItem("wall@$drop", x * TerrarumAppConfiguration.TILE_SIZED, y * TerrarumAppConfiguration.TILE_SIZED))
                    }
                }
            }

            usageStatus = usageStatus or true
        }


        usageStatus
    }

    fun endPrimaryUse(actor: ActorWithBody, delta: Float, item: GameItem): Boolean {

        item.using = false
        // reset action timer to zero
        actor.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
        return true
    }

    const val BASE_MASS_AND_SIZE = 20.0 // of iron sledgehammer
    const val TOOL_DURABILITY_BASE = 480 // of iron sledgehammer
}

class SledgehammerCopper(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override val originalName = "ITEM_SLEDGEHAMMER_COPPER"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val materialId = "CUPR"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(6,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.tags.add("HAMR")
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (SledgehammerCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = SledgehammerCore.endPrimaryUse(actor, delta, this)
}

class SledgehammerIron(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override val originalName = "ITEM_SLEDGEHAMMER_IRON"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val materialId = "IRON"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(7,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.tags.add("HAMR")
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (SledgehammerCore.startPrimaryUse(actor , delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = SledgehammerCore.endPrimaryUse(actor, delta, this)
}

class SledgehammerSteel(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override val originalName = "ITEM_SLEDGEHAMMER_STEEL"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val materialId = "STAL"
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.tags.add("HAMR")
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
        if (SledgehammerCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)) 0L else -1L
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = SledgehammerCore.endPrimaryUse(actor, delta, this)
}