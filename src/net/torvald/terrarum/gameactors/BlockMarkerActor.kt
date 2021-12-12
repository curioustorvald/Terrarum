package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

class BlockMarkerActor : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT) {

    private val defaultSize = 16.0

    override var referenceID: ActorID = ReferencingRanges.ACTORS_OVERLAY.last // custom refID
    override val hitbox = Hitbox(0.0, 0.0, 16.0, 16.0)

    var markerColour = Color.YELLOW
    var markerShape = 0

    private val blockMarkings: TextureRegionPack
        get() = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common")

    private var ghost: SpriteAnimation? = null
    private var hasGhost = false
    var ghostColour = Color.WHITE


    init {
        this.isVisible = false
        renderOrder = Actor.RenderOrder.OVERLAY // for some reason the constructor didn't work
    }

    override fun drawBody(batch: SpriteBatch) {
        if (isVisible) {
            if (hasGhost) {
                batch.shader = App.shaderGhastlyWhite
                if (ghost != null) {
                    batch.color = ghostColour
                    drawSpriteInGoodPosition(ghost!!, batch)
                }
            }
            else {
                batch.shader = null
                batch.color = markerColour
                batch.draw(blockMarkings.get(markerShape, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
            }

        }

        batch.shader = null
        batch.color = Color.WHITE
    }

    override fun drawGlow(batch: SpriteBatch) {
        batch.color = Color.WHITE
    }

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


    fun setGhost(actor: ActorWithBody) {
        ghost = actor.sprite
        hasGhost = true
    }

    fun unsetGhost() {
        ghost = null
        hasGhost = false
        setGhostColourNone()
    }

    fun setGhostColourNone() { ghostColour = Color.WHITE }
    fun setGhostColourAllow() { ghostColour = Color(-1) }
    fun setGhostColourBlock() { ghostColour = Color(0) }
}