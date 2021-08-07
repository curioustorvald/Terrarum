package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

class BlockMarkerActor : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT) {

    override var referenceID: ActorID = 1048575 // custom refID
    override val hitbox = Hitbox(0.0, 0.0, 16.0, 16.0)

    var color = Color.YELLOW
    var shape = 0

    private val blockMarkings: TextureRegionPack
        get() = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common")

    init {
        this.referenceID = ReferencingRanges.ACTORS_OVERLAY.last
        this.isVisible = false
    }

    override fun drawBody(batch: SpriteBatch) {
        if (isVisible) {
            batch.color = color
            batch.draw(blockMarkings.get(shape, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
        }
    }

    override fun drawGlow(batch: SpriteBatch) { }

    override fun dispose() {
    }

    override fun update(delta: Float) {
        if (isVisible) {
            hitbox.setPosition(
                    Terrarum.mouseTileX * 16.0,
                    Terrarum.mouseTileY * 16.0
            )
        }
    }

    override fun onActorValueChange(key: String, value: Any?) { }

}