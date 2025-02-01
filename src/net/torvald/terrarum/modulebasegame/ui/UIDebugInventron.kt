package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.unicode.getKeycapPC
import java.lang.reflect.Field
import java.util.*
import kotlin.math.roundToInt


/**
 * Created by minjaesong on 2025-02-01.
 */
class UIDebugInventron : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start",
) {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val catBar: UIItemCatBar
    private val itemListPlayer: UITemplateHalfInventory

    private val selectedItemSlot: UIItemInventoryElemWide

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap * 2) / 2

    private var selectedItem: GameItem? = null


    private val analyserPosX = UITemplateHalfInventory.XOFFSET_LEFT
    private val analyserPosY = UITemplateHalfInventory.YOFFSET
    private val analyserPosY2 = UITemplateHalfInventory.YOFFSET + UIItemInventoryElemWide.height + UIItemInventoryItemGrid.listGap

    private val analyserWidth: Int
    private val analyserHeight: Int

    private val analyserScroll: UIItemVertSlider

    init {
        catBar = UIItemCatBar(
            this,
            (width - UIInventoryFull.catBarWidth) / 2,
            42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
            UIInventoryFull.internalWidth,
            UIInventoryFull.catBarWidth,
            false,

            catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category"),
            catArrangement = intArrayOf(9,6,7,1,0,2,1_011,3,4,5,8), // icon order
            catIconsMeaning = listOf(
                // sortedBy: catArrangement
                arrayOf(UIItemCatBar.CAT_ALL),
                arrayOf(GameItem.Category.BLOCK),
                arrayOf(GameItem.Category.WALL),
                arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
                arrayOf(GameItem.Category.WEAPON),
                arrayOf(GameItem.Category.ARMOUR),
                arrayOf(GameItem.Category.FIXTURE),
                arrayOf(GameItem.Category.GENERIC),
                arrayOf(GameItem.Category.POTION),
                arrayOf(GameItem.Category.MAGIC),
                arrayOf(GameItem.Category.MISC),
            ),
            catIconsLabels = listOf(
                { Lang["MENU_LABEL_ALL"] },
                { Lang["GAME_INVENTORY_BLOCKS"] },
                { Lang["GAME_INVENTORY_WALLS"] },
                { Lang["CONTEXT_ITEM_TOOL_PLURAL"] },
                { Lang["GAME_INVENTORY_WEAPONS"] },
                { Lang["CONTEXT_ITEM_ARMOR"] },
                { Lang["CONTEXT_ITEM_FIXTURES"] },
                { Lang["GAME_INVENTORY_INGREDIENTS"] },
                { Lang["GAME_INVENTORY_POTIONS"] },
                { Lang["CONTEXT_ITEM_MAGIC"] },
                { Lang["GAME_GENRE_MISC"] },
            ),

            )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }


        itemListPlayer = UITemplateHalfInventory(this, false).also {
            it.itemListKeyDownFun = { _, _, _, _, _ -> Unit }
            it.itemListTouchDownFun = { gameItem, amount, button, _, _ ->
                selectedItem = gameItem
                refreshAnalysis()
            }
        }
        itemListPlayer.itemList.navRemoCon.listButtonListener = { _,_ -> setCompact(false) }
        itemListPlayer.itemList.navRemoCon.gridButtonListener = { _,_ -> setCompact(true) }

        selectedItemSlot = UIItemInventoryElemWide(this,
            analyserPosX, analyserPosY, itemListPlayer.itemList.width,
            keyDownFun = { _, _, _, _, _ -> },
            touchDownFun = { _, _, _, _, _ -> selectedItem = null; refreshAnalysis() },
            wheelFun = { _, _, _, _, _, _ -> }
        ).also {
            it.showItemCount = false
        }

        analyserWidth = itemListPlayer.itemList.width
        analyserHeight = itemListPlayer.itemList.height - (analyserPosY2 - analyserPosY)


        analyserScroll = UIItemVertSlider(this,
            analyserPosX - 18,
            analyserPosY2 + 1,
            0.0, 0.0, 1.0, analyserHeight - 2, analyserHeight - 2
        )

        handler.allowESCtoClose = true

        addUIitem(catBar)
        addUIitem(itemListPlayer)
        addUIitem(selectedItemSlot)
        addUIitem(analyserScroll)
    }

    override fun show() {
        super.show()

        itemListPlayer.itemList.getInventory = { INGAME.actorNowPlaying!!.inventory }
        selectedItem = null; refreshAnalysis()

        itemListUpdate()
    }

    private fun itemListUpdate() {
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])

    }

    private fun setCompact(yes: Boolean) {
        itemListPlayer.itemList.isCompactMode = yes
        itemListPlayer.itemList.navRemoCon.gridModeButtons[0].highlighted = !yes
        itemListPlayer.itemList.navRemoCon.gridModeButtons[1].highlighted = yes
        itemListPlayer.itemList.itemPage = 0
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIndex])

        itemListUpdate()
    }

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private val SP = "\u3000"
    private val controlHelpLeft: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}"


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // background fill
        UIInventoryFull.drawBackground(batch, 1f)

        // analyser view
        batch.color = Color(0x7F)
        Toolkit.fillArea(batch, analyserPosX, analyserPosY2, analyserWidth, analyserHeight)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, analyserPosX, analyserPosY2, analyserWidth, analyserHeight)

        batch.color = Color.WHITE
        drawAnalysis(batch)

        // UI items
        batch.color = Color.WHITE
        uiItems.forEach { it.render(frameDelta, batch, camera) }


        blendNormalStraightAlpha(batch)

        // control hint
        val controlHintXPos = analyserPosX + 2f

        batch.color = Color.WHITE
        App.fontGame.draw(batch, controlHelpLeft, controlHintXPos, UIInventoryFull.yEnd - 20)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
    }

    override fun dispose() {
    }

    private var analysisTextBuffer = ArrayList<String>()

    fun getAllFields(fields: MutableList<Field>, type: Class<*>): List<Field> {
        fields.addAll(Arrays.asList(*type.declaredFields))

        if (type.superclass != null) {
            getAllFields(fields, type.superclass)
        }

        return fields
    }

    private val TEXT_LINE_HEIGHT = 24

    private fun refreshAnalysis() {
        analysisTextBuffer = ArrayList<String>()

        // update wall of text
        if (selectedItem != null) {
            selectedItemSlot.item = selectedItem
            selectedItemSlot.itemImage = selectedItem!!.itemImage

            val fields: ArrayList<Field> = ArrayList<Field>()
            getAllFields(fields, selectedItem!!.javaClass)

            println("FIELDS:")
            println(fields)

            fields.forEach {
                try {
                    it.isAccessible = true
                    println("${it.name}(${it.type.simpleName}) = ${it.get(selectedItem)}")
                    analysisTextBuffer.add("$ccY${it.name}$ccW($ccO${it.type.simpleName}$ccW) = $ccG${it.get(selectedItem!!)}")
                }
                catch (e: Throwable) {}
            }
        }
        else {
            selectedItemSlot.item = null
        }

        // update scrollbar
        analyserScroll.isEnabled = (selectedItem == null)
        analyserScroll.handleHeight = if (analysisTextBuffer.isEmpty() || selectedItem == null)
            analyserHeight
        else
            (analyserHeight.toFloat() / analysisTextBuffer.size.times(TEXT_LINE_HEIGHT)).times(analyserHeight).roundToInt().coerceIn(12, analyserHeight)
    }

    private fun drawAnalysis(batch: SpriteBatch) {
        val scroll = (analyserScroll.value * analysisTextBuffer.size.times(TEXT_LINE_HEIGHT).minus(analyserHeight - 3)).ifNaN(0.0).roundToInt().coerceAtLeast(0)
        analysisTextBuffer.forEachIndexed { index, s ->
            App.fontGame.draw(batch, s, analyserPosX + 6, analyserPosY2 + 3 + index * TEXT_LINE_HEIGHT - scroll)
        }
    }


}