package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Second
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemList
import net.torvald.terrarum.ui.UIItemModuleInfoCell

/**
 * Created by minjaesong on 2017-08-01.
 */
class UITitleModules : UICanvas() {

    override var openCloseTime: Second = 0f

    private val moduleAreaHMargin = 48
    private val moduleAreaBorder = 8

    override var width = 600//App.scr.width - UIRemoCon.remoConWidth - moduleAreaHMargin
    override var height = App.scr.height - moduleAreaHMargin * 2


    private val moduleInfoCells = ArrayList<UIItemModuleInfoCell>()
    // build module list
    init {
        ModMgr.moduleInfo.toList().sortedBy { it.second.order }.forEachIndexed { index, it ->
            moduleInfoCells.add(UIItemModuleInfoCell(
                    this,
                    it.first,
                    width - 2 * moduleAreaBorder,
                    0, 0 // placeholder
            ))
        }
    }

    private val moduleArea = UIItemList<UIItemModuleInfoCell>(
            this,
            moduleInfoCells,
            (App.scr.width - this.width) / 2, moduleAreaHMargin,
            width,
            height,
            inactiveCol = Color.WHITE,
            border = moduleAreaBorder
    )


    init {
        uiItems.add(moduleArea)
    }

    override fun updateUI(delta: Float) {
        moduleArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        blendNormal(batch)
        Toolkit.drawBoxBorder(batch, moduleArea.posX, moduleArea.posY, width, height)
        moduleArea.render(batch, camera)
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