package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.blockproperties.FluidCodex
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.gameworld.FLUID_MIN_MASS
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed

/**
 * For example, `ItemFluidStoragePrototype("item@mymod:342", 3, 1, "mymod.buckets")` will result in:
 * - A bucket of type #2 that holds `fluid@mymod:3`
 *
 * And, `ItemFluidStoragePrototype("item@mymod:339", 0, 2, "mymod.buckets")` will result in:
 * - A bucket of type #3 that is empty
 *
 * @param originalID ID of the item. Module name is also retrieved from this string.
 * @param sheetX x-position within the sheet. Also determines the fluid ID. 0 for empty storage.
 * @param sheetY y-position within the sheet.
 * @param sheetName resource name of the sheet image.
 *
 * Created by minjaesong on 2024-09-14.
 */
open class ItemFluidStoragePrototype(originalID: ItemID, sheetX: Int, sheetY: Int, sheetName: String, keyInLang: String) : GameItem(originalID) {

    private val module = originalID.substringAfter('@').substringBefore(':')

    override var baseMass = 1.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.POTION
    override val canBeDynamic = false
    override val materialId = ""
    override var equipPosition = EquipPosition.HAND_GRIP

    @Transient private val fluid = if (sheetX == 0) null else "fluid@$module:$sheetX"

    override var originalName = if (fluid != null)
        "${FluidCodex[fluid].nameKey}>>=ITEM_BUCKET_TEMPLATE>>=$keyInLang"
    else
        "NULSTR>>=$keyInLang"

    init {
        itemImage = CommonResourcePool.getAsItemSheet(sheetName).get(sheetX,sheetY)
    }

    @Transient val itemIDRoot = originalID.substringAfter(':').toInt().and(0x7FFF_FF00)

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long { return mouseInInteractableRange(actor) { mx, my, mtx, mty ->
        val world = INGAME.world
        val terr = world.getTileFromTerrain(mtx, mty)
        val fluidAtWorld = world.getFluid(mtx, mty)
        val newItemID: ItemID

        if (BlockCodex[terr].isSolid) return@mouseInInteractableRange -1L

        // empty bucket -> filled bucket
        if (fluid == null) {
            if (ItemCodex[originalID]!!.hasTag("NOEXTREMETHERM") && FluidCodex[fluidAtWorld.type].therm !in 0..1) return@mouseInInteractableRange -1L
            if (fluidAtWorld.amount < 1f - FLUID_MIN_MASS) return@mouseInInteractableRange -1L

            // TODO respect the FLUIDROOMTEMP tag

            world.setFluid(mtx, mty, fluidAtWorld.type, fluidAtWorld.amount - 1f)
            newItemID = "item@$module:${itemIDRoot + fluidAtWorld.type.substringAfter(':').toInt()}"
        }
        // filled bucket -> empty bucket
        else {
            if (fluidAtWorld.type != Fluid.NULL && fluidAtWorld.type != fluid) return@mouseInInteractableRange -1L

            world.setFluid(mtx, mty, fluid, fluidAtWorld.amount + 1f)
            newItemID = "item@$module:${itemIDRoot}"
        }

        // spawn newItemID and add it to the inventory
        (actor as Pocketed).inventory.add(newItemID)
        1L
    }}
}


class ItemBucketWooden00(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 0, 0, "basegame.buckets", "ITEM_BUCKET_WOODEN")
class ItemBucketWooden01(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 1, 0, "basegame.buckets", "ITEM_BUCKET_WOODEN")
class ItemBucketWooden02(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 2, 0, "basegame.buckets", "ITEM_BUCKET_WOODEN")
class ItemBucketWooden03(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 3, 0, "basegame.buckets", "ITEM_BUCKET_WOODEN")


class ItemBucketIron00(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 0, 1, "basegame.buckets", "ITEM_BUCKET_IRON")
class ItemBucketIron01(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 1, 1, "basegame.buckets", "ITEM_BUCKET_IRON")
class ItemBucketIron02(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 2, 1, "basegame.buckets", "ITEM_BUCKET_IRON")
class ItemBucketIron03(originalID: ItemID) : ItemFluidStoragePrototype(originalID, 3, 1, "basegame.buckets", "ITEM_BUCKET_IRON")