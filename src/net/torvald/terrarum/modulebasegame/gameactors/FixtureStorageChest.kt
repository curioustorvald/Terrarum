package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory.Companion.CAPACITY_MODE_COUNT
import net.torvald.terrarum.modulebasegame.ui.HasInventory
import net.torvald.terrarum.modulebasegame.ui.InventoryNegotiator
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_HOR
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.catBarWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradEndCol
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradStartCol
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryEquippedView
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureStorageChest : FixtureBase(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
        inventory = FixtureInventory(40, CAPACITY_MODE_COUNT),
        mainUI = UIStorageChest()
) {

    init {
        (mainUI as UIStorageChest).chest = this.inventory!!

        setHitboxDimension(16, 16, 0, 0)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("itemplaceholder_16").texture, 16, 16))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    companion object {
        const val MASS = 2.0
    }
}


internal class UIStorageChest : UICanvas(), HasInventory {

    lateinit var chest: FixtureInventory

    override var width = AppLoader.screenW
    override var height = AppLoader.screenH
    override var openCloseTime: Second = 0.0f

    private val shapeRenderer = ShapeRenderer()

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
    private lateinit var itemListChest: UIItemInventoryItemGrid
    private lateinit var itemListPlayer: UIItemInventoryItemGrid

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    private var initialised = false

    private fun itemListUpdate() {
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
    }

    override fun updateUI(delta: Float) {
        if (!initialised) {
            initialised = true

            catBar = UIItemInventoryCatBar(
                    this,
                    (AppLoader.screenW - catBarWidth) / 2,
                    42 + (AppLoader.screenH - internalHeight) / 2,
                    internalWidth,
                    catBarWidth,
                    false
            )
            catBar.selectionChangeListener = { old, new -> itemListUpdate() }
            itemListChest = UIItemInventoryItemGrid(
                    this,
                    catBar,
                    chest,
                    INVENTORY_CELLS_OFFSET_X - halfSlotOffset,
                    INVENTORY_CELLS_OFFSET_Y,
                    6, CELLS_VRT,
                    drawScrollOnRightside = false,
                    drawWallet = false,
                    keyDownFun = { _, _ -> Unit },
                    touchDownFun = { _, _, _, _, _ -> itemListUpdate() }
            )
            itemListPlayer = UIItemInventoryItemGrid(
                    this,
                    catBar,
                    Terrarum.ingame!!.actorNowPlaying!!.inventory, // literally a player's inventory
                    INVENTORY_CELLS_OFFSET_X - halfSlotOffset + (listGap + UIItemInventoryElem.height) * 7,
                    INVENTORY_CELLS_OFFSET_Y,
                    6, CELLS_VRT,
                    drawScrollOnRightside = true,
                    drawWallet = false,
                    keyDownFun = { _, _ -> Unit },
                    touchDownFun = { _, _, _, _, _ -> itemListUpdate() }
            )

            handler.allowESCtoClose = true

            addUIitem(catBar)
            addUIitem(itemListChest)
            addUIitem(itemListPlayer)
        }


        
        catBar.update(delta)
        itemListChest.update(delta)
        itemListPlayer.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // background fill
        batch.end()
        gdxSetBlendNormal()


        val gradTopStart = (AppLoader.screenH - internalHeight).div(2).toFloat()
        val gradBottomEnd = AppLoader.screenH - gradTopStart

        shapeRenderer.inUse {
            shapeRenderer.rect(0f, gradTopStart, AppLoader.screenWf, gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)
            shapeRenderer.rect(0f, gradBottomEnd, AppLoader.screenWf, -gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, gradTopStart + gradHeight, AppLoader.screenWf, internalHeight - (2 * gradHeight), gradEndCol, gradEndCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, 0f, AppLoader.screenWf, gradTopStart, gradStartCol, gradStartCol, gradStartCol, gradStartCol)
            shapeRenderer.rect(0f, AppLoader.screenHf, AppLoader.screenWf, -(AppLoader.screenHf - gradBottomEnd), gradStartCol, gradStartCol, gradStartCol, gradStartCol)
        }


        batch.begin()

        // UI items
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemListChest.render(batch, camera)
        itemListPlayer.render(batch, camera)
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
        shapeRenderer.dispose()
    }
}