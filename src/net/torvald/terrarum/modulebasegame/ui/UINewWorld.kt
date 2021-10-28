package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Second
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.RandomWordsName

/**
 * Created by minjaesong on 2021-10-25.
 */
class UINewWorld(val remoCon: UIRemoCon) : UICanvas() {

    private val hugeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/huge.png")))
    private val largeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/large.png")))
    private val normalTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/normal.png")))
    private val smallTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/small.png")))

    private val tex = arrayOf(smallTex, normalTex, largeTex, hugeTex)

    override var width = 480
    override var height = 480
    override var openCloseTime: Second = 0f

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val radioCellWidth = 100
    private val inputWidth = 340
    private val radioX = (width - (radioCellWidth * 4 + 9)) / 2
    private val inputX = width - inputWidth

    private val sizeSelY = 186 + 40

    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10

    private val sizeSelector = UIItemInlineRadioButtons(this,
            drawX + radioX, drawY + sizeSelY, radioCellWidth,
            listOf(
                    { Lang["CONTEXT_DESCRIPTION_SMALL"] },
                    { Lang["MENU_SETTING_MEDIUM"] }, // ;p
                    { Lang["CONTEXT_DESCRIPTION_BIG"] },
                    { Lang["CONTEXT_DESCRIPTION_HUGE"] }
            ))

    private val rng = HQRNG()

    private val nameInput = UIItemTextLineInput(this,
            drawX + width - inputWidth, drawY + sizeSelY + 80, inputWidth,
            { RandomWordsName(4) }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES))

    private val seedInput = UIItemTextLineInput(this,
            drawX + width - inputWidth, drawY + sizeSelY + 120, inputWidth,
            { rng.nextLong().toString() }, InputLenCap(256, InputLenCap.CharLenUnit.CODEPOINTS))

    private val goButtonWidth = 180
    private val backButton = UIItemTextButton(this, "MENU_LABEL_BACK", drawX + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val goButton = UIItemTextButton(this, "MENU_LABEL_CONFIRM_BUTTON", drawX + width/2 + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

    init {
        tex.forEach { it.flip(false, true) }

        goButton.touchDownListener = { _, _, _, _ ->
            printdbg(this, "generate! Size=${sizeSelector.selection}, Name=${nameInput.getTextOrPlaceholder()}, Seed=${seedInput.getTextOrPlaceholder()}")
        }
        backButton.touchDownListener = { _, _, _, _ ->
            remoCon.openUI(UILoadDemoSavefiles(remoCon, 1))
        }

        addUIitem(sizeSelector)
        addUIitem(seedInput)  // order is important
        addUIitem(nameInput) // because of the IME candidates overlay
        addUIitem(goButton)
        addUIitem(backButton)
    }


    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        // ui title
        val titlestr = Lang["CONTEXT_WORLD_NEW"]
        // "Game Load"
        App.fontGame.draw(batch, titlestr, drawX + (width - App.fontGame.getWidth(titlestr)).div(2).toFloat(), titleTextPosY.toFloat())

        // draw size previews
        val texture = tex[sizeSelector.selection]
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
        App.fontGame.draw(batch, Lang["CONTEXT_GENERATOR_SEED"], drawX, drawY + sizeSelY + 120)

        uiItems.forEach { it.render(batch, camera) }
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        hugeTex.texture.dispose()
        largeTex.texture.dispose()
        normalTex.texture.dispose()
        smallTex.texture.dispose()
    }
}