package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.Second
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.tav.AudioBankTav
import net.torvald.terrarum.tav.TavDecoder
import java.io.InputStream

/**
 * A SpriteAnimation that plays a TAV video file.
 *
 * Usage:
 *   val anim = VideoSpriteAnimation(actor, stream, looping = true)
 *   actor.sprite = anim
 *   anim.start()
 *   // Optionally route audio:
 *   anim.audioBank?.let { bank ->
 *       val track = App.audioMixer.getFreeTrackNoMatterWhat()
 *       track.currentTrack = bank
 *       track.play()
 *   }
 */
class VideoSpriteAnimation(
    parentActor: ActorWithBody,
    tavStream: InputStream,
    val looping: Boolean = false
) : SpriteAnimation(parentActor) {

    val decoder = TavDecoder(tavStream, looping)
    val audioBank: AudioBankTav? = if (decoder.hasAudio) AudioBankTav(decoder) else null

    val cellWidth:  Int get() = decoder.videoWidth
    val cellHeight: Int get() = decoder.videoHeight

    override val currentDelay: Second get() = 1f / decoder.fps.coerceAtLeast(1)

    private var currentTexture: Texture? = null
    private var deltaAccumulator = 0f
    private var started = false

    val isFinished: Boolean get() = decoder.isFinished.get()

    fun start() {
        decoder.start()
        started = true
    }

    fun stop() {
        decoder.stop()
        started = false
    }

    // update() is a no-op: frame timing is handled in render() via frameDelta
    override fun update(delta: Float) {}

    override fun render(
        frameDelta: Float,
        batch: SpriteBatch,
        posX: Float,
        posY: Float,
        scale: Float,
        mode: Int,
        forcedColourFilter: Color?
    ) {
        if (!started) return

        // Advance frame timing
        deltaAccumulator += frameDelta
        while (deltaAccumulator >= currentDelay) {
            decoder.advanceFrame()
            deltaAccumulator -= currentDelay
        }

        val pixmap = decoder.getFramePixmap() ?: return

        // Dispose old texture and create new one from the current decoded Pixmap
        currentTexture?.dispose()
        currentTexture = Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }

        batch.color = forcedColourFilter ?: colourFilter

        val w = cellWidth
        val h = cellHeight

        val tx  = (parentActor.hitboxTranslateX) * scale
        val txF = (parentActor.hitboxTranslateX + parentActor.baseHitboxW) * scale
        val ty  = (parentActor.hitboxTranslateY + (h - parentActor.baseHitboxH)) * scale
        val tyF = (parentActor.hitboxTranslateY + parentActor.baseHitboxH) * scale

        val tex = currentTexture!!
        val x0 = FastMath.floor(posX).toFloat()
        val y0 = FastMath.floor(posY).toFloat()
        val fw = FastMath.floor(w * scale).toFloat()
        val fh = FastMath.floor(h * scale).toFloat()

        if (flipHorizontal && flipVertical) {
            batch.draw(tex, x0 + txF, y0 + tyF, -fw, -fh)
        } else if (flipHorizontal && !flipVertical) {
            batch.draw(tex, x0 + txF, y0 - ty, -fw, fh)
        } else if (!flipHorizontal && flipVertical) {
            batch.draw(tex, x0 - tx, y0 + tyF, fw, -fh)
        } else {
            batch.draw(tex, x0 - tx, y0 - ty, fw, fh)
        }
    }

    override fun dispose() {
        stop()
        decoder.dispose()
        currentTexture?.dispose()
        currentTexture = null
    }
}
