package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.colourutil.CIELabUtil.darkerLab
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.Second

/**
 * Created by SKYHi14 on 2017-03-03.
 */
class UIVitalMetre(
        var player: ActorHumanoid?,
        var vitalGetterVal: () -> Float?,
        var vitalGetterMax: () -> Float?,
        var color: Color?,
        val order: Int
) : UICanvas {

    init {
        // semitransparent
        color?.a = 0.91f
    }

    private val margin = 25
    private val gap = 4f

    override var width: Int = 80 + 2 * margin; set(value) { throw Error("operation not permitted") }
    override var height: Int; get() = player?.baseHitboxH ?: 0 * 3 + margin; set(value) { throw Error("operation not permitted") }
    override var handler: UIHandler? = null
        set(value) {
            // override customPositioning to be true
            if (value != null) {
                value.customPositioning = true
            }
            field = value
        }
    override var openCloseTime: Second = 0.05f

    //private val relativePX = width / 2f
    private val offsetY: Float; get() = (player?.baseHitboxH ?: 0) * 1.5f
    private val circleRadius: Float; get() = (player?.baseHitboxH ?: 0) * 3f

    private val theta = 33f
    private val halfTheta = theta / 2f

    private val backColor: Color
        get(): Color {
            val c = (color?.darkerLab(0.33f) ?: Color.BLACK)
            c.a = 0.7f
            return c
        }

    override fun update(delta: Float) {
        handler!!.setPosition(
                Terrarum.HALFW,
                Terrarum.HALFH
        )
    }

    /**
     * g must be same as World Graphics!
     */
    override fun render(batch: SpriteBatch) {
        // TODO now that we just can't draw arcs, we need to re-think about this
        
        /*if (vitalGetterVal() != null && vitalGetterMax() != null && player != null) {
            g.translate(
                    Terrarum.ingame!!.screenZoom * (player!!.centrePosPoint.x.toFloat() - (WorldCamera.x)),
                    Terrarum.ingame!!.screenZoom * (player!!.centrePosPoint.y.toFloat() - (WorldCamera.y))
            )


            g.lineWidth = 2f


            val ratio = minOf(1f, vitalGetterVal()!! / vitalGetterMax()!!)

            // background
            g.color = backColor
            g.drawArc(
                    -circleRadius - order * gap,
                    -circleRadius - order * gap - offsetY,
                    circleRadius * 2f + order * gap * 2,
                    circleRadius * 2f + order * gap * 2,
                    90f - halfTheta,
                    90f + halfTheta - theta * ratio
            )

            g.color = color
            g.drawArc(
                    -circleRadius - order * gap,
                    -circleRadius - order * gap - offsetY,
                    circleRadius * 2f + order * gap * 2,
                    circleRadius * 2f + order * gap * 2,
                    90f + halfTheta - theta * ratio,
                    90f + halfTheta
            )



            g.flush()
        }*/
    }

    override fun processInput(delta: Float) {
    }

    override fun doOpening(delta: Float) {
        UICanvas.doOpeningFade(handler, openCloseTime)
    }

    override fun doClosing(delta: Float) {
        UICanvas.doClosingFade(handler, openCloseTime)
    }

    override fun endOpening(delta: Float) {
        UICanvas.endOpeningFade(handler)
    }

    override fun endClosing(delta: Float) {
        UICanvas.endClosingFade(handler)
    }
}

fun Float.abs() = FastMath.abs(this)

/*

+-------------+ (84)
|             |
|             |
|      X      |
|      @      |
|,           ,|
| ''-------'' |
+-------------+

X: UICanvas position

 */