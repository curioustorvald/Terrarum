package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Color
import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SingleImageSprite
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.modulebasegame.ExplosionManager
import kotlin.math.log10

/**
 * Created by minjaesong on 2024-07-12.
 */
open class ActorLobbed(throwPitch: Float) : ActorWithBody() {

    protected constructor() : this(1f)

    @Transient private val whooshSound = MusicContainer(
        "throw_low_short", ModMgr.getFile("basegame", "audio/effects/throwing/throw_low_short.wav"),
        toRAM = true,
        samplingRateOverride = 48000f * throwPitch.coerceIn(0.5f, 2f)
    )

    init {
        renderOrder = RenderOrder.FRONT
        physProp = PhysProperties.PHYSICS_OBJECT()
        elasticity = 0.34
    }

    private var soundFired = false

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)
        if (!soundFired) {
            soundFired = true
            startAudio(whooshSound, 1.0)
        }
    }
}


/**
 * Created by minjaesong on 2024-02-13.
 */
open class ActorPrimedBomb(
    throwPitch: Float,
    @Transient private var explosionPower: Float = 1f,
    private var fuse: Second = 1f,
    @Transient private var dropProbNonOre: Float = 0.25f,
    @Transient private var dropProbOre: Float = 0.75f
) : ActorLobbed(throwPitch) {

    protected constructor() : this(1f, 1f, 1f)

    private var explosionCalled = false

    @Transient private val boomSound = MusicContainer(
        "boom", ModMgr.getFile("basegame", "audio/effects/explosion/bang_bomb.wav"), toRAM = true
    ) {
        this.flagDespawn()
    }
    @Transient private val fuseSound = MusicContainer(
        "fuse", ModMgr.getFile("basegame", "audio/effects/explosion/fuse.wav"), toRAM = true
    ) {
        this.flagDespawn()
    }
    @Transient private val fuseSoundCont = MusicContainer(
        "fuse_continue", ModMgr.getFile("basegame", "audio/effects/explosion/fuse_continue.wav"), toRAM = true
    ) {
        this.flagDespawn()
    }

    private var fuseSoundStatus = 0 // this value must be stored into the savegame
    @Transient private var fuseSoundFired = false

    override val stopMusicOnDespawn: Boolean
        get() = this.isVisible

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        if (!fuseSoundFired && fuse > 0f) {
            fuseSoundFired = true
            if (fuseSoundStatus == 0) {
                startAudio(fuseSound, 2.0)
                fuseSoundStatus = 1
            }
            else
                startAudio(fuseSoundCont, 2.0)
        }

        fuse -= delta

        if (fuse <= 0f && !explosionCalled) {
            explosionCalled = true

            ExplosionManager.goBoom(
                INGAME.world,
                hitbox.centeredX.div(TILE_SIZED).minus(1.0).toInt(),
                hitbox.startY.div(TILE_SIZED).minus(1.0).toInt(),
                explosionPower,
                dropProbNonOre,
                dropProbOre
            ) {
                physProp.usePhysics = false
                this.isVisible = false // or play explosion anim
                stopAudio(fuseSound)
                startAudio(boomSound, 10.0)
            }
        }
    }

    override fun dispose() {
        super.dispose()
        boomSound.dispose()
        fuseSound.dispose()
        fuseSoundCont.dispose()
    }
}


/**
 * Created by minjaesong on 2024-02-14.
 */
class ActorCherryBomb(throwPitch: Float) : ActorPrimedBomb(throwPitch, 14f, 4.5f) { // 14 is the intended value; 32 is for testing

    private constructor() : this(1f)

    init {
        val itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,13)

        setHitboxDimension(7, 7, 2, -2)
        sprite = SingleImageSprite(this, itemImage)

        avBaseMass = 1.0
        density = 1400.0
    }
}


/**
 * Created by minjaesong on 2024-07-12.
 */
class ActorGlowOrb(throwPitch: Float) : ActorLobbed(throwPitch) {

    private constructor() : this(1f)

    val spawnTime = INGAME.world.worldTime.TIME_T

    init {
        val itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(1,13)

        setHitboxDimension(7, 7, 2, -2)
        sprite = SingleImageSprite(this, itemImage)
        spriteEmissive = SingleImageSprite(this, itemImage)

        avBaseMass = 1.0
        density = 1400.0
    }

    @Transient private val lifePower = 10000L // charge reaches 0 on timeDelta = 9 * lifePower
    @Transient private val lumMult = 0.8f
    @Transient private val lumCol = BlockCodex["basegame:215"]

    override var lightBoxList = arrayListOf(Lightbox(Hitbox(1.0, 1.0, baseHitboxW - 2.0, baseHitboxH - 2.0), Cvec(0)))


    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        val timeDelta0 = INGAME.world.worldTime.TIME_T - spawnTime
        val timeDelta = timeDelta0.coerceIn(0, 9 * lifePower)
        val charge = log10((-timeDelta + 10 * lifePower.toFloat()) / lifePower.toFloat())

        // set colours
        spriteEmissive!!.colourFilter = Color(charge, charge, charge, 1f)
        lightBoxList[0].light.set(
            lumCol.baseLumColR * charge * lumMult,
            lumCol.baseLumColG * charge * lumMult,
            lumCol.baseLumColB * charge * lumMult,
            lumCol.baseLumColA * charge * lumMult,
        )
        // remove the actor some time AFTER the chemicals have exhausted
        if (timeDelta0 >= 10 * lifePower) {
            flagDespawn()
        }
    }
}