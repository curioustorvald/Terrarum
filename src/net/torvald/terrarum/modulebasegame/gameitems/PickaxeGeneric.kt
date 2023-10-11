package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
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
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.TOOL_DURABILITY_BASE
import org.dyn4j.geometry.Vector2
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

            val mousePoint = Point2d(x.toDouble(), y.toDouble())
            val actorvalue = actor.actorValue
            val tile = (INGAME.world).getTileFromTerrain(x, y)

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

            // filter passed, do the job
            val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
            val (oreOnTile, _) = INGAME.world.getTileFromOre(x, y)

            (INGAME.world).inflictTerrainDamage(
                    x, y,
                    Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg
            )?.let { tileBroken ->
                if (Math.random() < dropProbability) {
                    val drop = if (oreOnTile != Block.AIR)
                        OreCodex[oreOnTile].item
                    else
                        BlockCodex[tileBroken].drop

                    if (drop.isNotBlank()) {
                        INGAME.queueActorAddition(DroppedItem(drop, (x + 0.5) * TILE_SIZED, (y + 1.0) * TILE_SIZED))
                    }

                    repeat(9) {
                        val pos = Vector2(
                            x * TILE_SIZED + 2 + (4 * (it % 3)),
                            y * TILE_SIZED + 4 + (4 * (it / 3))
                        )
                        createRandomBlockParticle(tile, pos, 1.0 * (if (Math.random() < 0.5) -1 else 1)).let {
                            it.despawnUponCollision = true
                            (Terrarum.ingame as TerrarumIngame).addParticle(it)
                        }
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
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
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
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeIron(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
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
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}

/**
 * Created by minjaesong on 2019-03-10.
 */
class PickaxeSteel(originalID: ItemID) : GameItem(originalID) {
    internal constructor() : this("-uninitialised-")

    override var baseToolSize: Double? = BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val isDynamic = true
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
    override fun endPrimaryUse(actor: ActorWithBody, delta: Float) = PickaxeCore.endPrimaryUse(actor, delta, this)
}