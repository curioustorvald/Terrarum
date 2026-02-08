package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE

/**
 * A horizontal moving platform that carries actors standing on top of it.
 *
 * Subclasses must set [contraptionVelocity] before calling `super.updateImpl(delta)`.
 *
 * Created by minjaesong on 2022-02-28.
 */
open class ActorMovingPlatform() : PhysContraption() {

    protected var tilewiseWidth = 3 // default fallback value when no args were given

    constructor(newTilewiseWidth: Int) : this() {
        this.tilewiseWidth = newTilewiseWidth
        setHitboxDimension(TILE_SIZE * newTilewiseWidth, TILE_SIZE, 0, 0)
    }

    @Transient private var platformTexture: Texture? = null
    @Transient private var platformTextureRegion: TextureRegion? = null

    init {
        setHitboxDimension(TILE_SIZE * tilewiseWidth, TILE_SIZE, 0, 0)
    }

    private fun ensureTexture() {
        if (platformTexture == null) {
            val w = TILE_SIZE * tilewiseWidth
            val h = TILE_SIZE
            val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
            // grey-blue colour
            pixmap.setColor(Color(0.45f, 0.55f, 0.65f, 1f))
            pixmap.fill()
            // slightly darker border
            pixmap.setColor(Color(0.35f, 0.45f, 0.55f, 1f))
            pixmap.drawRectangle(0, 0, w, h)
            platformTexture = Texture(pixmap)
            platformTextureRegion = TextureRegion(platformTexture)
            pixmap.dispose()
        }
    }

    override fun isActorOnTop(actor: ActorWithBody): Boolean {
        val feetY = actor.hitbox.endY
        val headY = actor.hitbox.startY
        val platTop = hitbox.startY

        // Feet are near platform top: slightly above or sunk partway in
        val feetNearPlatTop = feetY >= platTop - MOUNT_TOLERANCE_ABOVE &&
                              feetY <= platTop + MOUNT_TOLERANCE_BELOW

        // Actor's head must be above platform top (prevents mounting from below)
        val comingFromAbove = headY < platTop

        // Check horizontal overlap
        val horizontalOverlap = actor.hitbox.endX > hitbox.startX && actor.hitbox.startX < hitbox.endX

        // Check not jumping upward
        val combinedVelY = actor.externalV.y + (actor.controllerV?.y ?: 0.0)
        val notJumping = combinedVelY >= JUMP_THRESHOLD_Y

        return feetNearPlatTop && comingFromAbove && horizontalOverlap && notJumping
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible) {
            ensureTexture()
            platformTextureRegion?.let {
                drawTextureInGoodPosition(frameDelta, it, batch)
            }
        }
    }

    override fun dispose() {
        platformTexture?.dispose()
        platformTexture = null
        platformTextureRegion = null
        super.dispose()
    }
}
