package net.torvald.terrarum

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.Terrarum.STATE_ID_TOOL_NOISEGEN
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.gameactors.roundInt
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * Created by minjaesong on 2016-12-21.
 */
class StateNoiseTexGen : BasicGameState() {

    companion object {
        val imagesize = 512
        val sampleDensity = 1.0
        val noiseImageBuffer = ImageBuffer(imagesize, imagesize)
        var generating = false
    }
    override fun init(p0: GameContainer?, p1: StateBasedGame?) {
        generateNoiseImage()
    }

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

        return Joise(ridged_autocorrect)
    }

    private fun noiseBrownian(): Joise {
        val ridged = ModuleFractal()
        ridged.setType(ModuleFractal.FractalType.FBM)
        ridged.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        ridged.setNumOctaves(8)
        ridged.setFrequency(1.0)
        ridged.seed = Random().nextLong()

        val ridged_autocorrect = ModuleAutoCorrect()
        ridged_autocorrect.setRange(0.0, 1.0)
        ridged_autocorrect.setSource(ridged)

        return Joise(ridged_autocorrect)
    }

    private fun noiseBrownianGranite(): Joise {
        val ridged = ModuleFractal()
        ridged.setType(ModuleFractal.FractalType.FBM)
        ridged.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        ridged.setNumOctaves(2)
        ridged.setFrequency(16.0)
        ridged.seed = Random().nextLong()

        val brownian_select = ModuleSelect()
        brownian_select.setControlSource(ridged)
        brownian_select.setThreshold(0.8)
        brownian_select.setLowSource(0.0)
        brownian_select.setHighSource(1.0)

        return Joise(brownian_select)
    }

    private fun noiseBillowFractal(): Joise {
        val ridged = ModuleFractal()
        ridged.setType(ModuleFractal.FractalType.BILLOW)
        ridged.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        ridged.setNumOctaves(8)
        ridged.setFrequency(1.0)
        ridged.seed = Random().nextLong()

        val ridged_autocorrect = ModuleAutoCorrect()
        ridged_autocorrect.setRange(0.0, 1.0)
        ridged_autocorrect.setSource(ridged)

        return Joise(ridged_autocorrect)
    }

    private fun noiseSimplex(): Joise {
        val simplex = ModuleFractal()
        simplex.seed = Random().nextLong()
        simplex.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
        simplex.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.LINEAR)
        simplex.setNumOctaves(2)
        simplex.setFrequency(1.0)

        val simplex_autocorrect = ModuleAutoCorrect()
        simplex_autocorrect.setRange(0.0, 1.0)
        simplex_autocorrect.setSource(simplex)

        return Joise(simplex_autocorrect)
    }

    private fun noiseCellular(): Joise {
        val cellgen = ModuleCellGen()
        cellgen.seed = Random().nextLong()

        val cellular = ModuleCellular()
        cellular.setCellularSource(cellgen)
        cellular.setCoefficients(-1.0, 1.0, 0.0, 0.0)

        val cellular_autocorrect = ModuleAutoCorrect()
        cellular_autocorrect.setRange(0.0, 1.0)
        cellular_autocorrect.setSource(cellular)

        return Joise(cellular_autocorrect)
    }

    fun generateNoiseImage() {
        val noiseModule = noiseBrownianGranite() // change noise function here

        for (y in 0..imagesize - 1) {
            for (x in 0..imagesize - 1) {
                noiseImageBuffer.setRGBA(x, y, 0, 0, 0, 255)
            }
        }

        for (i in 0..Terrarum.THREADS - 1) {
            ThreadParallel.map(
                    i,
                    ThreadRunNoiseSampling(
                            imagesize.toFloat().div(Terrarum.THREADS).times(i).roundInt(),
                            imagesize.toFloat().div(Terrarum.THREADS).times(i.plus(1)).roundInt() - 1,
                            noiseModule
                    ),
                    "SampleJoiseMap"
            )
        }

        ThreadParallel.startAll()
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")

        if (ThreadParallel.allFinished()) generating = false
    }

    override fun getID() = STATE_ID_TOOL_NOISEGEN

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        g.color = Color.red
        g.drawString("Press SPACE to generate new noise", 8f, 8f)
        g.drawString("CPUs: ${Terrarum.THREADS}", Terrarum.WIDTH - 90f, 8f)

        g.background = Color.cyan
        val img = noiseImageBuffer.image
        g.drawImage(img,//noiseImage,
                Terrarum.WIDTH.minus(imagesize).div(2).toFloat(),
                Terrarum.HEIGHT.minus(imagesize).div(2).toFloat()
        )

        img.destroy()
    }

    override fun keyPressed(key: Int, c: Char) {
        if (c == ' ' && !generating) {
            println("Generating noise, may take a while")
            generating = true
            generateNoiseImage()
        }
    }


    class ThreadRunNoiseSampling(val startIndex: Int, val endIndex: Int, val joise: Joise) : Runnable {
        override fun run() {
            for (sy in startIndex..endIndex) {
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

                    val noise = joise.get(
                            sampleX, sampleY, sampleZ, sampleW
                    ) // autocorrection REQUIRED!

                    val noiseCol = noise.times(255f).toInt()
                    noiseImageBuffer.setRGBA(sx, sy, noiseCol, noiseCol, noiseCol, 255)

                }
            }
        }
    }
}