package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
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

    private val modIcon = try {
        TextureRegion(Texture(modProp.iconFile))
    }
    catch (_: GdxRuntimeException) {
        CommonResourcePool.getAsTextureRegion("itemplaceholder_48")
    }
    private val modVer = modProp.version
    private val modDate = modProp.releaseDate
    private val modAuthor = modProp.author
    private val modDesc = modProp.description

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
    private val ccDesc = App.fontGame.toColorCode(13,13,13)

    override fun render(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, initialX, initialY, 32, height)
        Toolkit.fillArea(batch, initialX + 35, initialY, 48, height)
        Toolkit.fillArea(batch, initialX + 86, initialY, width - 86, height)

        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, initialX - 1, initialY - 1, width + 2, height + 2)
        Toolkit.fillArea(batch, initialX + 33, initialY, 1, height)
        Toolkit.fillArea(batch, initialX + 84, initialY, 1, height)

        if (order < 9)
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 13f, initialY + 18f + 12f)
        else if (order < 99)
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 9f, initialY + 18f + 12f)
        else
            App.fontSmallNumbers.draw(batch, "${order+1}", initialX + 6f, initialY + 18f + 12f)

        batch.color = Color.WHITE
        if (modErrored) {
            batch.shader = App.shaderGhastlyWhite
            batch.color = Color.LIGHT_GRAY
        }
        batch.draw(modIcon, initialX + 35f, initialY + 12f)
        batch.shader = null
        batch.color = Color.WHITE
        App.fontGame.draw(batch, "$ccZero${modName.toUpperCase()}$ccNum $modVer", initialX + 86f + 3f, initialY + 2f)
        App.fontGame.draw(batch, "$ccDesc$modDesc", initialX + 86f + 3f, initialY + 26f)
        App.fontGame.draw(batch, "$ccZero2$modAuthor$ccNum2 $modDate", initialX + 86f + 3f, initialY + 50f)

        if (modErrored) {
            batch.draw(CommonResourcePool.getAsTextureRegion("basegame_errored_icon32"), initialX + width - 40f, initialY + 8f + 12f)
        }
    }

    override fun dispose() {
        modIcon.texture.dispose()
    }
}