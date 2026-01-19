package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Minimal anchor actor for world updates.
 *
 * This actor has no visible representation and does not participate in physics.
 * It serves purely as a point around which world simulation (fluids, wires, tile updates)
 * remains active.
 *
 * Created by minjaesong on 2026-01-19.
 */
class WorldUpdateAnchor : ActorWithBody, WorldUpdater, NoSerialise {

    constructor() : super(RenderOrder.MIDDLE, PhysProperties.IMMOBILE()) {
        isVisible = false
        chunkAnchoring = true
        setHitboxDimension(1, 1, 0, 0)
    }

    constructor(id: ActorID) : super(RenderOrder.MIDDLE, PhysProperties.IMMOBILE(), id) {
        isVisible = false
        chunkAnchoring = true
        setHitboxDimension(1, 1, 0, 0)
    }

    override fun updateImpl(delta: Float) {
        // No-op; this actor exists solely as a world update anchor
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        // No-op; this actor is invisible
    }

    override fun drawGlow(frameDelta: Float, batch: SpriteBatch) {
        // No-op; this actor is invisible
    }

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        // No-op; this actor is invisible
    }

    override fun dispose() {
        // Nothing to dispose
    }
}
