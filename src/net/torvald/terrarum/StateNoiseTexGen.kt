package net.torvald.terrarum

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.Terrarum.Companion.STATE_ID_TOOL_NOISEGEN
import net.torvald.terrarum.concurrent.ThreadPool
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

    companion object {
        val imagesize = 512
        val noiseImage = Image(imagesize, imagesize)
        val sampleDensity = 1.0
        val noiseMap = Array<FloatArray>(imagesize, { FloatArray(size = imagesize, init = { 0f }) })
    }
    override fun init(p0: GameContainer?, p1: StateBasedGame?) {
        generateNoiseImage()

        println("Press SPACE to generate new noise")
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

    private fun noiseSmokyFractal(): Joise {
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

    private fun noiseBlobs(): Joise {
        val gradval = ModuleBasisFunction()
        gradval.seed = Random().nextLong()
        gradval.setType(ModuleBasisFunction.BasisType.GRADVAL)
        gradval.setInterpolation(ModuleBasisFunction.InterpolationType.QUINTIC)

        val gradval_autocorrect = ModuleAutoCorrect()
        gradval_autocorrect.setRange(0.0, 1.0)
        gradval_autocorrect.setSource(gradval)

        return Joise(gradval_autocorrect)
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
        val noiseModule = noiseSmokyFractal() // change noise function here

        noiseImage.graphics.background = Color.black

        for (y in 0..imagesize - 1) {
            for (x in 0..imagesize - 1) {
                noiseMap[y][x] = 0f
            }
        }

        for (i in 0..Terrarum.CORES - 1) {
            ThreadPool.map(
                    i,
                    ThreadRunNoiseSampling(
                            ((imagesize / Terrarum.CORES) * i),
                            ((imagesize / Terrarum.CORES) * i.plus(1)) - 1,
                            noiseModule
                    ),
                    "SampleJoiseMap"
            )
        }

        ThreadPool.startAll()
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")
    }

    override fun getID() = STATE_ID_TOOL_NOISEGEN

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        for (sy in 0..imagesize - 1) {
            for (sx in 0..imagesize - 1) {
                val noise = noiseMap[sy][sx]

                noiseImage.graphics.color = Color(noise, noise, noise)
                noiseImage.graphics.fillRect(sx.toFloat(), sy.toFloat(), 1f, 1f)
            }
        }

        noiseImage.graphics.flush()

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

                    noiseMap[sy][sx] = noise.toFloat()

                    //noiseImage.graphics.color = Color(noise.toFloat(), noise.toFloat(), noise.toFloat())
                    //noiseImage.graphics.fillRect(sx.toFloat(), sy.toFloat(), 1f, 1f)
                }
            }
        }
    }
}