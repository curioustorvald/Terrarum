package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory.Companion.CAPACITY_MODE_NO_ENCUMBER
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.Second
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        var actor: Pocketed?,

        toggleKeyLiteral: Int? = null, toggleButtonLiteral: Int? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    val internalWidth: Int = 630
    val internalHeight: Int = 558 // grad_begin..grad_end..contents..grad_begin..grad_end



    internal val catIcons: TextureRegionPack = TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
    internal val catArrangement: IntArray = intArrayOf(9,6,7,1,0,2,3,4,5,8)



    private val SP = "${0x3000.toChar()}${0x3000.toChar()}"
    val listControlHelp: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}..${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${Terrarum.joypadLabelNinY} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${Terrarum.joypadLabelNinA} ${Lang["GAME_INVENTORY_DROP"]}"
    val controlHelpHeight = Terrarum.fontGame.lineHeight

    private var encumbrancePerc = 0f
    private var isEncumbered = false


    val catBarWidth = 330
    val catBar = UIItemInventoryCatBar(
            this,
            (Terrarum.WIDTH - catBarWidth) / 2,
            66 + (Terrarum.HEIGHT - internalHeight) / 2,
            catBarWidth
    )
    val catSelection: Int
        get() = catBar.selectedIndex
    val catSelectedIcon: Int
        get() = catBar.selectedIcon

    override var openCloseTime: Second = 0.0f


    private val itemList: UIItemInventoryDynamicList? =
            if (actor != null) {
                UIItemInventoryDynamicList(
                        this,
                        actor!!.inventory,
                        0 + (Terrarum.WIDTH - internalWidth) / 2,
                        109 + (Terrarum.HEIGHT - internalHeight) / 2
                )
            }
    else null


    private val equipped: UIItemInventoryEquippedView? =
            if (actor != null) {
                UIItemInventoryEquippedView(
                        this,
                        actor!!.inventory,
                        actor as ActorWBMovable,
                        internalWidth - UIItemInventoryEquippedView.width + (Terrarum.WIDTH - internalWidth) / 2,
                        109 + (Terrarum.HEIGHT - internalHeight) / 2
                )
            }
    else null



    init {
        addItem(catBar)
        itemList?.let { addItem(it) }
        equipped?.let { addItem(it) }


        catBar.selectionChangeListener = { old, new  -> rebuildList() }



        rebuildList()

    }

    private var offsetX = ((Terrarum.WIDTH - internalWidth)   / 2).toFloat()
    private var offsetY = ((Terrarum.HEIGHT - internalHeight) / 2).toFloat()



    override fun updateUI(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }


        catBar.update(delta)
        itemList?.update(delta)
        equipped?.update(delta)
    }

    private val gradStartCol = Color(0x404040_60)
    private val gradEndCol   = Color(0x000000_70)
    private val shapeRenderer = ShapeRenderer()
    private val gradHeight = 48f

    private val weightBarWidth = 60f

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val xEnd = (Terrarum.WIDTH + internalWidth).div(2).toFloat()
        val yEnd = (Terrarum.HEIGHT + internalHeight).div(2).toFloat()


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
        catBar.render(batch, camera)
        itemList?.render(batch, camera)
        equipped?.render(batch, camera)


        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        Terrarum.fontGame.draw(batch, listControlHelp, offsetX, offsetY + internalHeight)


        // encumbrance meter
        if (actor != null) {
            val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]

            Terrarum.fontGame.draw(batch,
                    encumbranceText,
                    xEnd - 9 - Terrarum.fontGame.getWidth(encumbranceText) - weightBarWidth,
                    yEnd
            )

            // encumbrance bar background
            blendMul()
            batch.color = Color(0xa0a0a0_ff.toInt())
            batch.fillRect(
                    xEnd - 3 - weightBarWidth,
                    yEnd + 3f,
                    weightBarWidth,
                    controlHelpHeight - 6f
            )
            // encumbrance bar
            blendNormal()
            batch.color = if (isEncumbered) Color(0xff0000_cc.toInt()) else Color(0x00ff00_cc.toInt())
            batch.fillRect(
                    xEnd - 3 - weightBarWidth,
                    yEnd + 3f,
                    if (actor?.inventory?.capacityMode == CAPACITY_MODE_NO_ENCUMBER)
                        1f
                    else // make sure 1px is always be seen
                        minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                    controlHelpHeight - 5f
            )
        }
    }



    fun rebuildList() {
        itemList?.rebuild()
        equipped?.rebuild()

        actor?.let {
            encumbrancePerc = actor!!.inventory.capacity.toFloat() / actor!!.inventory.maxCapacity
            isEncumbered = actor!!.inventory.isEncumbered
        }
    }

    override fun dispose() {
        catBar.dispose()
        itemList?.dispose()
        equipped?.dispose()
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
        (Terrarum.ingame as? Ingame)?.setTooltipMessage(null) // required!!
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((Terrarum.WIDTH - internalWidth)   / 2).toFloat()
        offsetY = ((Terrarum.HEIGHT - internalHeight) / 2).toFloat()
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