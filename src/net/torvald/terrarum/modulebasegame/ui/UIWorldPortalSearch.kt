package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.random.HQRNG
import net.torvald.random.XXHash64
import net.torvald.terrarum.App
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.WorldgenLoadScreen
import net.torvald.terrarum.modulebasegame.gameactors.FixtureWorldPortal
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.serialise.ReadActor
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.VDFileID
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.RandomWordsName

/**
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalSearch(val full: UIWorldPortal) : UICanvas() {

//    override var width: Int = Toolkit.drawWidth
//    override var height: Int = App.scr.height



    private val hugeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/huge.png")))
    private val largeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/large.png")))
    private val normalTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/normal.png")))
    private val smallTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/small.png")))

    private val tex = arrayOf(smallTex, normalTex, largeTex, hugeTex)

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val radioCellWidth = 116
    private val inputWidth = 340
    private val radioX = (width - (radioCellWidth * tex.size + 9)) / 2

    private val sizeSelY = 186 + 40

    private val sizeSelector = UIItemInlineRadioButtons(this,
        drawX + radioX, drawY + sizeSelY, radioCellWidth,
        listOf(
            { Lang["CONTEXT_DESCRIPTION_TINY"] },
            { Lang["CONTEXT_DESCRIPTION_SMALL"] },
            { Lang["CONTEXT_DESCRIPTION_BIG"] },
            { Lang["CONTEXT_DESCRIPTION_HUGE"] }
        )
    )

    private val rng = HQRNG()

    private val nameInput = UIItemTextLineInput(this,
        drawX + width - inputWidth, drawY + sizeSelY + 80, inputWidth,
        { RandomWordsName(4) }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES)
    )

    private val seedInput = UIItemTextLineInput(this,
        drawX + width - inputWidth, drawY + sizeSelY + 120, inputWidth,
        { rng.nextLong().toString() }, InputLenCap(256, InputLenCap.CharLenUnit.CODEPOINTS)
    )

    private val goButtonWidth = 180
    private val buttonY = drawY + height - 24

    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, drawX + (width/2 - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val goButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CONFIRM_BUTTON"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

    init {
        goButton.clickOnceListener = { _, _ ->
            val seed = try {
                seedInput.getTextOrPlaceholder().toLong()
            }
            catch (e: NumberFormatException) {
                XXHash64.hash(seedInput.getTextOrPlaceholder().toByteArray(Charsets.UTF_8), 10000)
            }
            val (wx, wy) = TerrarumIngame.WORLDPORTAL_NEW_WORLD_SIZE[sizeSelector.selection]
            val worldParam = TerrarumIngame.NewWorldParameters(wx, wy, seed, nameInput.getTextOrPlaceholder())
            full.host.teleportRequest = FixtureWorldPortal.TeleportRequest(null, worldParam)
            full.setAsClose()
        }
        backButton.clickOnceListener = { _, _ ->
            full.requestTransition(0)
        }

        addUIitem(sizeSelector)
        addUIitem(goButton)
        addUIitem(backButton)
        addUIitem(seedInput)  // order is important
        addUIitem(nameInput) // because of the IME candidates overlay
    }


    override fun show() {
        uiItems.forEach { it.show() }
        seedInput.clearText()
        seedInput.refreshPlaceholder()
        nameInput.clearText()
        nameInput.refreshPlaceholder()
    }

    private var oldPosX = full.posX

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val posXDelta = posX - oldPosX

        // ugh why won't you just scroll along??
//        seedInput.posX += posXDelta
//        nameInput.posX += posXDelta // is it fixed now?

        // TODO teleporter memory usage and whatnot

        batch.color = Color.WHITE
        // ui title
        val titlestr = Lang["CONTEXT_WORLD_NEW"]
        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), INVENTORY_CELLS_OFFSET_Y() - 36f)

        // draw size previews
        val texture = tex[sizeSelector.selection.coerceAtMost(tex.lastIndex)]
        val tx = drawX + (width - texture.regionWidth) / 2
        val ty = drawY + (160 - texture.regionHeight) / 2
        batch.draw(texture, tx.toFloat(), ty.toFloat())
        // border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, tx - 1, ty - 1, texture.regionWidth + 2, texture.regionHeight + 2)

        batch.color = Color.WHITE
        // size selector title
        val sizestr = Lang["MENU_OPTIONS_SIZE"]
        App.fontGame.draw(batch, sizestr, drawX + (width - App.fontGame.getWidth(sizestr)).div(2).toFloat(), drawY + sizeSelY - 40f)

        // name/seed input labels
        App.fontGame.draw(batch, Lang["MENU_NAME"], drawX, drawY + sizeSelY + 80)
        App.fontGame.draw(batch, Lang["CONTEXT_PLACE_COORDINATE"], drawX, drawY + sizeSelY + 120)

        uiItems.forEach { it.render(batch, camera) }


        oldPosX = posX
    }

    override fun hide() {
        uiItems.forEach { it.hide() }
    }

    override fun dispose() {
        hugeTex.texture.dispose()
        largeTex.texture.dispose()
        normalTex.texture.dispose()
        smallTex.texture.dispose()
    }

}