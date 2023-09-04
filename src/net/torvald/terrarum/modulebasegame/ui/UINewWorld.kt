package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.random.HQRNG
import net.torvald.random.XXHash64
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.TerrarumIngame.Companion.NEW_WORLD_SIZE
import net.torvald.terrarum.modulebasegame.WorldgenLoadScreen
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.modulebasegame.serialise.ReadActor
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.serialise.toBigInt64
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.PasswordBase32
import net.torvald.terrarum.utils.RandomWordsName
import java.util.UUID

/**
 * Created by minjaesong on 2021-10-25.
 */
class UINewWorld(val remoCon: UIRemoCon) : UICanvas() {

    private var newPlayerCreationThread = Thread {}
    private var existingPlayer: DiskSkimmer? = null

    constructor(remoCon: UIRemoCon, playerCreationThread: Thread) : this(remoCon) {
        newPlayerCreationThread = playerCreationThread
    }

    constructor(remoCon: UIRemoCon, importedPlayer: DiskSkimmer) : this(remoCon) {
        existingPlayer = importedPlayer
    }

    private val hugeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/huge.png")))
    private val largeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/large.png")))
    private val normalTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/normal.png")))
    private val smallTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/small.png")))

    private val tex = arrayOf(/*smallTex, */normalTex, largeTex, hugeTex)

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val radioCellWidth = 120
    private val inputWidth = 350
    private val radioX = (width - (radioCellWidth * NEW_WORLD_SIZE.size + 9)) / 2
    private val inputX = drawX + width - inputWidth + 5

    private val sizeSelY = 186 + 40

    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10

    private val sizeSelector = UIItemInlineRadioButtons(this,
            drawX + radioX, drawY + sizeSelY, radioCellWidth,
            if (App.IS_DEVELOPMENT_BUILD)
                listOf(
                        { Lang["CONTEXT_DESCRIPTION_TINY"] },
//                        { Lang["CONTEXT_DESCRIPTION_TINY"] }, // only available for World Portal
                        { Lang["CONTEXT_DESCRIPTION_SMALL"] }, // ;p
                        { Lang["CONTEXT_DESCRIPTION_BIG"] },
                        { Lang["CONTEXT_DESCRIPTION_HUGE"] }
                )
            else
                listOf(
//                        { Lang["CONTEXT_DESCRIPTION_TINY"] }, // only available for World Portal
                        { Lang["CONTEXT_DESCRIPTION_SMALL"] }, // ;p
                        { Lang["CONTEXT_DESCRIPTION_BIG"] },
                        { Lang["CONTEXT_DESCRIPTION_HUGE"] }
                )
    )

    private val rng = HQRNG()

    private val inputLineY1 = 90
    private val inputLineY2 = 130
    private val goButtonWidth = 180
    private val gridGap = 10
    private val buttonBaseX = (Toolkit.drawWidth - 3 * goButtonWidth - 2 * gridGap) / 2
    private val buttonY = drawY + height - 24

    private var mode = 0 // 0: new world, 1: use invitation

    private var uiItemsChangeRequest: (() -> Unit)? = null

