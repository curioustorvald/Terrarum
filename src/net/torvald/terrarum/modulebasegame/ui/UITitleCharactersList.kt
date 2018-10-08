package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.serialise.SavegameLedger
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemList

/**
 * Created by minjaesong on 2018-09-15.
 */
class UITitleCharactersList : UICanvas() {

    override var openCloseTime: Second = 0f



    private val moduleAreaHMargin = 48

    private val moduleAreaBorder = 8

    override var width = (Terrarum.WIDTH * 0.75).toInt() - moduleAreaHMargin
    override var height = Terrarum.HEIGHT - moduleAreaHMargin * 2

    private val moduleInfoCells = ArrayList<UIItemSavegameInfoCell>()
    // build characters list
    init {
        //SavegameLedger.getSavefileList()?.forEachIndexed { index, file ->
        //
        //}
    }

    private val mouduleArea = UIItemList<UIItemSavegameInfoCell>(
            this,
            moduleInfoCells,
            (Terrarum.WIDTH * 0.25f).toInt(), moduleAreaHMargin,
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