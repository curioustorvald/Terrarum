package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Actor with visible body
 *
 * Created by minjaesong on 2017-01-21.
 */
abstract class ActorWithBody(renderOrder: RenderOrder) : Actor(renderOrder) {
    open val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)
    abstract fun drawBody(batch: SpriteBatch)
    abstract fun drawGlow(batch: SpriteBatch)
    open var tooltipText: String? = null // null: display nothing

    abstract fun dispose()
}