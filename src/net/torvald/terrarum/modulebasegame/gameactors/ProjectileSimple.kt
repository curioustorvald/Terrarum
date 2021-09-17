package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.Point2d
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameactors.PhysProperties
import org.dyn4j.geometry.Vector2
import java.util.*
import net.torvald.terrarum.*

/**
 * Simplest projectile.
 *
 * Created by minjaesong on 2016-08-29.
 */

// TODO simplified, lightweight physics (does not call PhysicsSolver)
open class ProjectileSimple : ActorWithBody, Luminous, Projectile {

    private var type: Int = 0
    var damage: Int = 0
    lateinit var displayColour: Color
    /** scalar part of velocity */
    var speed: Int = 0


    override var color: Cvec
        get() = (bulletDatabase[type][OFFSET_LUMINOSITY] as Cvec).cpy()
        set(value) {
        }
    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList = ArrayList<Hitbox>()

    private val lifetimeMax = 2500
    private var lifetimeCounter = 0f

    private lateinit var posPre: Point2d

    protected constructor()

    constructor(type: Int,
                fromPoint: Vector2, // projected coord
                toPoint: Vector2    // arriving coord
             ) : super(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT) {
        this.type = type


        setPosition(fromPoint.x, fromPoint.y)
        posPre = Point2d(fromPoint.x, fromPoint.y)
        // lightbox sized 8x8 centered to the bullet
        lightBoxList.add(Hitbox(-4.0, -4.0, 8.0, 8.0))
        //this.externalV.set(velocity)

        damage = bulletDatabase[type][OFFSET_DAMAGE] as Int
        displayColour = bulletDatabase[type][OFFSET_COL] as Color
        isNoSubjectToGrav = bulletDatabase[type][OFFSET_NOGRAVITY] as Boolean
        speed = bulletDatabase[type][OFFSET_SPEED] as Int

        setHitboxDimension(2, 2, 0, 0) // should be following sprite's properties if there IS one


        externalV.set((fromPoint to toPoint).setMagnitude(speed.toDouble()))



        collisionType = COLLISION_KINEMATIC
    }

    override fun update(delta: Float) {
        // hit something and despawn
        lifetimeCounter += delta
        if (walledTop || walledBottom || walledRight || walledLeft || lifetimeCounter >= lifetimeMax ||
            // stuck check
            BlockCodex[(INGAME.world).getTileFromTerrain(feetPosTile.x, feetPosTile.y) ?: Block.STONE].isSolid
                ) {
            flagDespawn()
        }

        posPre.set(centrePosPoint)

        super.update(delta)
    }

    /**
     * WARNING! ends and begins Batch
     */
    override fun drawBody(batch: SpriteBatch) {
        val colourTail = displayColour.cpy() // clone a colour
        colourTail.a = 0.16f

        /*batch.end()
        Terrarum.inShapeRenderer {
            // draw trail of solid colour (Terraria style maybe?)
            it.lineWidth = 2f * INGAME.screenZoom
            g.drawGradientLine(
                    hitbox.centeredX.toFloat() * INGAME.screenZoom,
                    hitbox.centeredY.toFloat() * INGAME.screenZoom,
                    displayColour,
                    posPre.x.toFloat() * INGAME.screenZoom,
                    posPre.y.toFloat() * INGAME.screenZoom,
                    colourTail
            )
        }
        batch.begin()*/
    }

    override fun drawGlow(batch: SpriteBatch) = drawBody(batch)

    companion object {
        val OFFSET_DAMAGE = 0
        val OFFSET_COL = 1 // Color or SpriteAnimation
        val OFFSET_NOGRAVITY = 2
        val OFFSET_SPEED = 3
        val OFFSET_LUMINOSITY = 4
        val bulletDatabase = arrayOf(
                // damage, display colour, no gravity, speed
                arrayOf(7, Cvec(0xFF5429_FF.toInt()), true, 40, 32),
                arrayOf(8, Cvec(0xFF5429_FF.toInt()), true, 20, 0)
                // ...
        )
    }

}