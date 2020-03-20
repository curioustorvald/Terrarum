package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.ENDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.*
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory.Companion.CAPACITY_MODE_NO_ENCUMBER
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryDynamicList.Companion.CAT_ALL
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT
import net.torvald.terrarum.ui.UIUtils
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        var actor: Pocketed,

        toggleKeyLiteral: Int? = null, toggleButtonLiteral: Int? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    private val debugvals = false
    
    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH

    private val REQUIRED_MARGIN = 166 // hard-coded value. Don't know the details

    private val CELLS_HOR = 10
    private val CELLS_VRT = (AppLoader.screenH - REQUIRED_MARGIN - 134 + UIItemInventoryDynamicList.listGap) / // 134 is another magic number
                            (UIItemInventoryElemSimple.height + UIItemInventoryDynamicList.listGap)

    private val itemListToEquipViewGap = UIItemInventoryDynamicList.listGap // used to be 24; figured out that the extra gap does nothig

    val internalWidth: Int = UIItemInventoryDynamicList.getEstimatedW(CELLS_HOR) + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
    val internalHeight: Int = REQUIRED_MARGIN + UIItemInventoryDynamicList.getEstimatedH(CELLS_VRT) // grad_begin..grad_end..contents..grad_begin..grad_end

    init {
        handler.allowESCtoClose = true
        CommonResourcePool.addToLoadingList("inventory_caticons") {
            TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    internal val catIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_caticons")
    internal val catArrangement: IntArray = intArrayOf(9,6,7,1,0,2,3,4,5,8)
    internal val catIconsMeaning = listOf( // sortedBy: catArrangement
            arrayOf(GameItem.Category.WEAPON),
            arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
            arrayOf(GameItem.Category.ARMOUR),
            arrayOf(GameItem.Category.GENERIC),
            arrayOf(GameItem.Category.POTION),
            arrayOf(GameItem.Category.MAGIC),
            arrayOf(GameItem.Category.BLOCK),
            arrayOf(GameItem.Category.WALL),
            arrayOf(GameItem.Category.MISC),
            arrayOf(CAT_ALL)
    )


    private val SP = "${0x3000.toChar()} "
    val listControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}$ENDASH${0x2009.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["CONTEXT_ITEM_MAP"]}$SP" +
            "$gamepadLabelRT ${Lang["MENU_LABEL_MENU"]}$SP" +
            "$gamepadLabelWest ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "$gamepadLabelNorth$gamepadLabelLStick ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$gamepadLabelEast ${Lang["GAME_INVENTORY_DROP"]}"
    val minimapControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_ACTION_MOVE_VERB"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelRStick ${Lang["GAME_ACTION_MOVE_VERB"]}$SP" +
            "$gamepadLabelRT ${Lang["GAME_INVENTORY"]}"
    val gameMenuControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["GAME_INVENTORY"]}"
    val controlHelpHeight = AppLoader.fontGame.lineHeight

    private var encumbrancePerc = 0f
    private var isEncumbered = false


    val catBarWidth = 330
    val categoryBar = UIItemInventoryCatBar(
            this,
            (AppLoader.screenW - catBarWidth) / 2,
            42 + (AppLoader.screenH - internalHeight) / 2,
            catBarWidth
    )


    override var openCloseTime: Second = 0.0f


    internal val itemList: UIItemInventoryDynamicList =
            UIItemInventoryDynamicList(
                    this,
                    actor.inventory,
                    0 + (AppLoader.screenW - internalWidth) / 2,
                    107 + (AppLoader.screenH - internalHeight) / 2,
                    CELLS_HOR, CELLS_VRT
            )


    private val equipped: UIItemInventoryEquippedView =
            UIItemInventoryEquippedView(
                    this,
                    actor.inventory,
                    actor as ActorWithBody,
                    internalWidth - UIItemInventoryEquippedView.WIDTH + (AppLoader.screenW - internalWidth) / 2,
                    107 + (AppLoader.screenH - internalHeight) / 2
            )
    private val gameMenu = arrayOf("MENU_LABEL_MAINMENU", "MENU_LABEL_DESKTOP", "MENU_OPTIONS_CONTROLS", "MENU_OPTIONS_SOUND", "MENU_LABEL_GRAPHICS")
    private val gameMenuListHeight = DEFAULT_LINE_HEIGHT * gameMenu.size
    private val gameMenuListWidth = 400
    private val gameMenuButtons = UIItemTextButtonList(
            this, gameMenu,
            AppLoader.screenW + (AppLoader.screenW - gameMenuListWidth) / 2,
            (itemList.height - gameMenuListHeight) / 2 + itemList.posY,
            gameMenuListWidth, gameMenuListHeight,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )

    private val SCREEN_MINIMAP = 2f
    private val SCREEN_INVENTORY = 1f
    private val SCREEN_MENU = 0f

    private var currentScreen = SCREEN_INVENTORY
    private var transitionRequested = false
    private var transitionOngoing = false
    private var transitionReqSource = SCREEN_INVENTORY
    private var transitionReqTarget = SCREEN_INVENTORY
    private var transitionTimer = 0f
    private val transitionLength = 0.212f


    private val transitionalUpdateUIs = ArrayList<UIItem>()
    private val transitionalUpdateUIoriginalPosX = ArrayList<Int>()

    
    private fun addToTransitionalGroup(item: UIItem) {
        transitionalUpdateUIs.add(item)
        transitionalUpdateUIoriginalPosX.add(item.posX)
    }

    private fun updateTransitionalItems() {
        for (k in 0..transitionalUpdateUIs.lastIndex) {
            val intOff = inventoryScrOffX.roundInt()
            transitionalUpdateUIs[k].posX = transitionalUpdateUIoriginalPosX[k] + intOff
        }
    }

    init {
        addItem(categoryBar)
        itemList.let { addItem(it) }
        equipped.let { addItem(it) }


        categoryBar.selectionChangeListener = { old, new  ->
            rebuildList()
            itemList.itemPage = 0 // set scroll to zero
            itemList.rebuild(catIconsMeaning[catArrangement[new]]) // have to manually rebuild, too!
        }


        rebuildList()


        addToTransitionalGroup(itemList)
        addToTransitionalGroup(equipped)
        addToTransitionalGroup(gameMenuButtons)

        // make gameMenuButtons work
        gameMenuButtons.selectionChangeListener = { old, new ->
            if (new == 0) {
                AppLoader.setScreen(TitleScreen(AppLoader.batch))
            }
            else if (new == 1) {
                Gdx.app.exit()
            }
        }

    }

    private var offsetX = ((AppLoader.screenW - internalWidth)  / 2).toFloat()
    private var offsetY = ((AppLoader.screenH - internalHeight) / 2).toFloat()



    override fun updateUI(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }


        categoryBar.update(delta)

        transitionalUpdateUIs.forEach { it.update(delta) }

        if (currentScreen > 1f + epsilon) {
            MinimapComposer.setWorld(Terrarum.ingame!!.world)
            MinimapComposer.update()
        }

        minimapRerenderTimer += Gdx.graphics.rawDeltaTime
    }

    private val gradStartCol = Color(0x404040_60)
    private val gradEndCol   = Color(0x000000_70)
    private val shapeRenderer = ShapeRenderer()
    private val gradHeight = 48f

    private val weightBarWidth = UIItemInventoryElemSimple.height * 2f + UIItemInventoryDynamicList.listGap

    private var xEnd = (AppLoader.screenW + internalWidth).div(2).toFloat()
    private var yEnd = (AppLoader.screenH + internalHeight).div(2).toFloat()

    private var minimapRerenderTimer = 0f
    private val minimapRerenderInterval = .5f

    fun requestTransition(target: Int) {
        if (!transitionOngoing) {
            transitionRequested = true
            transitionReqSource = currentScreen.round()
            transitionReqTarget = target.toFloat()
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        if (transitionRequested && !transitionOngoing) {
            transitionRequested = false
            transitionOngoing = true
            transitionTimer = 0f
        }

        if (transitionOngoing) {
            transitionTimer += Gdx.graphics.rawDeltaTime

            currentScreen = UIUtils.moveQuick(transitionReqSource, transitionReqTarget, transitionTimer, transitionLength)

            if (transitionTimer > transitionLength) {
                transitionOngoing = false
                currentScreen = transitionReqTarget
            }
        }



        // update at render time
        updateTransitionalItems()
        if (debugvals) {
            batch.color = Color.WHITE
            AppLoader.fontSmallNumbers.draw(batch, "screen:$currentScreen", 500f, 20f)
        }



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
        categoryBar.render(batch, camera)


        if (currentScreen > 1f + epsilon) {
            renderScreenMinimap(batch, camera)

            if (debugvals) {
                batch.color = Color.CORAL
                AppLoader.fontSmallNumbers.draw(batch, "Map", 300f, 10f)
            }
        }

        if (currentScreen in epsilon..2f - epsilon) {
            renderScreenInventory(batch, camera)

            if (debugvals) {
                batch.color = Color.CHARTREUSE
                AppLoader.fontSmallNumbers.draw(batch, "Inv", 350f, 10f)
            }
        }

        if (currentScreen < 1f - epsilon) {
            renderScreenGamemenu(batch, camera)

            if (debugvals) {
                batch.color = Color.SKY
                AppLoader.fontSmallNumbers.draw(batch, "Men", 400f, 10f)
            }
        }

        if (debugvals) {
            batch.color = Color.WHITE
            AppLoader.fontSmallNumbers.draw(batch, "inventoryScrOffX:$inventoryScrOffX", 500f, 10f)
        }


    }

    private val epsilon = 0.001f

    private val minimapScrOffX: Float
        get() = (currentScreen - 2f) * AppLoader.screenW
    /**
     * - 0 on inventory screen
     * - +WIDTH on minimap screen
     * - -WIDTH on gamemenu screen
     */
    private val inventoryScrOffX: Float
        get() = (currentScreen - 1f) * AppLoader.screenW
    private val menuScrOffX: Float
        get() = (currentScreen) * AppLoader.screenW

    private val MINIMAP_WIDTH = 800f
    private val MINIMAP_HEIGHT = itemList.height.toFloat()
    private val MINIMAP_SKYCOL = Color(0x88bbddff.toInt())
    private var minimapZoom = 1f
    private var minimapPanX = -MinimapComposer.totalWidth / 2f
    private var minimapPanY = -MinimapComposer.totalHeight / 2f
    private val MINIMAP_ZOOM_MIN = 0.5f
    private val MINIMAP_ZOOM_MAX = 8f
    private val minimapFBO = FrameBuffer(Pixmap.Format.RGBA8888, MINIMAP_WIDTH.toInt(), MINIMAP_HEIGHT.toInt(), false)
    private val minimapCamera = OrthographicCamera(MINIMAP_WIDTH, MINIMAP_HEIGHT)

    private fun renderScreenMinimap(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        // update map panning
        if (currentScreen >= 2f - epsilon) {
            // if left click is down and cursor is in the map area
            if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary")) &&
                Terrarum.mouseScreenY in itemList.posY..itemList.posY + itemList.height) {
                minimapPanX += Terrarum.mouseDeltaX / minimapZoom
                minimapPanY += Terrarum.mouseDeltaY / minimapZoom
            }


            if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
                minimapZoom *= (1f / 1.02f)
            }
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
                minimapZoom *= 1.02f
            }
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) {
                minimapZoom = 1f
                minimapPanX = -MinimapComposer.totalWidth / 2f
                minimapPanY = -MinimapComposer.totalHeight / 2f
            }


            try {
                //minimapPanX = minimapPanX.coerceIn(-(MinimapComposer.totalWidth * minimapZoom) + MINIMAP_WIDTH, 0f) // un-comment this line to constain the panning over x-axis
            } catch (e: IllegalArgumentException) { }
            try {
                //minimapPanY = minimapPanY.coerceIn(-(MinimapComposer.totalHeight * minimapZoom) + MINIMAP_HEIGHT, 0f)
            } catch (e: IllegalArgumentException) { }
            minimapZoom = minimapZoom.coerceIn(MINIMAP_ZOOM_MIN, MINIMAP_ZOOM_MAX)


            // make image to roll over for x-axis. This is for the ROUNDWORLD implementation, feel free to remove below.

        }


        // render minimap
        batch.end()

        if (minimapRerenderTimer >= minimapRerenderInterval) {
            minimapRerenderTimer = 0f
            MinimapComposer.requestRender()
        }

        MinimapComposer.renderToBackground()

        minimapFBO.inAction(minimapCamera, batch) {
            // whatever.
            MinimapComposer.tempTex.dispose()
            MinimapComposer.tempTex = Texture(MinimapComposer.minimap)
            MinimapComposer.tempTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Nearest)

            batch.inUse {

                // [  1  0 0 ]   [  s   0  0 ]   [  s  0 0 ]
                // [  0  1 0 ] x [  0   s  0 ] = [  0  s 0 ]
                // [ px py 1 ]   [ w/2 h/2 1 ]   [ tx ty 1 ]
                //
                // https://www.wolframalpha.com/input/?i=%7B%7B1,0,0%7D,%7B0,1,0%7D,%7Bp_x,p_y,1%7D%7D+*+%7B%7Bs,0,0%7D,%7B0,s,0%7D,%7Bw%2F2,h%2F2,1%7D%7D

                val tx = minimapPanX * minimapZoom + 0.5f * MINIMAP_WIDTH
                val ty = minimapPanY * minimapZoom + 0.5f * MINIMAP_HEIGHT

                // sky background
                batch.color = MINIMAP_SKYCOL
                batch.fillRect(0f, 0f, MINIMAP_WIDTH, MINIMAP_HEIGHT)
                // the actual image
                batch.color = Color.WHITE
                batch.draw(MinimapComposer.tempTex, tx, ty + MinimapComposer.totalHeight * minimapZoom, MinimapComposer.totalWidth * minimapZoom, -MinimapComposer.totalHeight * minimapZoom)

            }
        }
        batch.begin()


        AppLoader.fontSmallNumbers.draw(batch, "$minimapPanX, $minimapPanY; x$minimapZoom", minimapScrOffX + (AppLoader.screenW - MINIMAP_WIDTH) / 2, -10f + itemList.posY)


        batch.projectionMatrix = camera.combined
        // 1px stroke
        batch.color = Color.WHITE
        batch.fillRect(-1 + minimapScrOffX + (AppLoader.screenW - MINIMAP_WIDTH) / 2, -1 + itemList.posY.toFloat(), 2 + MINIMAP_WIDTH, 2 + MINIMAP_HEIGHT)

        // control hints
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, minimapControlHelp, offsetX + minimapScrOffX, yEnd - 20)

        // the minimap
        batch.draw(minimapFBO.colorBufferTexture, minimapScrOffX + (AppLoader.screenW - MINIMAP_WIDTH) / 2, itemList.posY.toFloat())
    }

    private fun renderScreenGamemenu(batch: SpriteBatch, camera: Camera) {
        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, gameMenuControlHelp, offsetX + menuScrOffX, yEnd - 20)

        // text buttons
        gameMenuButtons.render(batch, camera)
    }

    private fun renderScreenInventory(batch: SpriteBatch, camera: Camera) {
        itemList.render(batch, camera)
        equipped.render(batch, camera)


        // control hints
        val controlHintXPos = offsetX + inventoryScrOffX
        blendNormal(batch)
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, listControlHelp, controlHintXPos, yEnd - 20)


        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = xEnd - weightBarWidth + inventoryScrOffX
        val encumbBarTextXPos = encumbBarXPos - 6 - AppLoader.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = yEnd-20 + 3f +
                            if (AppLoader.fontGame.getWidth(listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                AppLoader.fontGame.lineHeight
                            else 0f
        
        AppLoader.fontGame.draw(batch,
                encumbranceText,
                encumbBarTextXPos,
                encumbBarYPos - 3f
        )

        // encumbrance bar background
        blendNormal(batch)
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = encumbBack
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                weightBarWidth, controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        batch.fillRect(
                encumbBarXPos, encumbBarYPos,
                if (actor.inventory.capacityMode == CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                controlHelpHeight - 6f
        )
        // debug text
        batch.color = Color.LIGHT_GRAY
        if (IS_DEVELOPMENT_BUILD) {
            AppLoader.fontSmallNumbers.draw(batch,
                    "${actor.inventory.capacity}/${actor.inventory.maxCapacity}",
                    encumbBarTextXPos,
                    encumbBarYPos + controlHelpHeight - 4f
            )
        }
    }


    fun rebuildList() {
        printdbg(this, "rebuilding list")

        itemList.rebuild(catIconsMeaning[categoryBar.selectedIcon])
        equipped.rebuild()

        encumbrancePerc = actor.inventory.capacity.toFloat() / actor.inventory.maxCapacity
        isEncumbered = actor.inventory.isEncumbered
    }

    private fun Int.fastLen(): Int {
        return if (this < 0) 1 + this.unaryMinus().fastLen()
        else if (this < 10) 1
        else if (this < 100) 2
        else if (this < 1000) 3
        else if (this < 10000) 4
        else if (this < 100000) 5
        else if (this < 1000000) 6
        else if (this < 10000000) 7
        else if (this < 100000000) 8
        else if (this < 1000000000) 9
        else 10
    }

    override fun dispose() {
        categoryBar.dispose()
        itemList.dispose()
        equipped.dispose()
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

        MinimapComposer.revalidateAll()
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((AppLoader.screenW - internalWidth)   / 2).toFloat()
        offsetY = ((AppLoader.screenH - internalHeight) / 2).toFloat()

        xEnd = (AppLoader.screenW + internalWidth).div(2).toFloat()
        yEnd = (AppLoader.screenH + internalHeight).div(2).toFloat()
    }



    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }
    
}

