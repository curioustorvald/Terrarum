package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Actor with visible body
 *
 * Created by minjaesong on 2017-01-21.
 */
abstract class ActorWithBody(renderOrder: RenderOrder) : Actor(renderOrder) {
    abstract val hitbox: Hitbox
    abstract fun drawBody(batch: SpriteBatch)
    abstract fun drawGlow(batch: SpriteBatch)
    open var tooltipText: String? = null // null: display nothing
}