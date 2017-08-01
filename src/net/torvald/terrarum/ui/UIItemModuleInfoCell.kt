package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.floor

class UIItemModuleInfoCell(
        parent: UICanvas,
        var moduleName: String,
        override val width: Int,
        override var posX: Int,
        override var posY: Int
) : UIItem(parent) {

    override val height: Int = Terrarum.fontGame.lineHeight.toInt() * 2

    private val numberAreaWidth = Terrarum.fontSmallNumbers.W * 3 + 4

    override fun render(batch: SpriteBatch) {
        if (ModMgr.moduleInfo.containsKey(moduleName)) {
            val modInfo = ModMgr.moduleInfo[moduleName]!!

            // print load order index
            batch.color = Color(0x7f7f7fff)
            var strlen = Terrarum.fontSmallNumbers.getWidth(modInfo.order.toString())
            Terrarum.fontSmallNumbers.draw(batch,
                    modInfo.order.toString(),
                    (numberAreaWidth - strlen).div(2f).floor(),
                    (height - Terrarum.fontSmallNumbers.H).div(2f).floor()
            )

            // print module name
            batch.color = Color.WHITE
            Terrarum.fontGame.draw(batch,
                    "${modInfo.properName} (${modInfo.version})",
                    numberAreaWidth.toFloat(),
                    0f
            )

            // print author name
            strlen = Terrarum.fontGame.getWidth(modInfo.author)
            Terrarum.fontGame.draw(batch,
                    modInfo.author,
                    width - strlen.toFloat(),
                    0f
            )

            // print description
            Terrarum.fontGame.draw(batch,
                    modInfo.description,
                    numberAreaWidth.toFloat(),
                    Terrarum.fontGame.lineHeight
            )

            // print releasedate
            strlen = Terrarum.fontGame.getWidth(modInfo.releaseDate)
            Terrarum.fontGame.draw(batch,
                    modInfo.releaseDate,
                    width - strlen.toFloat(),
                    Terrarum.fontGame.lineHeight
            )

        }
        else {
            batch.color = Color(0xff8080_ff.toInt())
            val str = "InternalError: no such module: '$moduleName'"
            val strlen = Terrarum.fontSmallNumbers.getWidth(str)
            Terrarum.fontSmallNumbers.draw(batch,
                    str,
                    (width - numberAreaWidth - strlen).div(2f).floor() + numberAreaWidth,
                    (height - Terrarum.fontSmallNumbers.H).div(2f).floor()
            )
        }
    }

    override fun dispose() {
    }
}