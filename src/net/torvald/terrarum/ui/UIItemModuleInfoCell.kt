package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.floor

class UIItemModuleInfoCell(
        parent: UICanvas,
        var moduleName: String,
        override val width: Int,
        override var posX: Int,
        override var posY: Int
) : UIItem(parent) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    override val height: Int = Terrarum.fontGame.lineHeight.toInt() * 2

    private val numberAreaWidth = Terrarum.fontSmallNumbers.W * 3 + 4

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        if (ModMgr.moduleInfo.containsKey(moduleName)) {
            val modInfo = ModMgr.moduleInfo[moduleName]!!

            // print load order index
            batch.color = Color(0xccccccff.toInt())
            var strlen = Terrarum.fontSmallNumbers.getWidth(modInfo.order.toString())
            Terrarum.fontSmallNumbers.draw(batch,
                    modInfo.order.toString(),
                    posX + (numberAreaWidth - strlen).div(2f).floor(),
                    posY + (height - Terrarum.fontSmallNumbers.H).div(2f).floor()
            )

            // print module name
            batch.color = Color.WHITE
            Terrarum.fontGame.draw(batch,
                    "${modInfo.properName} (${modInfo.version})",
                    posX + numberAreaWidth.toFloat(),
                    posY.toFloat()
            )

            // print author name
            strlen = Terrarum.fontGame.getWidth(modInfo.author)
            Terrarum.fontGame.draw(batch,
                    modInfo.author,
                    posX + width - strlen.toFloat(),
                    posY.toFloat()
            )

            // print description
            Terrarum.fontGame.draw(batch,
                    modInfo.description,
                    posX + numberAreaWidth.toFloat(),
                    posY + Terrarum.fontGame.lineHeight
            )

            // print releasedate
            strlen = Terrarum.fontGame.getWidth(modInfo.releaseDate)
            Terrarum.fontGame.draw(batch,
                    modInfo.releaseDate,
                    posX + width - strlen.toFloat(),
                    posY + Terrarum.fontGame.lineHeight
            )

        }
        else {
            batch.color = Color(0xff8080_ff.toInt())
            val str = "InternalError: no such module: '$moduleName'"
            val strlen = Terrarum.fontSmallNumbers.getWidth(str)
            Terrarum.fontSmallNumbers.draw(batch,
                    str,
                    posX + (width - numberAreaWidth - strlen).div(2f).floor() + numberAreaWidth,
                    posY + (height - Terrarum.fontSmallNumbers.H).div(2f).floor()
            )
        }
    }

    override fun dispose() {
    }
}