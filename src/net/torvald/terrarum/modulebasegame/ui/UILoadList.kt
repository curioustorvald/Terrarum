package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Movement
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.unicode.getKeycapConsole
import net.torvald.unicode.getKeycapPC
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadList(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_up"))}${getKeycapPC(App.getConfigInt("control_key_down"))}" +
                    " ${Lang["MENU_CONTROLS_SCROLL"]}"
        else
            "${getKeycapConsole('R')} ${Lang["MENU_CONTROLS_SCROLL"]}"

    internal val uiWidth = SAVE_CELL_WIDTH
    internal val uiXdiffChatOverlay = App.scr.chatWidth / 2
    internal val textH = App.fontGame.lineHeight.toInt()
    //    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10
    internal val cellGap = 20
    internal val cellInterval = cellGap + SAVE_CELL_HEIGHT
    internal val gradAreaHeight = 32
    //    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10
    internal val titleTopGradStart: Int = App.scr.tvSafeGraphicsHeight
    internal val titleTopGradEnd: Int = titleTopGradStart + gradAreaHeight
    internal val titleBottomGradStart: Int = height - App.scr.tvSafeGraphicsHeight - gradAreaHeight
    internal val titleBottomGradEnd: Int = titleBottomGradStart + gradAreaHeight
    internal val controlHelperY: Int = titleBottomGradStart + gradAreaHeight - textH

    private var scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
    private var listScroll = 0 // only update when animation is finished
    private var savesVisible = (scrollAreaHeight + cellGap) / cellInterval
    private var uiScroll = 0f
    private var scrollFrom = 0
    private var scrollTarget = 0
    private var scrollAnimCounter = 0f
    private val scrollAnimLen = 0.1f
    private var sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)

    private val playerCells = ArrayList<UIItemPlayerCells>()

    private val mode1Node = Yaml(UITitleRemoConYaml.injectedMenuSingleCharSel).parse()


    override fun show() {
        mode1Node.parent = full.remoCon.treeRoot
        mode1Node.data = "MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadSavegame"
        full.remoCon.setNewRemoConContents(mode1Node)

        try {
            full.remoCon.handler.lockToggle()
            full.showSpinner = true

            Thread {
                // read savegames
                var savegamesCount = 0
                App.sortedPlayers.forEach { uuid ->
                    val skimmer = App.savegamePlayers[uuid]!!.loadable()
                    val x = full.uiX
                    val y = titleTopGradEnd + cellInterval * savegamesCount
                    try {
                        playerCells.add(UIItemPlayerCells(full, x, y, skimmer))
                        savegamesCount += 1
                    }
                    catch (e: Throwable) {
                        System.err.println("[UILoadSavegame] Error while loading Player '${skimmer.diskFile.absolutePath}'")
                        e.printStackTrace()
                    }
                }


                full.remoCon.handler.unlockToggle()
                full.showSpinner = false
            }.start()

        }
        catch (e: UninitializedPropertyAccessException) {}
    }

    override fun updateUI(delta: Float) {
        if (scrollTarget != listScroll) {
            if (scrollAnimCounter < scrollAnimLen) {
                scrollAnimCounter += delta
                uiScroll = Movement.fastPullOut(
                    scrollAnimCounter / scrollAnimLen,
                    listScroll * cellInterval.toFloat(),
                    scrollTarget * cellInterval.toFloat()
                )
            }
            else {
                scrollAnimCounter = 0f
                listScroll = scrollTarget
                uiScroll = cellInterval.toFloat() * scrollTarget
            }
        }

        for (index in 0 until playerCells.size) {
            val it = playerCells[index]
            if (index in listScroll - 2 until listScroll + savesVisible + 2) {
                // re-position
                it.posY = (it.initialY - uiScroll).roundToInt()
                it.update(delta)
            }
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.end()

        val cells = playerCells

        lateinit var savePixmap: Pixmap
        sliderFBO.inAction(camera as OrthographicCamera, batch) {
            gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

            full.setCameraPosition(batch, camera, 0f, 0f)
            batch.color = Color.WHITE
            batch.inUse {
                for (index in 0 until cells.size) {
                    val it = cells[index]

                    if (App.getConfigBoolean("fx_streamerslayout"))
                        it.posX += uiXdiffChatOverlay

                    if (index in listScroll - 2 until listScroll + savesVisible + 2)
                        it.render(batch, camera)

                    if (App.getConfigBoolean("fx_streamerslayout"))
                        it.posX -= uiXdiffChatOverlay
                }
            }
            savePixmap = Pixmap.createFromFrameBuffer(0, 0, sliderFBO.width, sliderFBO.height)
            savePixmap.blending = Pixmap.Blending.None
        }


        // implement "wipe-out" by CPU-rendering (*deep exhale*)
        //savePixmap.setColor(1f,1f,1f,0f)
        savePixmap.setColor(0f, 0f, 0f, 0f)
        savePixmap.fillRectangle(0, savePixmap.height - titleTopGradStart, savePixmap.width, titleTopGradStart)
        // top grad
        for (y in titleTopGradStart until titleTopGradEnd) {
            val alpha = (y - titleTopGradStart).toFloat() / gradAreaHeight
            for (x in 0 until savePixmap.width) {
                val col = savePixmap.getPixel(x, savePixmap.height - y)
                val blendAlpha = (col.and(0xFF) * alpha).roundToInt()
                savePixmap.drawPixel(x, savePixmap.height - y, col.and(0xFFFFFF00.toInt()) or blendAlpha)
            }
        }
        // bottom grad
        for (y in titleBottomGradStart until titleBottomGradEnd) {
            val alpha = 1f - ((y - titleBottomGradStart).toFloat() / gradAreaHeight)
            for (x in 0 until savePixmap.width) {
                val col = savePixmap.getPixel(x, savePixmap.height - y)
                val blendAlpha = (col.and(0xFF) * alpha).roundToInt()
                savePixmap.drawPixel(x, savePixmap.height - y, col.and(0xFFFFFF00.toInt()) or blendAlpha)
            }
        }
        savePixmap.setColor(0f, 0f, 0f, 0f)
        savePixmap.fillRectangle(0, 0, savePixmap.width, height - titleBottomGradEnd + 1)



        full.setCameraPosition(batch, camera, 0f, 0f)
        val saveTex = TextureRegion(Texture(savePixmap)); saveTex.flip(false, true)
        batch.inUse {
            batch.draw(saveTex, (width - uiWidth - 10) / 2f, 0f)
            // Control help
            App.fontGame.draw(batch, controlHelp, full.uiX.toFloat(), controlHelperY.toFloat())
        }

        saveTex.texture.dispose()
        savePixmap.dispose()

        batch.begin()
    }

    override fun dispose() {
    }

    override fun keyDown(keycode: Int): Boolean {
        if (this.isVisible) {
            val cells = playerCells

            if ((keycode == Input.Keys.UP || keycode == App.getConfigInt("control_key_up")) && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if ((keycode == Input.Keys.DOWN || keycode == App.getConfigInt("control_key_down")) && scrollTarget < cells.size - savesVisible) {
                scrollFrom = listScroll
                scrollTarget += 1
                scrollAnimCounter = 0f
            }
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        playerCells.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        playerCells.forEach { it.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (this.isVisible) {
            val cells = playerCells

            if (amountY <= -1f && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if (amountY >= 1f && scrollTarget < cells.size - savesVisible) {
                scrollFrom = listScroll
                scrollTarget += 1
                scrollAnimCounter = 0f
            }
        }
        return true
    }

    override fun hide() {
        playerCells.forEach { it.dispose() }
        playerCells.clear()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
        savesVisible = (scrollAreaHeight + cellInterval) / (cellInterval + SAVE_CELL_HEIGHT)

        listScroll = 0
        scrollTarget = 0
        uiScroll = 0f

        sliderFBO.dispose()
        sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)
    }

}