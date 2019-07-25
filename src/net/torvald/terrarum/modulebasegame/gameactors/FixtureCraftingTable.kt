package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureCraftingTable : FixtureBase(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
        mainUI = UICraftingTable
) {

    init {
        setHitboxDimension(16, 16, 0, 0)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("itemplaceholder_16").texture, 16, 16))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    companion object {
        const val MASS = 2.0
    }
}


internal object UICraftingTable : UICanvas() {
    override var width = 512
    override var height = 512
    override var openCloseTime: Second = 0.0f

    init {
        handler.allowESCtoClose = true
    }

    override fun updateUI(delta: Float) {

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        batch.color = Color.WHITE
        batch.draw(CommonResourcePool.getAsTextureRegion("test_texture"), 0f, 0f)

    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.paused = true
    }

    override fun doClosing(delta: Float) {
        Terrarum.ingame?.paused = false
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }



    override fun dispose() {
    }
}