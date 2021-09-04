package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Point2d
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.BASE_MASS_AND_SIZE
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore.TOOL_DURABILITY_BASE
import kotlin.math.roundToInt
import net.torvald.terrarum.*

/**
 * Created by minjaesong on 2019-03-10.
 */
object PickaxeCore {
    fun startPrimaryUse(delta: Float, item: GameItem): Boolean {
        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying ?: return false

        val mouseTileX = Terrarum.mouseTileX
        val mouseTileY = Terrarum.mouseTileY

        val mousePoint = Point2d(mouseTileX.toDouble(), mouseTileY.toDouble())
        val actorvalue = player.actorValue

        item.using = true

        // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
        // return false if hitting actors
        // ** below is commented out -- don't make actors obstruct the way of digging **
        /*var ret1 = true
        Terrarum.ingame!!.actorContainerActive.forEach {
            if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                ret1 =  false // return is not allowed here
        }
        if (!ret1) return ret1*/

        // return false if here's no tile
        if (Block.AIR == (Terrarum.ingame!!.world).getTileFromTerrain(mouseTileX, mouseTileY))
            return false

        // filter passed, do the job
        val swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!

        (Terrarum.ingame!!.world).inflictTerrainDamage(
                mouseTileX, mouseTileY,
                Calculate.pickaxePower(player, item.material) * swingDmgToFrameDmg
        )?.let { tileBroken ->
            val drop = BlockCodex[tileBroken].drop
            if (drop.isNotBlank()) {
                Terrarum.ingame!!.addNewActor(DroppedItem(drop, mouseTileX * TILE_SIZE, mouseTileY * TILE_SIZE))
            }
        }

        return true
    }

    fun endPrimaryUse(delta: Float, item: GameItem): Boolean {
        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
        if (player == null) return false

        item.using = false
        // reset action timer to zero
        player.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
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

    override fun startPrimaryUse(delta: Float) = PickaxeCore.startPrimaryUse(delta, this)
    override fun endPrimaryUse(delta: Float) = PickaxeCore.endPrimaryUse(delta, this)
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

    override fun startPrimaryUse(delta: Float) = PickaxeCore.startPrimaryUse(delta, this)
    override fun endPrimaryUse(delta: Float) = PickaxeCore.endPrimaryUse(delta, this)
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

    override fun startPrimaryUse(delta: Float) = PickaxeCore.startPrimaryUse(delta, this)
    override fun endPrimaryUse(delta: Float) = PickaxeCore.endPrimaryUse(delta, this)
}