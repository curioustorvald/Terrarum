package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
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

    override val height: Int = AppLoader.fontGame.lineHeight.toInt() * 2

    private val numberAreaWidth = AppLoader.fontSmallNumbers.W * 3 + 4

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        if (ModMgr.moduleInfo.containsKey(moduleName)) {
            val modInfo = ModMgr.moduleInfo[moduleName]!!

            // print load order index
            batch.color = Color(0xccccccff.toInt())
            var strlen = AppLoader.fontSmallNumbers.getWidth(modInfo.order.toString())
            AppLoader.fontSmallNumbers.draw(batch,
                    modInfo.order.toString(),
                    posX + (numberAreaWidth - strlen).div(2f).floor(),
                    posY + (height - AppLoader.fontSmallNumbers.H).div(2f).floor()
            )

            // print module name
            batch.color = Color.WHITE
            AppLoader.fontGame.draw(batch,
                    "${modInfo.properName} (${modInfo.version})",
                    posX + numberAreaWidth.toFloat(),
                    posY.toFloat()
            )

            // print author name
            strlen = AppLoader.fontGame.getWidth(modInfo.author)
            AppLoader.fontGame.draw(batch,
                    modInfo.author,
                    posX + width - strlen.toFloat(),
                    posY.toFloat()
            )

            // print description
            AppLoader.fontGame.draw(batch,
                    modInfo.description,
                    posX + numberAreaWidth.toFloat(),
                    posY + AppLoader.fontGame.lineHeight
            )

            // print releasedate
            strlen = AppLoader.fontGame.getWidth(modInfo.releaseDate)
            AppLoader.fontGame.draw(batch,
                    modInfo.releaseDate,
                    posX + width - strlen.toFloat(),
                    posY + AppLoader.fontGame.lineHeight
            )

        }
        else {
            batch.color = Color(0xff8080_ff.toInt())
            val str = "InternalError: no such module: '$moduleName'"
            val strlen = AppLoader.fontSmallNumbers.getWidth(str)
            AppLoader.fontSmallNumbers.draw(batch,
                    str,
                    posX + (width - numberAreaWidth - strlen).div(2f).floor() + numberAreaWidth,
                    posY + (height - AppLoader.fontSmallNumbers.H).div(2f).floor()
            )
        }
    }

    override fun dispose() {
    }
}