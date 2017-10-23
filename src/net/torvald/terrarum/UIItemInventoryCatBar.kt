package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-10-20.
 */
class UIItemInventoryCatBar(
        parentUI: UICanvas,
        override var posX: Int,
        override var posY: Int,
        override val width: Int
) : UIItem(parentUI) {

    private val catIcons = (parentUI as UIInventoryFull).catIcons
    private val catArrangement = (parentUI as UIInventoryFull).catArrangement


    private val inventoryUI = parentUI
    override val height = catIcons.tileH + 5
    

    private val buttons: Array<UIItemImageButton>
    private val buttonGapSize = (width.toFloat() - (catArrangement.size * catIcons.tileW)) / (catArrangement.size)
    var selectedIndex = 0 // default to ALL
        private set
    val selectedIcon: Int
        get() = catArrangement[selectedIndex]
    private val catSelectionOld = 0 // default to ALL

    // set up buttons
    init {
        // place sub UIs: Image Buttons
        buttons = Array(catArrangement.size, { index ->
            val iconPosX = ((buttonGapSize / 2) + index * (catIcons.tileW + buttonGapSize)).roundInt()
            val iconPosY = 0

            UIItemImageButton(
                    inventoryUI,
                    catIcons.get(catArrangement[index], 0),
                    activeBackCol = Color(0),
                    activeBackBlendMode = BlendMode.NORMAL,
                    posX = posX + iconPosX,
                    posY = posY + iconPosY,
                    highlightable = true
            )
        })
    }


    private val underlineIndTex: Texture
    private val underlineColour = Color(0xeaeaea_40.toInt())
    private val underlineHighlightColour = buttons[0].highlightCol

    private var highlighterXPos = buttons[selectedIndex].posX.toDouble()
    private var highlighterXStart = highlighterXPos
    private var highlighterXEnd = highlighterXPos

    private val highlighterYPos = catIcons.tileH + 4f
    private var highlighterMoving = false
    private val highlighterMoveDuration: Second = 0.1f
    private var highlighterMoveTimer: Second = 0f

    // set up underlined indicator
    init {
        // procedurally generate texture
        val pixmap = Pixmap(catIcons.tileW + buttonGapSize.floorInt(), 1, Pixmap.Format.RGBA8888)
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


        buttons[selectedIndex].highlighted = true
    }


    /** (oldIndex: Int?, newIndex: Int) -> Unit */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    override fun update(delta: Float) {
        super.update(delta)


        if (highlighterMoving) {
            highlighterMoveTimer += delta

            highlighterXPos = UIUtils.moveQuick(
                    highlighterXStart,
                    highlighterXEnd,
                    highlighterMoveTimer.toDouble(),
                    highlighterMoveDuration.toDouble()
            )

            if (highlighterMoveTimer > highlighterMoveDuration) {
                highlighterMoveTimer = 0f
                highlighterXStart = highlighterXEnd
                highlighterXPos = highlighterXEnd
                highlighterMoving = false
            }
        }


        buttons.forEachIndexed { index, btn ->
            btn.update(delta)


            if (btn.mousePushed && index != selectedIndex) {
                val oldIndex = selectedIndex

                highlighterXStart = buttons[selectedIndex].posX.toDouble() // using old selectedIndex
                selectedIndex = index
                highlighterMoving = true
                highlighterXEnd = buttons[selectedIndex].posX.toDouble() // using new selectedIndex

                selectionChangeListener?.invoke(oldIndex, index)
            }
            btn.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

        }
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        // button
        // colour determined by UI items themselves
        buttons.forEach { it.render(batch, camera) }


        blendNormal(batch)


        // underline
        batch.color = underlineColour
        batch.drawStraightLine(posX.toFloat(), posY + height - 1f, posX + width.toFloat(), 1f, false)

        // indicator
        batch.color = underlineHighlightColour
        batch.draw(underlineIndTex, (highlighterXPos - buttonGapSize / 2).toFloat().round(), posY + highlighterYPos)
    }




    override fun dispose() {
        underlineIndTex.dispose()
        catIcons.dispose()
    }
}