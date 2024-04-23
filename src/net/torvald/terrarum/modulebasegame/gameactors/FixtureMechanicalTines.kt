package net.torvald.terrarum.modulebasegame.gameactors

import com.jme3.math.FastMath
import com.jme3.math.FastMath.DEG_TO_RAD
import net.torvald.colourutil.OKHsv
import net.torvald.colourutil.toColor
import net.torvald.colourutil.tosRGB
import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameparticles.ParticleVanishingTexture
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.audio.audiobank.AudioBankMusicBox
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-04-15.
 */
class FixtureMechanicalTines : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_MECHANICAL_TINES"] }
    )

    @Transient private val audioBank = AudioBankMusicBox()

    @Transient private val track = App.audioMixer.getFreeTrackNoMatterWhat()

    init {
        track.trackingTarget = this
        track.currentTrack = audioBank
        track.play()


        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mechanical_tines.tga")
        density = 1400.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 2, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 2)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0


        setWireSinkAt(0, 1, "digital_bit")
        setWireSinkAt(1, 1, "network")

        despawnHook = {
            track.stop()
            track.currentTrack = null
            track.trackingTarget = null
            audioBank.dispose()
        }
    }

    @Transient private var testRollCursor = 0

    override fun updateSignal() {
        // TODO update using network port


        if (isSignalHigh(0, 1)) {
            // advance every tick
            testNotes[testRollCursor].let {
                audioBank.sendMessage(it)
                spewParticles(it)
            }
            testRollCursor = (testRollCursor + 1) % testNotes.size
        }
    }

    private fun spewParticles(noteBits: Long) {
        if (noteBits == 0L) return
        val notes = findSetBits(noteBits)
        notes.forEach {
            val particle = ParticleMusicalNoteFactory.makeRandomParticle(it, this.hitbox.canonVec)
            (INGAME as TerrarumIngame).addParticle(particle)
        }
    }


    companion object {
        @Transient val testNotes = PreludeInCshMaj()

        fun findSetBits(num: Long): List<Int> {
            val result = mutableListOf<Int>()
            for (i in 0 until 61) {
                if (num and (1L shl i) != 0L) {
                    result.add(i)
                }
            }
            return result
        }
    }
}

object ParticleMusicalNoteFactory {

    init {
        CommonResourcePool.addToLoadingList("basegame-particles-musical_note") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/musical_note.tga"), 8, 8)
        }
        CommonResourcePool.loadAll()
    }

    private val tex = CommonResourcePool.getAsTextureRegionPack("basegame-particles-musical_note")
    private val rng = HQRNG()

    private const val HALF_PI = 1.5707963267948966

    private const val ANGLE_LEFTMOST = -(HALF_PI - 1.0)
    private const val ANGLE_RIGHTMOST = -(HALF_PI + 1.0)

    private val noteColours = (0..60).map {
        val h = (it / 60f * 360f) * DEG_TO_RAD
        val s = 0.75f
        val v = 1f
        OKHsv(h, s, v).tosRGB().toColor()
    }

    private val angles = (0..60).map {
        FastMath.interpolateLinear(it / 60.0, ANGLE_RIGHTMOST, ANGLE_LEFTMOST)
    }

    fun makeRandomParticle(note: Int, pos: Vector2): ParticleVanishingTexture {
        val it = ParticleVanishingTexture(tex.get(rng.nextInt(3), rng.nextInt(2)), pos.x, pos.y, false)

        // set flying velocity
        val direction = angles[note]
        val magnitude = rng.nextTriangularBal() * 0.2 + 1.0

        it.velocity.set(Vector2.create(magnitude, direction))

        // set particle colour
        it.drawColour.set(noteColours[note])


        return it
    }
}
