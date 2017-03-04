package net.torvald.terrarum.ui

import net.torvald.colourutil.CIELabUtil.darkerLab
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.times
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by SKYHi14 on 2017-03-03.
 */
class UIVitalMetre(
        var player: ActorHumanoid,
        val vitalGetterVal: () -> Float,
        val vitalGetterMax: () -> Float,
        val color: Color,
        val order: Int
) : UICanvas {

    private val margin = 25
    private val gap = 4f

    override var width: Int = 80 + 2 * margin
    override var height: Int = player.baseHitboxH * 3 + margin
    override var handler: UIHandler? = null
    override var openCloseTime: Int = 50

    private val relativePX = width / 2f
    private val relativePY = player.baseHitboxH * 1.5f
    private val circleRadius = player.baseHitboxH * 3f

    private val theta = 33f
    private val halfTheta = theta / 2f

    private val backColor = color.darkerLab(0.3f)


    override fun update(gc: GameContainer, delta: Int) {
        handler!!.setPosition(
                (Terrarum.HALFW - relativePX).roundInt(),
                (Terrarum.HALFH - relativePY).floorInt()
        )
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.lineWidth = 2f

        // background
        g.color = backColor
        g.drawArc(
                relativePX - circleRadius - order * gap,
                -circleRadius             - order * gap,
                circleRadius * 2f         + order * gap * 2,
                circleRadius * 2f         + order * gap * 2,
                90f - halfTheta,
                90f + halfTheta - theta * (vitalGetterVal() / vitalGetterMax())
        )

        g.color = color
        g.drawArc(
                relativePX - circleRadius - order * gap,
                -circleRadius             - order * gap,
                circleRadius * 2f         + order * gap * 2,
                circleRadius * 2f         + order * gap * 2,
                90f + halfTheta - theta * (vitalGetterVal() / vitalGetterMax()),
                90f + halfTheta
        )
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        UICanvas.doOpeningFade(handler, openCloseTime)
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        UICanvas.doClosingFade(handler, openCloseTime)
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        UICanvas.endOpeningFade(handler)
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        UICanvas.endClosingFade(handler)
    }
}

/*

X-------------+ (84)
|             |
|             |
|      @      |
|      @      |
|,           ,|
| ''-------'' |
+-------------+

X: UICanvas position

 */