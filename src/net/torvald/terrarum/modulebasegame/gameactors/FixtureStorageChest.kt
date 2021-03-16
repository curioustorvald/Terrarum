package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.UIItemInventoryCatBar
import net.torvald.terrarum.UIItemInventoryCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.modulebasegame.ui.HasInventory
import net.torvald.terrarum.modulebasegame.ui.InventoryNegotiator
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureStorageChest : FixtureBase(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
        mainUI = UIStorageChest
) {

    init {
        setHitboxDimension(16, 16, 0, 0)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("itemplaceholder_16").texture, 16, 16))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    companion object {
        const val MASS = 2.0
    }
}


internal object UIStorageChest : UICanvas(), HasInventory {
    override var width = 512
    override var height = 512
    override var openCloseTime: Second = 0.0f

    private val negotiator = object : InventoryNegotiator() {
        override fun accept(item: GameItem, amount: Int) {
            printdbg(this, "Accept")
        }

        override fun reject(item: GameItem, amount: Int) {
            printdbg(this, "Reject")
        }
    }

    override fun getNegotiator() = negotiator

    override fun getFixtureInventory() {
        TODO("Not yet implemented")
    }

    override fun getPlayerInventory() {
        TODO("Not yet implemented")
    }

    private lateinit var catBar: UIItemInventoryCatBar

    private lateinit var itemList: UIItemInventoryItemGrid

    init {
        catBar = UIItemInventoryCatBar(
                this,
                100,
                50,
                500,
                500,
                false
        )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }
        itemList = UIItemInventoryItemGrid(
                this,
                catBar,
                Terrarum.ingame!!.actorNowPlaying!!.inventory, // just for a placeholder...
                100,
                100,
                4, 5,
                drawScrollOnRightside = false,
                drawWallet = true,
                keyDownFun = { _,_ -> Unit },
                touchDownFun = { _,_,_,_,_ -> itemListUpdate() }
        )

        handler.allowESCtoClose = true

        addUIitem(catBar)
        addUIitem(itemList)
    }

    private fun itemListUpdate() {
        itemList.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
    }

    override fun updateUI(delta: Float) {
        catBar.update(delta)
        itemList.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemList.render(batch, camera)
    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.paused = true
    }

    override fun doClosing(delta: Float) {
        Terrarum.ingame?.paused = false
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }



    override fun dispose() {
    }
}