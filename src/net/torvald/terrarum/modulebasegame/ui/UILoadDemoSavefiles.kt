package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.tvda.ByteArray64InputStream
import net.torvald.terrarum.tvda.VDUtil
import net.torvald.terrarum.tvda.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.LoadSavegame
import net.torvald.terrarum.serialise.ReadMeta
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Created by minjaesong on 2021-09-09.
 */
class UILoadDemoSavefiles : UICanvas() {

    override var width: Int
        get() = App.scr.width
        set(value) {}
    override var height: Int
        get() = App.scr.height
        set(value) {}
    override var openCloseTime: Second = 0f

    // read savegames
    init {
        File(App.defaultSaveDir).listFiles().forEachIndexed { index, file ->
            printdbg(this, "save file: ${file.absolutePath}")

            try {
                val x = (width - UIItemDemoSaveCells.WIDTH) / 2
                val y = 144 + (24 + UIItemDemoSaveCells.HEIGHT) * index
                val disk = VDUtil.readDiskArchive(file, charset = Common.CHARSET)
                addUIitem(UIItemDemoSaveCells(this, x, y, disk))
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        uiItems.forEach { it.render(batch, camera) }

        val loadGameTitleStr = Lang["MENU_IO_LOAD_GAME"]
        App.fontGame.draw(batch, loadGameTitleStr, (width - App.fontGame.getWidth(loadGameTitleStr)).div(2).toFloat(), 62f)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {}
}

class UIItemDemoSaveCells(
        parent: UILoadDemoSavefiles,
        initialX: Int,
        initialY: Int,
        val disk: VirtualDisk) : UIItem(parent, initialX, initialY) {

    companion object {
        const val WIDTH = 480
        const val HEIGHT = 160
    }

    override val width: Int = WIDTH
    override val height: Int = HEIGHT

    private lateinit var thumbPixmap: Pixmap
    private lateinit var thumb: TextureRegion
    private val grad = CommonResourcePool.getAsTexture("title_halfgrad")

    private val meta = ReadMeta(disk)

    private val x = initialX.toFloat()
    private val y = initialY.toFloat()

    init {
        try {
            // load a thumbnail
            val zippedTga = VDUtil.getAsNormalFile(disk, -2).getContent()
            val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
            val tgaFileContents = gzin.readAllBytes(); gzin.close()

            thumbPixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
            val thumbTex = Texture(thumbPixmap)
            thumbTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            thumb = TextureRegion(thumbTex)
            thumb.setRegion(0, thumbTex.height / 4, thumbTex.width, thumbTex.height / 2)
        }
        catch (e: NullPointerException) {
            // use stock texture
        }


    }

    override var clickOnceListener: ((Int, Int, Int) -> Unit)? = { _: Int, _: Int, _: Int ->
        LoadSavegame(disk)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE

        // draw thumbnail
        blendNormal(batch)
        batch.draw(thumb, x, y + height, width.toFloat(), -height.toFloat())
        // draw gradient
        blendMul(batch)
        batch.draw(grad, x + width, y, -width.toFloat(), height.toFloat())
        // draw timestamp
        blendNormal(batch)
        val timestamp = "${meta.lastplay_t}"
        val tlen = App.fontGame.getWidth(timestamp)
        App.fontGame.draw(batch, timestamp, posX + (width - tlen) - 5f, posY + height - 23f)


        super.render(batch, camera)
    }

    override fun dispose() {
        thumb.texture.dispose()
        thumbPixmap.dispose()
    }

}