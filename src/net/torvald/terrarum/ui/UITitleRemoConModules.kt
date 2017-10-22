package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2017-08-01.
 */
class UITitleRemoConModules(val superMenu: UICanvas) : UICanvas() {

    val menuLabels = arrayOf(
            "MENU_LABEL_RETURN"
    )


    override var width: Int = UITitleRemoConRoot.remoConWidth
    override var height: Int = UITitleRemoConRoot.getRemoConHeight(menuLabels)
    override var openCloseTime: Second = 0f


    private val menubar = UIItemTextButtonList(
            this,
            menuLabels,
            0, UITitleRemoConRoot.menubarOffY,
            this.width, this.height,
            textAreaWidth = this.width,
            readFromLang = true,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )


    private val moduleAreaHMargin = 48

    private val moduleAreaBorder = 8

    private val moduleAreaWidth = (Terrarum.WIDTH * 0.75).toInt() - moduleAreaHMargin
    private val moduleAreaHeight = Terrarum.HEIGHT - moduleAreaHMargin * 2

    private val moduleInfoCells = ArrayList<UIItemModuleInfoCell>()
    // build module list
    init {
        ModMgr.moduleInfo.toList().sortedBy { it.second.order }.forEachIndexed { index, it ->
            moduleInfoCells.add(UIItemModuleInfoCell(
                    this,
                    it.first,
                    moduleAreaWidth - 2 * moduleAreaBorder,
                    0, 0 // placeholder
            ))
        }
    }

    private val mouduleArea = UIItemList<UIItemModuleInfoCell>(
            this,
            moduleInfoCells,
            (Terrarum.WIDTH * 0.25f).toInt(), moduleAreaHMargin,
            moduleAreaWidth,
            moduleAreaHeight,
            inactiveCol = Color.WHITE,
            border = moduleAreaBorder
    )


    init {
        uiItems.add(menubar)
        uiItems.add(mouduleArea)


        ////////////////////////////




        // attach listeners

        menubar.buttons[menuLabels.indexOf("MENU_LABEL_RETURN")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            menubar.selectedIndex = menubar.defaultSelection
            superMenu.setAsOpen()
        }
    }

    override fun updateUI(delta: Float) {
        menubar.update(delta)
        mouduleArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        menubar.render(batch, camera)

        batch.color = Color.WHITE
        blendNormal()
        mouduleArea.render(batch, camera)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }

}