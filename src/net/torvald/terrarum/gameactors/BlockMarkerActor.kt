package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.floor

/**
 * Used as construction markers and fixture ghost images.
 *
 * `isVisible` behaves differently by `markerMode`.
 * - FIXTURE_GHOST: `isVisible` toggles if the ghost is being updated. FALSE - will not be updated and also not visible
 * - BLOCK_MARKER: `isVisible` controls the visibility. FALSE - invisible, TRUE - always visible
 *
 * MarkerMode must be set manually after calling `setGhost` -- the `unsetGhost` will not reset the field.
 */
class BlockMarkerActor : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT()), NoSerialise {

    enum class MarkerMode {
        FIXTURE_GHOST, BLOCK_MARKER, HIDDEN
    }

    private val defaultSize = 16.0

    override var referenceID: ActorID = 2147483647 // custom refID
    override val hitbox = Hitbox(0.0, 0.0, TILE_SIZED, TILE_SIZED)

    var markerColour = Color.YELLOW
    var markerShape = 0

    private val blockMarkings: TextureRegionPack
        get() = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common")

    private var ghost: SpriteAnimation? = null
    var markerMode: MarkerMode = MarkerMode.FIXTURE_GHOST
    var ghostColour = Color.WHITE


    init {
        this.isVisible = true
        renderOrder = Actor.RenderOrder.OVERLAY // for some reason the constructor didn't work
    }


    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        this.isVisible = true

        if (isVisible) {
            if (markerMode == MarkerMode.FIXTURE_GHOST) {
                if (INGAME.actorNowPlaying != null) {
//                    mouseInInteractableRange(INGAME.actorNowPlaying!!) { _, _, _, _ ->

                        batch.shader = App.shaderGhastlyWhite
                        if (ghost != null) {
//                            batch.color = ghostColour
                            batch.shader.setUniformf("ghostColour", ghostColour.r, ghostColour.g, ghostColour.b, ghostColour.a)
                            drawSpriteInGoodPosition(frameDelta, ghost!!, batch)
                        }
//                        0L
//                    }
                }
                else {
                    batch.shader = App.shaderGhastlyWhite
                    if (ghost != null) {
//                        batch.color = ghostColour
                        batch.shader.setUniformf("ghostColour", ghostColour.r, ghostColour.g, ghostColour.b, ghostColour.a)
                        drawSpriteInGoodPosition(frameDelta, ghost!!, batch)
                    }
                }
            }
            else if (markerMode == MarkerMode.BLOCK_MARKER) {
                batch.shader = null
                batch.color = markerColour
                batch.draw(blockMarkings.get(markerShape, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
            }

        }

        batch.shader = null
        batch.color = Color.WHITE
    }

    override fun drawGlow(frameDelta: Float, batch: SpriteBatch) {
        batch.color = Color.WHITE
    }

    override fun dispose() {
    }

    override fun updateImpl(delta: Float) {
        if (isVisible) {
            val hbx = (Terrarum.mouseTileX - floor((hitbox.width - 0.5).div(2) / TILE_SIZED)) * TILE_SIZED
            val hby = (Terrarum.mouseTileY - floor((hitbox.height - 0.5) / TILE_SIZED)) * TILE_SIZED
            hitbox.setPosition(hbx, hby)
        }
    }

    override fun onActorValueChange(key: String, value: Any?) { }


    fun setGhost(actor: ActorWithBody) {
        ghost = actor.sprite
        markerMode = MarkerMode.FIXTURE_GHOST
        hitbox.setDimension(actor.baseHitboxW.toDouble(), actor.baseHitboxH.toDouble())
    }

    fun unsetGhost() {
        ghost = null
        setGhostColourNone()
        hitbox.setDimension(TILE_SIZED, TILE_SIZED)
        markerMode = MarkerMode.HIDDEN
    }

    fun hideMarker() {
        unsetGhost()
    }

    fun showMarker(shape: Int) {
        markerShape = shape
        markerMode = MarkerMode.BLOCK_MARKER
    }

    fun setGhostColourNone() { ghostColour = Color.WHITE }
    fun setGhostColourAllow() { ghostColour = Color(-1) }
    fun setGhostColourDeny() { ghostColour = Color(0xff8080ff.toInt()) }
    fun setGhostColourBlock() { ghostColour = Color(0) }
}