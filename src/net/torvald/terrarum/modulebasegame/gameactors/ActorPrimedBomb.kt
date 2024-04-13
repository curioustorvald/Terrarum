package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import net.torvald.spriteanimation.SingleImageSprite
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.modulebasegame.ExplosionManager

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
        "boom", ModMgr.getFile("basegame", "audio/effects/explosion/bang_bomb.ogg"), toRAM = true
    ) {
        this.flagDespawn()
    }
    @Transient private val fuseSound = MusicContainer(
        "fuse", ModMgr.getFile("basegame", "audio/effects/explosion/fuse.ogg"), toRAM = true
    ) {
        this.flagDespawn()
    }
    @Transient private val fuseSoundCont = MusicContainer(
        "fuse_continue", ModMgr.getFile("basegame", "audio/effects/explosion/fuse_continue.ogg"), toRAM = true
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
class ActorCherryBomb : ActorPrimedBomb(14f, 4.5f) { // 14 is the intended value; 32 is for testing

    init {
        val itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,13)

        setHitboxDimension(7, 7, 2, -2)
        sprite = SingleImageSprite(this, itemImage)

        avBaseMass = 1.0
        density = 1400.0
    }



}