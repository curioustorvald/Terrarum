package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.utils.RandomWordsName
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by minjaesong on 2021-12-13.
 */
open class FixtureItemBase(originalID: ItemID, val fixtureClassName: String) : GameItem(originalID) {

//    @Transient private val hash = RandomWordsName(4)


    @Transient protected open val makeFixture: () -> FixtureBase = {
        Class.forName(fixtureClassName).getDeclaredConstructor().newInstance() as FixtureBase
    }

    init {
        ItemCodex.fixtureToSpawnerItemID[fixtureClassName] = originalID


//        println("FixtureItemBase init: $hash")
    }

//    private var _ghostItem: FixtureBase? = null
    @Transient private var ghostItem = AtomicReference<FixtureBase>()

    override var dynamicID: ItemID = originalID
    override val originalName = "FIXTUREBASE"
    override var baseMass = 1.0
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()

    /**
     * Do not address the CommonResourcePool directly; just do it like this snippet:
     *
     * ```get() = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_source.tga")```
     */
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_32")

    override var baseToolSize: Double? = baseMass

    @Transient private var ghostInit = AtomicBoolean(false)

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
//        println("ghost: ${ghostItem}; ghostInit = $ghostInit; instance: $hash")
        if (!ghostInit.get()) {
            ghostItem.getAndSet(makeFixture())
            ghostInit.getAndSet(true)
//            printdbg(this, "ghost item initialised: $ghostItem")
        }

        (INGAME as TerrarumIngame).blockMarkingActor.let {
            it.setGhost(ghostItem.get())
            it.isVisible = true
            it.update(delta)
            it.setGhostColourBlock()
            mouseInInteractableRange(actor) { it.setGhostColourAllow(); true }
        }
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
//        ghostInit = false

        (INGAME as TerrarumIngame).blockMarkingActor.let {
            it.unsetGhost()
            it.isVisible = false
            it.setGhostColourNone()
        }
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) {
        val item = ghostItem.getAndSet(makeFixture()) // renew the "ghost" otherwise you'll be spawning exactly the same fixture again; old ghost will be returned

        item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY - item.blockBox.height + 1)
        // return true when placed, false when cannot be placed
    }

    /**
     * Also see: [net.torvald.terrarum.modulebasegame.gameactors.FixtureBase.Companion]
     */
    companion object {
        /** Always use with Getter! */
        fun getItemImageFromSheet(module: String, path: String, tileW: Int, tileH: Int): TextureRegion {
            val id = "$module/${path.replace('\\','/')}"
            return (CommonResourcePool.getOrPut(id) {
                TextureRegionPack(ModMgr.getGdxFile(module, path), tileW, tileH)
            } as TextureRegionPack).get(0,0)
        }

        /** Always use with Getter! */
        fun getItemImageFromSingleImage(module: String, path: String): TextureRegion {
            val id = "$module/${path.replace('\\','/')}"
            return CommonResourcePool.getOrPut(id) {
                TextureRegion(Texture(ModMgr.getGdxFile(module, path)))
            } as TextureRegion
        }
    }
}