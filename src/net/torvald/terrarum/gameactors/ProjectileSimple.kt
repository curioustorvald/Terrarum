package net.torvald.terrarum.gameactors

import net.torvald.colourutil.CIELabUtil.darkerLab
import net.torvald.point.Point2d
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.Terrarum
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
        private val type: Int,
        fromPoint: Vector2, // projected coord
        toPoint: Vector2    // arriving coord
        ) : ActorWithBody(), Luminous, Projectile {

    val damage: Int
    val displayColour: Color
    /** scalar part of velocity */
    val speed: Int


    override var luminosity: Int
        get() = bulletDatabase[type][OFFSET_LUMINOSITY] as Int
        set(value) {
        }
    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList = ArrayList<Hitbox>()

    val lifetimeMax = 2500
    var lifetimeCounter = 0

    val posPre: Point2d

    init {
        setPosition(fromPoint.x, fromPoint.y)
        posPre = Point2d(fromPoint.x, fromPoint.y)
        // lightbox sized 8x8 centered to the bullet
        lightBoxList.add(Hitbox(-4.0, -4.0, 8.0, 8.0))
        this.velocity.set(velocity)

        damage = bulletDatabase[type][OFFSET_DAMAGE] as Int
        displayColour = bulletDatabase[type][OFFSET_COL] as Color
        isNoSubjectToGrav = bulletDatabase[type][OFFSET_NOGRAVITY] as Boolean
        speed = bulletDatabase[type][OFFSET_SPEED] as Int

        setHitboxDimension(2, 2, 0, 0) // should be following sprite's properties if there IS one


        velocity.set((fromPoint to toPoint).setMagnitude(speed.toDouble()))



        collisionType = KINEMATIC
    }

    override fun update(gc: GameContainer, delta: Int) {
        // hit something and despawn
        lifetimeCounter += delta
        if ((ccdCollided || grounded) || lifetimeCounter >= lifetimeMax) flagDespawn()

        posPre.set(centrePosPoint)

        super.update(gc, delta)
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        val colourTail = displayColour.darker(0f) // clone a colour
        colourTail.a = 0.16f

        // draw trail of solid colour (Terraria style maybe?)
        g.lineWidth = 2f * Terrarum.ingame.screenZoom
        g.drawGradientLine(
                hitbox.centeredX.toFloat() * Terrarum.ingame.screenZoom,
                hitbox.centeredY.toFloat() * Terrarum.ingame.screenZoom,
                displayColour,
                posPre.x.toFloat() * Terrarum.ingame.screenZoom,
                posPre.y.toFloat() * Terrarum.ingame.screenZoom,
                colourTail
        )
    }

    override fun drawGlow(gc: GameContainer, g: Graphics) = drawBody(gc, g)

    companion object {
        val OFFSET_DAMAGE = 0
        val OFFSET_COL = 1 // Color or SpriteAnimation
        val OFFSET_NOGRAVITY = 2
        val OFFSET_SPEED = 3
        val OFFSET_LUMINOSITY = 4
        val bulletDatabase = arrayOf(
                // damage, display colour, no gravity, speed
                arrayOf(7, Color(0xFF5429), true, 40, 32),
                arrayOf(8, Color(0xFF5429), true, 20, 0)
                // ...
        )
    }

}