package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory.Companion.CAPACITY_MODE_COUNT
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIInventoryCells.Companion.weightBarWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELLS_VRT
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_X
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.catBarWidth
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.controlHelpHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradEndCol
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradStartCol
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureStorageChest : FixtureBase {

    private constructor()

    constructor(nameFun: () -> String) : super(
            BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
            mainUI = UIStorageChest(),
            inventory = FixtureInventory(40, CAPACITY_MODE_COUNT),
            nameFun = nameFun
    ) {

        (mainUI as UIStorageChest).chestInventory = this.inventory!!
        (mainUI as UIStorageChest).chestNameFun = this.nameFun

        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("itemplaceholder_16").texture, 16, 16))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    companion object {
        const val MASS = 2.0
    }
}


internal class UIStorageChest : UICanvas(
        toggleKeyLiteral = AppLoader.getConfigInt("config_keyinventory"),
        toggleButtonLiteral = AppLoader.getConfigInt("config_gamepadstart"),
), HasInventory {

    lateinit var chestInventory: FixtureInventory
    lateinit var chestNameFun: () -> String

    override var width = AppLoader.screenSize.screenW
    override var height = AppLoader.screenSize.screenH
    override var openCloseTime: Second = 0.0f

    private val shapeRenderer = ShapeRenderer()

    private val negotiator = object : InventoryNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Int) {
            player.remove(item, amount)
            fixture.add(item, amount)
        }

        override fun reject(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Int) {
            fixture.remove(item, amount)
            player.add(item, amount)
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = chestInventory
    override fun getPlayerInventory(): FixtureInventory = Terrarum.ingame!!.actorNowPlaying!!.inventory

    private lateinit var catBar: UIItemInventoryCatBar
    private lateinit var itemListChest: UIItemInventoryItemGrid
    private lateinit var itemListPlayer: UIItemInventoryItemGrid

    private var encumbrancePerc = 0f
    private var isEncumbered = false

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    private var initialised = false

    private fun itemListUpdate() {
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        encumbrancePerc = getPlayerInventory().capacity.toFloat() / getPlayerInventory().maxCapacity
        isEncumbered = getPlayerInventory().isEncumbered
    }

    private fun setCompact(yes: Boolean) {
        itemListChest.isCompactMode = yes
        itemListChest.gridModeButtons[0].highlighted = !yes
        itemListChest.gridModeButtons[1].highlighted = yes
        itemListChest.itemPage = 0
        itemListChest.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListPlayer.isCompactMode = yes
        itemListPlayer.gridModeButtons[0].highlighted = !yes
        itemListPlayer.gridModeButtons[1].highlighted = yes
        itemListPlayer.itemPage = 0
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListUpdate()
    }

    override fun updateUI(delta: Float) {
        if (!initialised) {
            initialised = true

            catBar = UIItemInventoryCatBar(
                    this,
                    (AppLoader.screenSize.screenW - catBarWidth) / 2,
                    42 + (AppLoader.screenSize.screenH - internalHeight) / 2,
                    internalWidth,
                    catBarWidth,
                    false
            )
            catBar.selectionChangeListener = { old, new -> itemListUpdate() }
            itemListChest = UIItemInventoryItemGrid(
                    this,
                    catBar,
                    { getFixtureInventory() },
                    INVENTORY_CELLS_OFFSET_X - halfSlotOffset,
                    INVENTORY_CELLS_OFFSET_Y,
                    6, CELLS_VRT,
                    drawScrollOnRightside = false,
                    drawWallet = false,
                    keyDownFun = { _, _, _ -> Unit },
                    touchDownFun = { gameItem, amount, _ ->
                        if (gameItem != null) {
                            negotiator.reject(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                        }
                        itemListUpdate()
                    }
            )
            itemListPlayer = UIItemInventoryItemGrid(
                    this,
                    catBar,
                    { Terrarum.ingame!!.actorNowPlaying!!.inventory }, // literally a player's inventory
                    INVENTORY_CELLS_OFFSET_X - halfSlotOffset + (listGap + UIItemInventoryElem.height) * 7,
                    INVENTORY_CELLS_OFFSET_Y,
                    6, CELLS_VRT,
                    drawScrollOnRightside = true,
                    drawWallet = false,
                    keyDownFun = { _, _, _ -> Unit },
                    touchDownFun = { gameItem, amount, _ ->
                        if (gameItem != null) {
                            negotiator.accept(getPlayerInventory(), getFixtureInventory(), gameItem, amount)
                        }
                        itemListUpdate()
                    }
            )

            handler.allowESCtoClose = true

            // make grid mode buttons work together
            itemListChest.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
            itemListChest.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }
            itemListPlayer.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
            itemListPlayer.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

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


        val gradTopStart = (AppLoader.screenSize.screenH - internalHeight).div(2).toFloat()
        val gradBottomEnd = AppLoader.screenSize.screenH - gradTopStart

        shapeRenderer.inUse {
            shapeRenderer.rect(0f, gradTopStart, AppLoader.screenSize.screenWf, gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)
            shapeRenderer.rect(0f, gradBottomEnd, AppLoader.screenSize.screenWf, -gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, gradTopStart + gradHeight, AppLoader.screenSize.screenWf, internalHeight - (2 * gradHeight), gradEndCol, gradEndCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, 0f, AppLoader.screenSize.screenWf, gradTopStart, gradStartCol, gradStartCol, gradStartCol, gradStartCol)
            shapeRenderer.rect(0f, AppLoader.screenSize.screenHf, AppLoader.screenSize.screenWf, -(AppLoader.screenSize.screenHf - gradBottomEnd), gradStartCol, gradStartCol, gradStartCol, gradStartCol)
        }



        batch.begin()

        // UI items
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemListChest.render(batch, camera)
        itemListPlayer.render(batch, camera)


        blendNormal(batch)

        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        val encumbBarXPos = itemListPlayer.posX + itemListPlayer.width - weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - AppLoader.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = UIInventoryCells.encumbBarYPos
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        val chestName = chestNameFun()

        // encumbrance bar background
        batch.color = encumbBack
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                weightBarWidth, controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                if (getPlayerInventory().capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                controlHelpHeight - 6f
        )

        // chest name text
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, chestName, itemListChest.posX + 6f, encumbBarYPos - 3f)
        // encumb text
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.paused = true
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        Terrarum.ingame?.paused = false
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null) // required!
    }


    override fun dispose() {
        shapeRenderer.dispose()
    }
}