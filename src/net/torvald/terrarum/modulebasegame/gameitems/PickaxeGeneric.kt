package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameitem.inInteractableRange
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.TOOL_DURABILITY_BASE
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2019-03-10.
 */
object PickaxeCore {
    /**
     * @param mx centre position of the digging
     * @param my centre position of the digging
     * @param mw width of the digging
     * @param mh height of the digging
     */
    fun startPrimaryUse(actor: ActorWithBody, delta: Float, item: GameItem?, mx: Int, my: Int, dropProbability: Double = 1.0, mw: Int = 1, mh: Int = 1) = inInteractableRange(actor) {
        // un-round the mx
        val ww = INGAME.world.width
        val apos = actor.centrePosPoint
        val mx = if (apos.x < 0.0 && mx >= ww / 2) mx - ww
                else if (apos.x > 0.0 && mx < ww / 2) mx + ww
                else mx
        val wmx = mx * TILE_SIZED
        val wmy = my * TILE_SIZED

        var xoff = -(mw / 2) // implicit flooring
        var yoff = -(mh / 2) // implicit flooring
        // if mw or mh is even number, make it closer toward the actor
        if (mw % 2 == 0 && apos.x > wmx) xoff += 1
        if (mh % 2 == 0 && apos.y > wmy) yoff += 1

        var usageStatus = false

        for (oy in 0 until mh) for (ox in 0 until mw) {
            val x = mx + xoff + ox
            val y = my + yoff + oy

            val mousePoint = Point2d(x.toDouble(), y.toDouble())
            val actorvalue = actor.actorValue

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
            if (Block.AIR == (INGAME.world).getTileFromTerrain(x, y)) {
                usageStatus = usageStatus or false
                continue
            }

            // filter passed, do the job
            val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!

            (INGAME.world).inflictTerrainDamage(
                    x, y,
                    Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg
            )?.let { tileBroken ->
                if (Math.random() < dropProbability) {
                    val drop = BlockCodex[tileBroken].drop
                    if (drop.isNotBlank()) {
                        INGAME.addNewActor(DroppedItem(drop, x * TILE_SIZE, y * TILE_SIZE))
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

    const val BASE_MASS_AND_SIZE = 10.0 // of iron pick
    const val TOOL_DURABILITY_BASE = 350 // of iron pick
}

/**
 * Created by minjaesong on 2017-07-17.
 */
class PickaxeCopper(originalID: ItemID) : GameItem(originalID) {

    override val originalName = "PACKAGED_PICK"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val material = MaterialCodex["CUPR"]
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items24").get(0,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.name = "Copper Pickaxe"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeIron(originalID: ItemID) : GameItem(originalID) {

    override val originalName = "PACKAGED_PICK"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val material = MaterialCodex["IRON"]
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items24").get(1,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.name = "Iron Pickaxe"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.startPrimaryUse(actor , delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeSteel(originalID: ItemID) : GameItem(originalID) {

    override val originalName = "PACKAGED_PICK"
    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val material = MaterialCodex["STAL"]
    override var baseMass = material.density.toDouble() / MaterialCodex["IRON"].density * BASE_MASS_AND_SIZE
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items24").get(2,0)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.maxDurability = (TOOL_DURABILITY_BASE * material.enduranceMod).roundToInt()
        super.durability = maxDurability.toFloat()
        super.name = "Steel Pickaxe"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.startPrimaryUse(actor, delta, this, Terrarum.mouseTileX, Terrarum.mouseTileY)
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}