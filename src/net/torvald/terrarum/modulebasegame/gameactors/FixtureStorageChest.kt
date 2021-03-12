package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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

    private val negotiator = object : InventoryNegotiator {
        override fun getItemFilter(): List<String> = listOf(CAT_ALL)

        override fun accept(item: GameItem, amount: Int) {
            TODO("Not yet implemented")
        }

        override fun reject(item: GameItem, amount: Int) {
            TODO("Not yet implemented")
        }
    }

    override fun getNegotiator() = negotiator

    override fun getFixtureInventory() {
        TODO("Not yet implemented")
    }

    override fun getPlayerInventory() {
        TODO("Not yet implemented")
    }

    init {
        handler.allowESCtoClose = true
    }

    private val catBar = UIItemInventoryCatBar(
            this,
            100,
            50,
            500,
            500,
            {},
            false
    )

    private val itemList = UIItemInventoryItemGrid(
            this,
            catBar,
            Terrarum.ingame!!.actorNowPlaying!!.inventory, // just for a placeholder...
            100,
            100,
            4, 5,
            drawScrollOnRightside = true,
            drawWallet = true,
            listRebuildFun = { itemListUpdate() }
    )

    private fun itemListUpdate() {
        itemList.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
    }

    override fun updateUI(delta: Float) {
        catBar.update(delta)
        itemList.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE

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