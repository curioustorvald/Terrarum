package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import net.torvald.spriteanimation.SingleImageSprite
import net.torvald.terrarum.*
import net.torvald.terrarum.audio.MusicContainer
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.modulebasegame.ExplosionManager

/**
 * Created by minjaesong on 2024-02-13.
 */
open class ActorPrimedBomb(
    private var explosionPower: Float = 1f,
    private var fuse: Second = 1f,
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
        "boom", ModMgr.getFile("basegame", "audio/effects/explosion/bang_bomb.ogg")
    ) {
        this.flagDespawn()
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        fuse -= delta

        if (fuse <= 0f && !explosionCalled) {
            explosionCalled = true
            physProp.usePhysics = false

            this.isVisible = false // or play explosion anim
            startAudio(boomSound, 10.0)

            ExplosionManager.goBoom(INGAME.world, intTilewiseHitbox.centeredX.toInt(), intTilewiseHitbox.centeredY.toInt(), explosionPower) {


            }
        }
    }

    override fun dispose() {
        super.dispose()
        boomSound.dispose()
    }
}


/**
 * Created by minjaesong on 2024-02-14.
 */
class ActorCherryBomb : ActorPrimedBomb(500f, 4.5f) {

    init {
        val itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(0,13)

        setHitboxDimension(7, 7, 2, -2)
        sprite = SingleImageSprite(this, itemImage)

        avBaseMass = 1.0
        density = 1400.0
    }



}