    private val nameInput = UIItemTextLineInput(this,
        inputX, drawY + sizeSelY + inputLineY1, inputWidth,
            { RandomWordsName(4) }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES))

    private val seedInput = UIItemTextLineInput(this,
        inputX, drawY + sizeSelY + inputLineY2, inputWidth,
            { rng.nextLong().toString() }, InputLenCap(256, InputLenCap.CharLenUnit.CODEPOINTS))

    private val codeInput = UIItemTextLineInput(this,
        inputX, drawY + sizeSelY, inputWidth,
        { "AAAA BB CCCCC DDDDD EEEEE FFFFF" }, InputLenCap(31, InputLenCap.CharLenUnit.CODEPOINTS)).also {

        // reset importReturnCode if the text input has changed
        it.onKeyDown = { _ ->
            importReturnCode = 0
        }
    }

    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonBaseX, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            remoCon.openUI(UILoadSavegame(remoCon))
        }
    }
    private val useInvitationButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_USE_CODE"] }, buttonBaseX + goButtonWidth + gridGap, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            if (mode == 0) {
                it.textfun = { Lang["CONTEXT_WORLD_NEW"] }
                uiItemsChangeRequest = {
                    uiItems.clear()
                    addUIitem(codeInput)
                    addUIitem(goButton)
                    addUIitem(it)
                    addUIitem(backButton)
                }
                mode = 1
            }
            else if (mode == 1) {
                it.textfun = { Lang["MENU_LABEL_USE_CODE"] }
                uiItemsChangeRequest = {
                    uiItems.clear()
                    addUIitem(sizeSelector)
                    addUIitem(seedInput)  // order is important
                    addUIitem(nameInput) // because of the IME candidates overlay
                    addUIitem(goButton)
                    addUIitem(it)
                    addUIitem(backButton)
                }
                mode = 0
            }
        }
    }
    private val goButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CONFIRM_BUTTON"] }, buttonBaseX + (goButtonWidth + gridGap) * 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->


            if (mode == 0) {
                // after the save is complete, proceed to new world generation
                if (existingPlayer == null) {
                    newPlayerCreationThread.start()
                    newPlayerCreationThread.join()
                }


                printdbg(this, "generate! Size=${sizeSelector.selection}, Name=${nameInput.getTextOrPlaceholder()}, Seed=${seedInput.getTextOrPlaceholder()}")

                val ingame = TerrarumIngame(App.batch)
                val playerDisk = existingPlayer ?: App.savegamePlayers[UILoadGovernor.playerUUID]!!.loadable()
                val player = ReadActor.invoke(
                    playerDisk,
                    ByteArray64Reader(playerDisk.getFile(SAVEGAMEINFO)!!.bytes, Common.CHARSET)
                ) as IngamePlayer
                val seed = try {
                    seedInput.getTextOrPlaceholder().toLong()
                }
                catch (e: NumberFormatException) {
                    XXHash64.hash(seedInput.getTextOrPlaceholder().toByteArray(Charsets.UTF_8), 10000)
                }
                val (wx, wy) = TerrarumIngame.NEW_WORLD_SIZE[sizeSelector.selection]
                val worldParam = TerrarumIngame.NewGameParams(
                    player, TerrarumIngame.NewWorldParameters(
                        wx, wy, seed, nameInput.getTextOrPlaceholder()
                    )
                )
                ingame.gameLoadInfoPayload = worldParam
                ingame.gameLoadMode = TerrarumIngame.GameLoadMode.CREATE_NEW

                Terrarum.setCurrentIngameInstance(ingame)
                val loadScreen = WorldgenLoadScreen(ingame, wx, wy)
                App.setLoadScreen(loadScreen)
            }
            else {
                val code = codeInput.getText().replace(" ", "")
                val uuid = PasswordBase32.decode(code, 16).let {
                    UUID(it.toBigInt64(0), it.toBigInt64(8))
                }
                val world = App.savegameWorlds[uuid]

                printdbg(this, "Decoded UUID=$uuid")

                // world exists?
                if (world == null) {
                    importReturnCode = 1
                }
                else {
                    TODO()

                    // after the save is complete, proceed to importing
                    if (existingPlayer == null) {
                        newPlayerCreationThread.start()
                        newPlayerCreationThread.join()
                    }
                }
            }
        }
    }

    private var importReturnCode = 0
    private val errorMessages = listOf(
        "", // 0
        Lang["ERROR_WORLD_NOT_FOUND"], // 1
    )

    init {
        addUIitem(sizeSelector)
        addUIitem(seedInput)  // order is important
        addUIitem(nameInput) // because of the IME candidates overlay
        addUIitem(goButton)
        addUIitem(useInvitationButton)
        addUIitem(backButton)
    }

    override fun show() {
        super.show()
        seedInput.clearText()
        nameInput.clearText()
        codeInput.clearText()
        importReturnCode = 0
    }

    override fun updateUI(delta: Float) {
        if (uiItemsChangeRequest != null) {
            uiItemsChangeRequest!!()
            uiItemsChangeRequest = null
        }

        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE
        // ui title
//        val titlestr = Lang["CONTEXT_WORLD_NEW"]
//        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), titleTextPosY.toFloat())

        if (mode == 0) {
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
            App.fontGame.draw(
                batch,
                sizestr,
                drawX + (width - App.fontGame.getWidth(sizestr)).div(2).toFloat(),
                drawY + sizeSelY - 40f
            )

            // name/seed input labels
            App.fontGame.draw(batch, Lang["MENU_NAME"], drawX - 4, drawY + sizeSelY + inputLineY1)
            App.fontGame.draw(batch, Lang["CONTEXT_GENERATOR_SEED"], drawX - 4, drawY + sizeSelY + inputLineY2)
        }
        else if (mode == 1) {
            // code input labels
            App.fontGame.draw(batch, Lang["CREDITS_CODE"], drawX - 4, drawY + sizeSelY)

            if (importReturnCode != 0) {
                batch.color = Toolkit.Theme.COL_RED
                val tby = codeInput.posY
                val btny = backButton.posY
                Toolkit.drawTextCentered(batch, App.fontGame, errorMessages[importReturnCode], Toolkit.drawWidth, 0, (tby + btny) / 2)
            }
        }

        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
        hugeTex.texture.dispose()
        largeTex.texture.dispose()
        normalTex.texture.dispose()
        smallTex.texture.dispose()
    }
}