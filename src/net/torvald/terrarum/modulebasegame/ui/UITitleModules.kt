package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Second
import net.torvald.terrarum.blendNormal
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

    override var width = AppLoader.screenW - UIRemoCon.remoConWidth - moduleAreaHMargin
    override var height = AppLoader.screenH - moduleAreaHMargin * 2


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

    private val mouduleArea = UIItemList<UIItemModuleInfoCell>(
            this,
            moduleInfoCells,
            UIRemoCon.remoConWidth, moduleAreaHMargin,
            width,
            height,
            inactiveCol = Color.WHITE,
            border = moduleAreaBorder
    )


    init {
        uiItems.add(mouduleArea)
    }

    override fun updateUI(delta: Float) {
        mouduleArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        blendNormal(batch)
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