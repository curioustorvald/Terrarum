package net.torvald.terrarum

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.Terrarum.Companion.STATE_ID_TOOL_NOISEGEN
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * Created by SKYHi14 on 2016-12-21.
 */
class StateNoiseTexGen : BasicGameState() {

    private val imagesize = 512

    private val noiseImage = Image(imagesize, imagesize)

    private val sampleDensity = 4.0

    override fun init(p0: GameContainer?, p1: StateBasedGame?) {
        generateNoiseImage()

        println("Press SPACE to generate new noise")
    }

    // TODO multithreaded

    private fun noiseRidged(): Joise {
        val ridged = ModuleFractal()
        ridged.setType(ModuleFractal.FractalType.RIDGEMULTI)
        ridged.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        ridged.setNumOctaves(4)
        ridged.setFrequency(1.0)
        ridged.seed = Random().nextLong()

        val ridged_autocorrect = ModuleAutoCorrect()
        ridged_autocorrect.setRange(0.0, 1.0)
        ridged_autocorrect.setSource(ridged)

        val ridged_scale = ModuleScaleDomain()
        ridged_scale.setScaleX(1.0)
        ridged_scale.setScaleY(1.0)
        ridged_scale.setSource(ridged_autocorrect)

        return Joise(ridged_scale)
    }

    private fun noiseBlobs(): Joise {
        val gradval = ModuleBasisFunction()
        gradval.seed = Random().nextLong()
        gradval.setType(ModuleBasisFunction.BasisType.GRADVAL)
        gradval.setInterpolation(ModuleBasisFunction.InterpolationType.QUINTIC)

        val gradval_scale = ModuleScaleDomain()
        gradval_scale.setScaleX(1.0)
        gradval_scale.setScaleY(1.0)
        gradval_scale.setSource(gradval)

        return Joise(gradval_scale)
    }

    private fun noiseSimplex(): Joise {
        val simplex = ModuleFractal()
        simplex.seed = Random().nextLong()
        simplex.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
        simplex.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.LINEAR)
        simplex.setNumOctaves(2)
        simplex.setFrequency(1.0)

        val simplex_scale = ModuleScaleDomain()
        simplex_scale.setScaleX(1.0)
        simplex_scale.setScaleY(1.0)
        simplex_scale.setSource(simplex)

        return Joise(simplex_scale)
    }

    private fun noiseCellular(): Joise {
        val cellgen = ModuleCellGen()
        cellgen.seed = Random().nextLong()

        val cellular = ModuleCellular()
        cellular.setCellularSource(cellgen)

        return Joise(cellular)
    }

    fun generateNoiseImage() {
        val noiseModule = noiseCellular()

        noiseImage.graphics.background = Color.black

        for (sy in 0..imagesize - 1) {
            for (sx in 0..imagesize - 1) {
                val y = sy.toDouble() / imagesize
                val x = sx.toDouble() / imagesize

                val sampleOffset = sampleDensity
                // 4-D toroidal sampling (looped H and V)
                val sampleTheta1 = x * Math.PI * 2.0
                val sampleTheta2 = y * Math.PI * 2.0
                val sampleX = Math.sin(sampleTheta1) * sampleDensity + sampleDensity
                val sampleY = Math.cos(sampleTheta1) * sampleDensity + sampleDensity
                val sampleZ = Math.sin(sampleTheta2) * sampleDensity + sampleDensity
                val sampleW = Math.cos(sampleTheta2) * sampleDensity + sampleDensity

                val noise = noiseModule.get(
                        sampleX, sampleY, sampleZ, sampleW
                ).plus(1.0).div(2.0)

                noiseImage.graphics.color = Color(noise.toFloat(), noise.toFloat(), noise.toFloat())
                noiseImage.graphics.fillRect(sx.toFloat(), sy.toFloat(), 1f, 1f)
            }
        }

        noiseImage.graphics.flush()
    }

    override fun update(p0: GameContainer?, p1: StateBasedGame?, p2: Int) {

    }

    override fun getID() = STATE_ID_TOOL_NOISEGEN

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        g.background = Color.cyan
        g.drawImage(noiseImage,
                Terrarum.WIDTH.minus(imagesize).div(2).toFloat(),
                Terrarum.HEIGHT.minus(imagesize).div(2).toFloat()
        )
    }

    override fun keyPressed(key: Int, c: Char) {
        if (c == ' ') {
            println("Generating noise, may take a while")
            generateNoiseImage()
        }
    }
}