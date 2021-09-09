package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.floor

class UIItemModuleInfoCell(
        parent: UICanvas,
        var moduleName: String,
        override val width: Int,
        initialX: Int,
        initialY: Int
) : UIItem(parent, initialX, initialY) {

    override val height: Int = App.fontGame.lineHeight.toInt() * 2

    private val numberAreaWidth = App.fontSmallNumbers.W * 3 + 4

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        if (ModMgr.moduleInfo.containsKey(moduleName)) {
            val modInfo = ModMgr.moduleInfo[moduleName]!!

            // print load order index
            batch.color = Color(0xccccccff.toInt())
            var strlen = App.fontSmallNumbers.getWidth(modInfo.order.toString())
            App.fontSmallNumbers.draw(batch,
                    modInfo.order.toString(),
                    posX + (numberAreaWidth - strlen).div(2f).floor(),
                    posY + (height - App.fontSmallNumbers.H).div(2f).floor()
            )

            // print module name
            batch.color = Color.WHITE
            App.fontGame.draw(batch,
                    "${modInfo.properName} (${modInfo.version})",
                    posX + numberAreaWidth.toFloat(),
                    posY.toFloat()
            )

            // print author name
            strlen = App.fontGame.getWidth(modInfo.author)
            App.fontGame.draw(batch,
                    modInfo.author,
                    posX + width - strlen.toFloat(),
                    posY.toFloat()
            )

            // print description
            App.fontGame.draw(batch,
                    modInfo.description,
                    posX + numberAreaWidth.toFloat(),
                    posY + App.fontGame.lineHeight
            )

            // print releasedate
            strlen = App.fontGame.getWidth(modInfo.releaseDate)
            App.fontGame.draw(batch,
                    modInfo.releaseDate,
                    posX + width - strlen.toFloat(),
                    posY + App.fontGame.lineHeight
            )

        }
        else {
            batch.color = Color(0xff8080_ff.toInt())
            val str = "InternalError: no such module: '$moduleName'"
            val strlen = App.fontSmallNumbers.getWidth(str)
            App.fontSmallNumbers.draw(batch,
                    str,
                    posX + (width - numberAreaWidth - strlen).div(2f).floor() + numberAreaWidth,
                    posY + (height - App.fontSmallNumbers.H).div(2f).floor()
            )
        }
    }

    override fun dispose() {
    }
}