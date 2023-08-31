package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

class UIBuildingMakerToolbox : UICanvas() {

    val toolsTexture = TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/building_maker_toolbox.tga"), 16, 16)
    val tools = Array(toolsTexture.verticalCount) { UIItemImageButton(
            this, toolsTexture.get(0, it),
            initialX = 0,
            initialY = 20 * it,
            highlightable = true
    ) }

    override var width = 16
    override var height = 20 * tools.size - 4
    override var openCloseTime = 0f

    var selectedTool = 0; private set

    init {
        setAsAlwaysVisible()
        tools[selectedTool].highlighted = true
    }

    override fun updateUI(delta: Float) {
        tools.forEachIndexed { counter, it ->
            it.update(delta)

            if (it.highlighted) selectedTool = counter
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        tools.forEach { it.render(batch, camera) }
    }

    override fun doOpening(delta: Float) { }

    override fun doClosing(delta: Float) { }

    override fun endOpening(delta: Float) { }

    override fun endClosing(delta: Float) { }

    override fun dispose() {
        toolsTexture.dispose()
    }
}
