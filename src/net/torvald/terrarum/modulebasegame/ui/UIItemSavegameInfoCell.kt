package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
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
        initialX: Int,
        initialY: Int
) : UIItem(parent, initialX, initialY) {

    override val height: Int = App.fontGame.lineHeight.toInt() * 2

    override fun render(batch: SpriteBatch, camera: Camera) {

    }

    override fun dispose() {

    }
}