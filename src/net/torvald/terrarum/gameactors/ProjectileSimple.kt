package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ActorWithBody
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Created by minjaesong on 16-08-29.
 */
open class ProjectileSimple(
        type: Int,
        position: Vector2,
        velocity: Vector2,
        override var luminosity: Int = 0) : ActorWithBody(), Luminous {

    val damage: Int
    val displayColour: Color

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList = ArrayList<Hitbox>()

    init {
        hitbox.set(position.x, position.y, 2.0, 2.0) // 2.0: size of the hitbox in pixels
        lightBoxList.add(Hitbox(0.0, 0.0, 2.0, 2.0))
        this.velocity.set(velocity)

        damage = bulletDatabase[type][0] as Int
        displayColour = bulletDatabase[type][1] as Color
    }

    override fun update(gc: GameContainer, delta: Int) {
        // hit something and despawn! (use ```flagDespawn = true```)


        super.update(gc, delta)
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        // draw trail of solid colour (Terraria style maybe?)

    }

    companion object {
        val TYPE_BULLET_BASIC = 0

        val bulletDatabase = arrayOf(
                arrayOf(7, Color(0xFF5429)),
                arrayOf(8, Color(0xFF5429))
                // ...
        )
    }

}