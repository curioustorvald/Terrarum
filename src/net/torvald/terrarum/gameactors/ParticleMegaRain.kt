package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.random.HQRNG
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2017-12-18.
 */
class ParticleMegaRain(posX: Double, posY: Double) : ParticleBase(Actor.RenderOrder.BEHIND, true, 3.2f) {

    init {
        body = MegaRainGovernor.get()
        val w = body.regionWidth.toDouble()
        val h = body.regionHeight.toDouble()
        hitbox.setFromWidthHeight(
                posX - w.times(0.5),
                posY - h.times(0.5),
                w, h
        )

        velocity.y = 11.5 * ActorWithPhysics.SI_TO_GAME_VEL
    }

}

object MegaRainGovernor {

    private var reseedTimer = 0f
    var reseedTime: Second = 90f

    private val body = Pixmap(ModMgr.getGdxFile("basegame", "weathers/raindrop.tga"))
    private lateinit var bodies: Array<TextureRegion>

    private var withdrawCounter = 0

    init {
        seed()
    }

    private fun seed() {
        val w = body.width
        val h = body.height

        bodies = Array(1024) {
            //val pixmap = Pixmap(Terrarum.WIDTH * 2, Terrarum.HEIGHT / 4, Pixmap.Format.RGBA8888)
            val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)

            val rng = HQRNG()

            repeat(rng.nextInt(2) + 3) { // 3 or 4
                val rndX = rng.nextInt(pixmap.width - body.width)
                val rndY = rng.nextInt(pixmap.height - body.height)

                pixmap.drawPixmap(body, rndX, rndY)
            }

            // return composed (mega)pixmap
            val region = TextureRegion(Texture(pixmap))
            region.flip(false, true)

            /*return*/region
        }

        // randomise
        bodies.shuffle()
    }

    fun get(): TextureRegion {
        if (withdrawCounter >= bodies.size) {
            withdrawCounter = 0
            //bodies.shuffle() // if pre-rendered random set is sufficiently large, it'd look random enough
        }

        return bodies[withdrawCounter++]
    }

    @Deprecated("re-seeding freezes the game a little and large enough randomnesses ought to be good")
    fun update(delta: Float) {
        if (reseedTimer >= reseedTime) {
            seed()
            reseedTimer -= reseedTime
        }

        reseedTimer += delta
    }

    fun resize() {
        seed()
        withdrawCounter = 0
        reseedTimer = 0f
    }



    fun Array<TextureRegion>.shuffle() {
        for (i in this.size - 1 downTo 1) {
            val rndIndex = (Math.random() * (i + 1)).toInt()

            val t = this[rndIndex]
            this[rndIndex] = this[i]
            this[i] = t
        }
    }
}