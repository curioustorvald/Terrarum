package net.torvald.terrarum

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-10-20.
 */
class UIItemInventoryCatBar(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        uiInternalWidth: Int,
        override val width: Int,
        val showSideButtons: Boolean = false,
        val panelTransitionReqFun: (Int) -> Unit = {} // for side buttons; for the selection change, override selectionChangeListener
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        const val CAT_ALL = "__all__"
    }

    internal val catIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_category")
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
    internal val catIconsLabels = listOf(
        { Lang["GAME_INVENTORY_WEAPONS"] },
        { Lang["CONTEXT_ITEM_TOOL_PLURAL"] },
        { Lang["CONTEXT_ITEM_ARMOR"] },
        { Lang["GAME_INVENTORY_INGREDIENTS"] },
        { Lang["GAME_INVENTORY_POTIONS"] },
        { Lang["CONTEXT_ITEM_MAGIC"] },
        { Lang["GAME_INVENTORY_BLOCKS"] },
        { Lang["GAME_INVENTORY_WALLS"] },
        { Lang["GAME_GENRE_MISC"] },
        { Lang["MENU_LABEL_ALL"] },
    )


    private val inventoryUI = parentUI
    override val height = catIcons.tileH + 5
    

    private val mainButtons: Array<UIItemImageButton>
    private val buttonGapSize = (width.toFloat() - (catArrangement.size * catIcons.tileW)) / (catArrangement.size)
    /** raw order */
    private var selectedIndex = 0 // default to ALL
        private set
    /** re-arranged order */
    val selectedIcon: Int
        get() = catArrangement[selectedIndex]

    private val sideButtons: Array<UIItemImageButton>

    // set up all the buttons
    init {
        // place sub UIs: Image Buttons
        mainButtons = Array(catArrangement.size) { index ->
            val iconPosX = ((buttonGapSize / 2) + index * (catIcons.tileW + buttonGapSize)).roundToInt()
            val iconPosY = 0

            UIItemImageButton(
                    inventoryUI,
                    catIcons.get(catArrangement[index], 0),
                    activeBackCol = Color(0),
                    backgroundCol = Color(0),
                    highlightBackCol = Color(0),
                    activeBackBlendMode = BlendMode.NORMAL,
                    initialX = posX + iconPosX,
                    initialY = posY + iconPosY,
                    highlightable = true
            )
        }


        // side buttons
        // NOTE: < > arrows must "highlightable = false"; "true" otherwise
        //  determine gaps: hacky way exploiting that we already know the catbar is always at the c of the ui
        val relativeStartX = posX - (uiInternalWidth - width) / 2
        val sideButtonsGap = (((uiInternalWidth - width) / 2) - 2f * catIcons.tileW) / 3f
        val iconIndex = arrayOf(
                catIcons.get(9,1),
                catIcons.get(16,0),
                catIcons.get(17,0),
                catIcons.get(13,0)
        )


        //println("[UIItemInventoryCatBar] relativeStartX: $relativeStartX")
        //println("[UIItemInventoryCatBar] posX: $posX")

        sideButtons = Array(iconIndex.size) { index ->
            val iconPosX = if (index < 2)
                (relativeStartX + sideButtonsGap + (sideButtonsGap + catIcons.tileW) * index).roundToInt()
            else
                (relativeStartX + width + 2 * sideButtonsGap + (sideButtonsGap + catIcons.tileW) * index).roundToInt()
            val iconPosY = 0

            UIItemImageButton(
                    inventoryUI,
                    iconIndex[index],
                    activeBackCol = Color(0),
                    backgroundCol = Color(0),
                    highlightBackCol = Color(0),
                    activeBackBlendMode = BlendMode.NORMAL,
                    initialX = iconPosX,
                    initialY = posY + iconPosY,
                    inactiveCol = if (index == 0 || index == 3) Color.WHITE else Color(0xffffff7f.toInt()),
                    activeCol = if (index == 0 || index == 3) Toolkit.Theme.COL_MOUSE_UP else Color(0xffffff7f.toInt()),
                    highlightable = (index == 0 || index == 3)
            )
        }
    }


    private val underlineIndTex: Texture
    private val underlineColour = Color(0xeaeaea_40.toInt())
    private val underlineHighlightColour = mainButtons[0].highlightCol

    private var highlighterXPos = mainButtons[selectedIndex].posX
    private var highlighterXStart = highlighterXPos
    private var highlighterXEnd = highlighterXPos

    private val highlighterYPos = catIcons.tileH + 4
    private var highlighterMoving = false
    private val highlighterMoveDuration: Second = 0.15f
    private var highlighterMoveTimer: Second = 0f

    private var transitionFired = false

    /**
     * 0: map, 1: inventory caticons, 2: menu
     */
    var selectedPanel = 1; private set


    fun setSelectedPanel(n: Int) {
        if (n !in 0..2) throw IllegalArgumentException("$n")
        selectedPanel = n

        sideButtons[0].highlighted = (n == 0)
        mainButtons[selectedIndex].highlighted = (n == 1)
        sideButtons[3].highlighted = (n == 2)
    }


    // set up underlined indicator
    init {
        // procedurally generate texture
        val pixmap = Pixmap(catIcons.tileW + buttonGapSize.floorToInt(), 1, Pixmap.Format.RGBA8888)
        for (x in 0 until pixmap.width.plus(1).ushr(1)) { // eqv. of ceiling the half-int
            val col = /*if      (x == 0)*/ /*0xffffff_80.toInt()*/
                      /*else if (x == 1)*/ /*0xffffff_c0.toInt()*/
                      /*else            */ 0xffffff_ff.toInt()

            pixmap.drawPixel(x, 0, col)
            pixmap.drawPixel(pixmap.width - (x + 1), 0, col)
        }
        underlineIndTex = Texture(pixmap)
        underlineIndTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixmap.dispose()


        mainButtons[selectedIndex].highlighted = true
    }


    /** (oldIndex: Int?, newIndex: Int) -> Unit
     * Indices are raw index. That is, not re-arranged. */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    override fun update(delta: Float) {
        super.update(delta)


        if (highlighterMoving) {
            highlighterMoveTimer += delta

            highlighterXPos = Movement.moveQuick(
                    highlighterXStart.toFloat(),
                    highlighterXEnd.toFloat(),
                    highlighterMoveTimer,
                    highlighterMoveDuration
            ).roundToInt()

            if (highlighterMoveTimer > highlighterMoveDuration) {
                highlighterMoveTimer = 0f
                highlighterXStart = highlighterXEnd
                highlighterXPos = highlighterXEnd
                highlighterMoving = false
            }
        }


        mainButtons.forEachIndexed { index, btn ->
            btn.update(delta)

            if (btn.mousePushed && selectedPanel != 1) {
                transitionFired = true
                selectedPanel = 1
            }

            // move selection highlighter
            if (btn.mousePushed && index != selectedIndex) {
                // normal stuffs
                val oldIndex = selectedIndex

                highlighterXStart = mainButtons[selectedIndex].posX // using old selectedIndex
                selectedIndex = index
                highlighterMoving = true
                highlighterXEnd = mainButtons[selectedIndex].posX // using new selectedIndex

                selectionChangeListener?.invoke(oldIndex, index)
            }

            if (selectedPanel == 1) {
                btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

                sideButtons[0].highlighted = false
                sideButtons[3].highlighted = false
            }
        }

        if (showSideButtons) {
            sideButtons[0].update(delta)
            sideButtons[3].update(delta)

            // more transition stuffs
            if (sideButtons[0].mousePushed) {
                if (selectedPanel != 0) transitionFired = true
                mainButtons.forEach { it.highlighted = false }
                selectedPanel = 0

                sideButtons[0].highlighted = true
                sideButtons[3].highlighted = false
            }
            else if (sideButtons[3].mousePushed) {
                if (selectedPanel != 2) transitionFired = true
                mainButtons.forEach { it.highlighted = false }
                selectedPanel = 2
                transitionFired = true

                sideButtons[0].highlighted = false
                sideButtons[3].highlighted = true
            }


            if (transitionFired) {
                transitionFired = false
                panelTransitionReqFun(selectedPanel)
            }
        }
    }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        super.render(batch, camera)

        // button
        // colour determined by UI items themselves
        mainButtons.forEach { it.render(batch, camera) }
        if (showSideButtons) sideButtons.forEach { it.render(batch, camera) }


        blendNormalStraightAlpha(batch)


        // underline
        batch.color = underlineColour
        Toolkit.drawStraightLine(batch, posX, posY + height - 1, posX + width, 1, false)

        if (selectedPanel == 1) {
            // indicator
            batch.color = underlineHighlightColour
            batch.draw(underlineIndTex, (highlighterXPos - buttonGapSize / 2), posY + highlighterYPos.toFloat())

            // label
            batch.color = Color.WHITE
            catIconsLabels[selectedIcon]().let {
                App.fontGame.draw(batch, it, posX + ((width - App.fontGame.getWidth(it)) / 2), posY + highlighterYPos + 4)
            }
        }

    }




    override fun dispose() {
        underlineIndTex.dispose()
        //catIcons.dispose() // disposed of by the AppLoader
        //mainButtons.forEach { it.dispose() } // disposed of by the AppLoader
        //sideButtons.forEach { it.dispose() } // disposed of by the AppLoader
    }
}