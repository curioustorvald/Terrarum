package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Second
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.ExplosionManager
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-02-13.
 */
open class ActorPrimedBomb : ActorWithBody {

    protected constructor() {
        renderOrder = RenderOrder.MIDTOP
    }

    private var explosionPower: Float = 1f
    private var fuse: Second = 1f

    constructor(
        initialPos: Vector2,
        initialVelo: Vector2,
        power: Float,
        fuse: Second
    ) {
        renderOrder = RenderOrder.MIDTOP

        this.explosionPower = power
        this.fuse = fuse
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        fuse -= delta

        if (fuse <= 0f) {
            physProp.usePhysics = false
            ExplosionManager.goBoom(INGAME.world, intTilewiseHitbox.centeredX.toInt(), intTilewiseHitbox.centeredY.toInt(), explosionPower)
            flagDespawn()
        }
    }
}