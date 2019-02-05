package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendScreen
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.serialise.ReadWorldInfo
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import java.util.*

/**
 * Created by minjaesong on 2019-02-05.
 *
 * If ingamePlayer is specified, sprite of current ingamePlayer will be drawn, instead of the SaveMetaData's thumbnail.
 */
class UIItemPlayerInfoCell(
        parent: UICanvas,
        val saveInfo: ReadWorldInfo.SaveMetaData,
        override val width: Int,
        override var posX: Int,
        override var posY: Int,
        var highlightable: Boolean,
        var ingamePlayer: IngamePlayer? = null
) : UIItem(parent) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    override val height = HEIGHT

    companion object {
        const val HEIGHT = 64
    }

    private val spriteAreaWidth = 56
    private val spriteToNameAreaGap = 8
    private val edgeGap = 8

    private val backColInactive = ItemSlotImageFactory.CELLCOLOUR_BLACK
    private val backColActive = ItemSlotImageFactory.CELLCOLOUR_BLACK_ACTIVE

    private val textRow1 = (((height / 2) - Terrarum.fontGame.lineHeight) / 2).toFloat()
    private val textRow2 = textRow1 + (height / 2)

    private val creationTimeStr: String
    private val modificationTimeStr: String

    private val worldCountStr: String
    private val worldCountStrWidth: Int

    init {
        val cal = Calendar.getInstance()

        cal.timeInMillis = saveInfo.creationTime * 1000
        creationTimeStr = "${cal[Calendar.YEAR]}-" +
                "${cal[Calendar.MONTH].toString().padStart(2,'0')}-" +
                "${cal[Calendar.DATE].toString().padStart(2,'0')}"

        cal.timeInMillis = saveInfo.lastPlayTime * 1000
        modificationTimeStr = "${cal[Calendar.YEAR]}-" +
                "${cal[Calendar.MONTH].toString().padStart(2,'0')}-" +
                "${cal[Calendar.DATE].toString().padStart(2,'0')}"


        worldCountStr = Lang["CONTEXT_WORLD_COUNT"] + saveInfo.worldCount
        worldCountStrWidth = Terrarum.fontGame.getWidth(worldCountStr)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        // background
        if (highlightable && mouseUp) {
            batch.color = backColActive
            blendScreen(batch)
        }
        else {
            batch.color = backColInactive
            blendNormal(batch)
        }

        batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())


        blendNormal(batch)
        /*batch.color = SPRITE_DRAW_COL

        // character sprite image
        if (ingamePlayer != null) {
            val spriteImage = ingamePlayer?.sprite?.textureRegion?.get(0,0)
            batch.draw(spriteImage,
                    ((spriteImage?.regionWidth ?: 2) - spriteAreaWidth).div(2).toFloat(),
                    ((spriteImage?.regionHeight ?: 2) - height).div(2).toFloat()
            )
        }
        else {
            val spriteImage = saveInfo.thumbnail
            batch.draw(spriteImage,
                    (spriteImage.width - spriteAreaWidth).div(2).toFloat(),
                    (spriteImage.height - height).div(2).toFloat()
            )
        }

        // texts //

        // name
        batch.color = Color.WHITE
        Terrarum.fontGame.draw(batch, saveInfo.playerName, spriteAreaWidth + spriteToNameAreaGap.toFloat(), textRow1)
        // creation and modification time
        Terrarum.fontGame.draw(batch, "$creationTimeStr/$modificationTimeStr", spriteAreaWidth + spriteToNameAreaGap.toFloat(), textRow2)
        // world count
        Terrarum.fontGame.draw(batch, worldCountStr, width - (edgeGap + worldCountStrWidth).toFloat(), textRow1)
        // wallet
        val walletStr = "¤ " + (ingamePlayer?.inventory?.wallet ?: saveInfo.playerWallet)
        val walletStrWidth = Terrarum.fontGame.getWidth(walletStr)
        Terrarum.fontGame.draw(batch, walletStr, width - (edgeGap + walletStrWidth).toFloat(), textRow2)
        */
    }

    override fun update(delta: Float) {
        super.update(delta)



        oldPosX = posX
        oldPosY = posY
    }

    override fun dispose() {

    }
}