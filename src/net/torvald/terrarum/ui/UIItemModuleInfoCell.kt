package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.modulebasegame.ui.MODULEINFO_CELL_HEIGHT
import net.torvald.terrarum.modulebasegame.ui.MODULEINFO_CELL_WIDTH

class UIItemModuleInfoCell(
        parent: UICanvas,
        var order: Int,
        initialX: Int,
        initialY: Int
) : UIItem(parent, initialX, initialY) {

    override val width = MODULEINFO_CELL_WIDTH
    override val height = MODULEINFO_CELL_HEIGHT

    private val modName = ModMgr.loadOrder[order]

    private val modProp = ModMgr.moduleInfo[modName] ?: ModMgr.moduleInfoErrored[modName]!!

    private val modErrored = (ModMgr.moduleInfo[modName] == null)

    private val modIcon = TextureRegion(Texture(modProp.iconFile))
    private val modVer = modProp.version
    private val modDate = modProp.releaseDate
    private val modAuthor = modProp.author

    init {
        modIcon.flip(false, false)

        CommonResourcePool.addToLoadingList("basegame_errored_icon32") {
            val t = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/modwitherror.tga")))
            t.flip(false, false)
            t
        }
        CommonResourcePool.loadAll()
    }

    private val ccZero = App.fontGame.toColorCode(15,15,15)
    private val ccZero2 = App.fontGame.toColorCode(12,12,12)
    private val ccNum = App.fontGame.toColorCode(15,14,6)
    private val ccNum2 = App.fontGame.toColorCode(12,11,4)

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, initialX, initialY, 32, 48)
        Toolkit.fillArea(batch, initialX + 35, initialY, 48, 48)
        Toolkit.fillArea(batch, initialX + 86, initialY, width - 86, 48)

        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, initialX - 1, initialY - 1, width + 2, height + 2)
        Toolkit.fillArea(batch, initialX + 33, initialY, 1, 48)
        Toolkit.fillArea(batch, initialX + 84, initialY, 1, 48)

        if (order < 9)
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 13f, initialY + 18f)
        else if (order < 99)
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 9f, initialY + 18f)
        else
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 6f, initialY + 18f)

        batch.color = Color.WHITE
        batch.draw(modIcon, initialX + 35f, initialY.toFloat())
        App.fontGame.draw(batch, "$ccZero${modName.toUpperCase()}$ccNum $modVer", initialX + 86f + 6f, initialY + 2f)
        App.fontGame.draw(batch, "$ccZero2$modAuthor$ccNum2 $modDate", initialX + 86f + 6f, initialY + 26f)

        if (modErrored) {
            batch.draw(CommonResourcePool.getAsTextureRegion("basegame_errored_icon32"), initialX + width - 40f, initialY + 8f)
        }
    }

    override fun dispose() {
        modIcon.texture.dispose()
    }
}