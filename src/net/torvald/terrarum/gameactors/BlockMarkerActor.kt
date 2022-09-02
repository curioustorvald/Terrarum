package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.floor

/**
 * Used as construction markers and fixture ghost images
 */
class BlockMarkerActor : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT), NoSerialise {

    private val defaultSize = 16.0

    override var referenceID: ActorID = 2147483647 // custom refID
    override val hitbox = Hitbox(0.0, 0.0, TILE_SIZED, TILE_SIZED)

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
            val hbx = (Terrarum.mouseTileX - floor((hitbox.width - 0.5).div(2) / TILE_SIZED)) * TILE_SIZED
            val hby = (Terrarum.mouseTileY - floor((hitbox.height - 0.5) / TILE_SIZED)) * TILE_SIZED
            hitbox.setPosition(hbx, hby)
        }
    }

    override fun onActorValueChange(key: String, value: Any?) { }


    fun setGhost(actor: ActorWithBody) {
        ghost = actor.sprite
        hasGhost = true
        hitbox.setDimension(actor.baseHitboxW.toDouble(), actor.baseHitboxH.toDouble())
    }

    fun unsetGhost() {
        ghost = null
        hasGhost = false
        setGhostColourNone()
        hitbox.setDimension(TILE_SIZED, TILE_SIZED)
    }

    fun setGhostColourNone() { ghostColour = Color.WHITE }
    fun setGhostColourAllow() { ghostColour = Color(-1) }
    fun setGhostColourBlock() { ghostColour = Color(0) }
}