package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.colourutil.darkerLab
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2017-03-03.
 */
class UIVitalMetre(
        var player: ActorHumanoid,
        var vitalGetterVal: () -> Float?,
        var vitalGetterMax: () -> Float?,
        var color: Color?,
        val order: Int
) : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

    init {
        // semitransparent
        color?.a = 0.91f
    }

    private val margin = 25
    private val gap = 4f

    override var width: Int = 80 + 2 * margin; set(value) { throw Error("operation not permitted") }
    override var height: Int; get() = player.baseHitboxH; set(value) { throw Error("operation not permitted") }

    override var openCloseTime: Second = 0.05f

    //private val relativePX = width / 2f
    private val offsetY: Float; get() = player.baseHitboxH * 1.5f
    private val circleRadius: Float; get() = player.baseHitboxH * 3f

    private val theta = 33f
    private val halfTheta = theta / 2f

    private val backColor: Color
        get(): Color {
            val c = (color?.darkerLab(0.33f) ?: Color.BLACK)
            c.a = 0.7f
            return c
        }

    override fun updateImpl(delta: Float) {
        handler.setPosition(
                App.scr.halfw,
                App.scr.halfh
        )
    }

    /**
     * g must be same as World Graphics!
     */
    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // TODO now that we just can't draw arcs, we need to re-think about this
        
        /*if (vitalGetterVal() != null && vitalGetterMax() != null && player != null) {
            g.translate(
                    INGAME.screenZoom * (player.centrePosPoint.x.toFloat() - (WorldCamera.x)),
                    INGAME.screenZoom * (player.centrePosPoint.y.toFloat() - (WorldCamera.y))
            )


            g.lineWidth = 2f


            val ratio = min(1f, vitalGetterVal()!! / vitalGetterMax()!!)

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

    override fun doOpening(delta: Float) {
        doOpeningFade(this, openCloseTime)
    }

    override fun doClosing(delta: Float) {
        doClosingFade(this, openCloseTime)
    }

    override fun endOpening(delta: Float) {
        endOpeningFade(this)
    }

    override fun endClosing(delta: Float) {
        endClosingFade(this)
    }

    override fun dispose() {
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