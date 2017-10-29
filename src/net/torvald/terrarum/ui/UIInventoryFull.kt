package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithPhysics
import net.torvald.terrarum.gameactors.InventoryPair
import net.torvald.terrarum.gameactors.Pocketed
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.ArrayList

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

    override var openCloseTime: Second = 1f


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
                        actor as ActorWithPhysics,
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

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
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
        Terrarum.fontGame.draw(batch, listControlHelp, offsetX, offsetY + internalHeight + controlHelpHeight)
    }



    fun rebuildList() {
        itemList?.rebuild()
        equipped?.rebuild()
    }

    override fun dispose() {
        catBar.dispose()
        itemList?.dispose()
        equipped?.dispose()
    }



    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
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