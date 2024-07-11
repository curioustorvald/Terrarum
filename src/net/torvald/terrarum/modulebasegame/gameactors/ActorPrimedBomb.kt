package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SingleImageSprite
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.modulebasegame.ExplosionManager
import java.util.ArrayList
import kotlin.math.log10

/**
 * Created by minjaesong on 2024-02-13.
 */
open class ActorPrimedBomb(
    @Transient private var explosionPower: Float = 1f,
    private var fuse: Second = 1f,
    @Transient private var dropProbNonOre: Float = 0.25f,
    @Transient private var dropProbOre: Float = 0.75f
) : ActorWithBody() {

    init {
        renderOrder = RenderOrder.MIDTOP
        physProp = PhysProperties.PHYSICS_OBJECT()
        elasticity = 0.34
    }

    protected constructor() : this(1f, 1f) {
        renderOrder = RenderOrder.MIDTOP
        physProp = PhysProperties.PHYSICS_OBJECT()
    }

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

    fun updatePhysOnly(delta: Float) {
        super.updateImpl(delta)
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
class ActorCherryBomb : ActorPrimedBomb(14f, 4.5f) { // 14 is the intended value; 32 is for testing
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
class ActorGlowOrb : ActorPrimedBomb(0f, 0f) { // 14 is the intended value; 32 is for testing
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
        updatePhysOnly(delta)

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
        // remove the actor some time AFTER the chemicals are exhausted
        if (timeDelta0 >= 10 * lifePower) {
            flagDespawn()
        }
    }
}