package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import java.io.File

/**
 * @param savefile TEVd file
 *
 * Created by minjaesong on 2018-09-15.
 */
class UIItemSavegameInfoCell(
        parent: UICanvas,
        savefile: File,
        override val width: Int,
        override var posX: Int,
        override var posY: Int
) : UIItem(parent) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    override val height: Int = Terrarum.fontGame.lineHeight.toInt() * 2

    override fun render(batch: SpriteBatch, camera: Camera) {

    }

    override fun dispose() {

    }
}