package net.torvald.terrarum.gameactors

import net.torvald.colourutil.CIELabUtil.brighterLab
import net.torvald.spriteanimation.SpriteAnimation
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Simplest projectile.
 *
 * Created by minjaesong on 16-08-29.
 */
open class ProjectileSimple(
        type: Int,
        fromPoint: Vector2, // projected coord
        toPoint: Vector2, // arriving coord
        override var luminosity: Int = 0) : ActorWithBody(), Luminous {

    val damage: Int
    val displayColour: Color
    /** scalar part of velocity */
    val speed: Int

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList = ArrayList<Hitbox>()

    init {
        hitbox.set(fromPoint.x, fromPoint.y, 2.0, 2.0) // 2.0: size of the hitbox in pixels
        // lightbox sized 8x8 centered to the bullet
        lightBoxList.add(Hitbox(-4.0, -4.0, 8.0, 8.0))
        this.velocity.set(velocity)

        damage = bulletDatabase[type][OFFSET_DAMAGE] as Int
        displayColour = bulletDatabase[type][OFFSET_COL] as Color
        isNoSubjectToGrav = bulletDatabase[type][OFFSET_NOGRAVITY] as Boolean
        speed = bulletDatabase[type][OFFSET_SPEED] as Int

        if (displayColour == Color(254, 0, 0, 0)) {
            sprite = bulletDatabase[type][OFFSET_SPRITE] as SpriteAnimation
        }



        val initVelo = Vector2(speed.toDouble(), 0.0)
        initVelo.setDirection((fromPoint to toPoint).direction)

        velocity.set(initVelo)



        collisionType = KINEMATIC
    }

    override fun update(gc: GameContainer, delta: Int) {
        // hit something and despawn
        if (ccdCollided || grounded) flagDespawn()

        super.update(gc, delta)
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        // draw trail of solid colour (Terraria style maybe?)
        g.lineWidth = 3f
        g.drawGradientLine(
                nextHitbox.centeredX.toFloat(),
                nextHitbox.centeredY.toFloat(),
                displayColour,
                hitbox.centeredX.toFloat(),
                hitbox.centeredY.toFloat(),
                displayColour.brighterLab(0.8f)
        )
    }

    companion object {
        val OFFSET_DAMAGE = 0
        val OFFSET_COL = 1 // set it to Color(254, 0, 0, 0) to use sprite
        val OFFSET_NOGRAVITY = 2
        val OFFSET_SPEED = 3
        val OFFSET_SPRITE = 4
        val bulletDatabase = arrayOf(
                // damage, display colour, no gravity, speed
                arrayOf(7, Color(0xFF5429), true, 50),
                arrayOf(8, Color(0xFF5429), true, 50)
                // ...
        )
    }

}