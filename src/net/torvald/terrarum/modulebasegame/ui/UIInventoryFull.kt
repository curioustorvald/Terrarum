package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory.Companion.CAPACITY_MODE_NO_ENCUMBER
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButtonList
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
    
    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    private val itemListToEquipViewGap = 24

    val internalWidth: Int = UIItemInventoryDynamicList.WIDTH + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
    val internalHeight: Int = 166 + UIItemInventoryDynamicList.HEIGHT // grad_begin..grad_end..contents..grad_begin..grad_end



    internal val catIcons: TextureRegionPack = TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
    internal val catArrangement: IntArray = intArrayOf(9,6,7,1,0,2,3,4,5,8)



    private val SP = "${0x3000.toChar()}${0x3000.toChar()}"
    val listControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}..${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${Terrarum.gamepadLabelNinY} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${Terrarum.gamepadLabelNinA} ${Lang["GAME_INVENTORY_DROP"]}"
    val minimapControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP${0xe06b.toChar()} ${Lang["GAME_INVENTORY"]}"
    val gameMenuControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP${0xe068.toChar()} ${Lang["GAME_INVENTORY"]}"
    val controlHelpHeight = Terrarum.fontGame.lineHeight

    private var encumbrancePerc = 0f
    private var isEncumbered = false


    val catBarWidth = 330
    val categoryBar = UIItemInventoryCatBar(
            this,
            (Terrarum.WIDTH - catBarWidth) / 2,
            42 + (Terrarum.HEIGHT - internalHeight) / 2,
            catBarWidth
    )
    val catSelection: Int
        get() = categoryBar.selectedIndex
    val catSelectedIcon: Int
        get() = categoryBar.selectedIcon

    override var openCloseTime: Second = 0.0f


    private val itemList: UIItemInventoryDynamicList =
            UIItemInventoryDynamicList(
                    this,
                    actor.inventory,
                    0 + (Terrarum.WIDTH - internalWidth) / 2,
                    109 + (Terrarum.HEIGHT - internalHeight) / 2
            )


    private val equipped: UIItemInventoryEquippedView =
            UIItemInventoryEquippedView(
                    this,
                    actor.inventory,
                    actor as ActorWBMovable,
                    internalWidth - UIItemInventoryEquippedView.WIDTH + (Terrarum.WIDTH - internalWidth) / 2,
                    109 + (Terrarum.HEIGHT - internalHeight) / 2
            )
    private val gameMenuListWidth = 400
    private val gameMenuListHeight = 40 * 5
    private val gameMenuCharInfoHeight = 64 + 40 // no top margin, 40 bottom margin
    private val gameMenuListTotalHeight = gameMenuListHeight + gameMenuCharInfoHeight
    private val gameMenuButtons = UIItemTextButtonList(
            this, arrayOf("MENU_LABEL_MAINMENU", "MENU_LABEL_DESKTOP", "MENU_OPTIONS_CONTROLS", "MENU_OPTIONS_SOUND", "MENU_LABEL_GRAPHICS"),
            Terrarum.WIDTH + (Terrarum.WIDTH - gameMenuListWidth) / 2,
            (itemList.height - gameMenuListTotalHeight) / 2 + itemList.posY + gameMenuCharInfoHeight,
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
            itemList.rebuild() // have to manually rebuild, too!
        }



        rebuildList()


        addToTransitionalGroup(itemList)
        addToTransitionalGroup(equipped)
        addToTransitionalGroup(gameMenuButtons)

        // make gameMenuButtons work
        gameMenuButtons.selectionChangeListener = { old, new ->
            if (new == 0) {
                Terrarum.setScreen(TitleScreen(Terrarum.batch))
            }
            else if (new == 1) {
                Gdx.app.exit()
            }
        }
    }

    private var offsetX = ((Terrarum.WIDTH - internalWidth)   / 2).toFloat()
    private var offsetY = ((Terrarum.HEIGHT - internalHeight) / 2).toFloat()



    override fun updateUI(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }


        categoryBar.update(delta)

        transitionalUpdateUIs.forEach { it.update(delta) }

    }

    private val gradStartCol = Color(0x404040_60)
    private val gradEndCol   = Color(0x000000_70)
    private val shapeRenderer = ShapeRenderer()
    private val gradHeight = 48f

    private val weightBarWidth = 60f

    private var xEnd = (Terrarum.WIDTH + internalWidth).div(2).toFloat()
    private var yEnd = (Terrarum.HEIGHT + internalHeight).div(2).toFloat()

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
            transitionTimer += Gdx.graphics.deltaTime

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
        Gdx.gl.glEnable(GL20.GL_BLEND) // ending the batch disables blend

        val gradTopStart = (Terrarum.HEIGHT - internalHeight).div(2).toFloat()
        val gradBottomEnd = Terrarum.HEIGHT - gradTopStart

        shapeRenderer.inUse {
            shapeRenderer.rect(0f, gradTopStart, Terrarum.WIDTH.toFloat(), gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)
            shapeRenderer.rect(0f, gradBottomEnd, Terrarum.WIDTH.toFloat(), -gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, gradTopStart + gradHeight, Terrarum.WIDTH.toFloat(), internalHeight - (2 * gradHeight), gradEndCol, gradEndCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, 0f, Terrarum.WIDTH.toFloat(), gradTopStart, gradStartCol, gradStartCol, gradStartCol, gradStartCol)
            shapeRenderer.rect(0f, Terrarum.HEIGHT.toFloat(), Terrarum.WIDTH.toFloat(), -(Terrarum.HEIGHT.toFloat() - gradBottomEnd), gradStartCol, gradStartCol, gradStartCol, gradStartCol)
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
        get() = (currentScreen - 2f) * Terrarum.WIDTH
    /**
     * - 0 on inventory screen
     * - +WIDTH on minimap screen
     * - -WIDTH on gamemenu screen
     */
    private val inventoryScrOffX: Float
        get() = (currentScreen - 1f) * Terrarum.WIDTH
    private val menuScrOffX: Float
        get() = (currentScreen) * Terrarum.WIDTH

    private fun renderScreenMinimap(batch: SpriteBatch, camera: Camera) {
        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        Terrarum.fontGame.draw(batch, minimapControlHelp, offsetX + minimapScrOffX, yEnd - 20)
    }

    private fun renderScreenGamemenu(batch: SpriteBatch, camera: Camera) {
        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        Terrarum.fontGame.draw(batch, gameMenuControlHelp, offsetX + menuScrOffX, yEnd - 20)

        // text buttons
        gameMenuButtons.render(batch, camera)

        // character info window

        // !! DUMMY !!
        batch.color = itemList.backColour
        batch.fillRect(
                ((Terrarum.WIDTH - 400) / 2) + menuScrOffX,
                (itemList.height - gameMenuListTotalHeight) / 2 + itemList.posY.toFloat(),
                gameMenuListWidth.toFloat(),
                64f
        )
    }

    private fun renderScreenInventory(batch: SpriteBatch, camera: Camera) {
        itemList.render(batch, camera)
        equipped.render(batch, camera)


        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        Terrarum.fontGame.draw(batch, listControlHelp, offsetX + inventoryScrOffX, yEnd - 20)


        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]

        Terrarum.fontGame.draw(batch,
                encumbranceText,
                xEnd - 9 - Terrarum.fontGame.getWidth(encumbranceText) - weightBarWidth + inventoryScrOffX,
                yEnd-20
        )

        // encumbrance bar background
        blendMul(batch)
        batch.color = Color(0xa0a0a0_ff.toInt())
        batch.fillRect(
                xEnd - 3 - weightBarWidth + inventoryScrOffX,
                yEnd-20 + 3f,
                weightBarWidth,
                controlHelpHeight - 6f
        )
        // encumbrance bar
        blendNormal(batch)
        batch.color = if (isEncumbered) Color(0xff0000_cc.toInt()) else Color(0x00ff00_cc.toInt())
        batch.fillRect(
                xEnd - 3 - weightBarWidth + inventoryScrOffX,
                yEnd-20 + 3f,
                if (actor.inventory.capacityMode == CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                controlHelpHeight - 5f
        )
    }


    fun rebuildList() {
        itemList.rebuild()
        equipped.rebuild()

        encumbrancePerc = actor.inventory.capacity.toFloat() / actor.inventory.maxCapacity
        isEncumbered = actor.inventory.isEncumbered
    }

    override fun dispose() {
        categoryBar.dispose()
        itemList.dispose()
        equipped.dispose()
    }



    override fun doOpening(delta: Float) {
        (Terrarum.ingame as? Ingame)?.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        (Terrarum.ingame as? Ingame)?.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        (Terrarum.ingame as? Ingame)?.setTooltipMessage(null) // required!
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((Terrarum.WIDTH - internalWidth)   / 2).toFloat()
        offsetY = ((Terrarum.HEIGHT - internalHeight) / 2).toFloat()

        xEnd = (Terrarum.WIDTH + internalWidth).div(2).toFloat()
        yEnd = (Terrarum.HEIGHT + internalHeight).div(2).toFloat()
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