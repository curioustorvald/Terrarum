package net.torvald.terrarum

import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.gameactors.roundInt
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.ImageBuffer
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * WARNING! HAS SERIOUS MEMORY LEAK
 *
 * Created by SKYHi14 on 2017-01-30.
 */
class StateNoiseTester : BasicGameState() {

    companion object {
        val imagesize = 512
        val sampleDensity = 1.0
        val noiseImageBuffer = ImageBuffer(imagesize, imagesize)
        var generating = false
    }
    override fun init(p0: GameContainer?, p1: StateBasedGame?) {
        generateNoiseImage()
    }

    private fun noise(seed: Long): Joise {
        /* Init */

        val joiseSeed = seed
        val lowlandMagic: Long = 0x44A21A114DBE56 // maria lindberg
        val highlandMagic: Long = 0x0114E091      // olive oyl
        val mountainMagic: Long = 0x115AA4DE2504  // lisa anderson
        val selectionMagic: Long = 0x44E10D9B100  // melody blue

        val ground_gradient = ModuleGradient()
        ground_gradient.setGradient(0.0, 0.0, 0.0, 1.0)

        /* Lowlands */

        val lowland_shape_fractal = ModuleFractal()
        lowland_shape_fractal.setType(ModuleFractal.FractalType.FBM)
        lowland_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        lowland_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        lowland_shape_fractal.setNumOctaves(2)
        lowland_shape_fractal.setFrequency(1.0)
        lowland_shape_fractal.seed = joiseSeed xor lowlandMagic

        val lowland_autocorrect = ModuleAutoCorrect()
        lowland_autocorrect.setSource(lowland_shape_fractal)
        lowland_autocorrect.setLow(0.0)
        lowland_autocorrect.setHigh(1.0)

        val lowland_scale = ModuleScaleOffset()
        lowland_scale.setSource(lowland_autocorrect)
        lowland_scale.setScale(0.2)
        lowland_scale.setOffset(-0.25)

        val lowland_y_scale = ModuleScaleDomain()
        lowland_y_scale.setSource(lowland_scale)
        lowland_y_scale.setScaleY(0.0)

        val lowland_terrain = ModuleTranslateDomain()
        lowland_terrain.setSource(ground_gradient)
        lowland_terrain.setAxisYSource(lowland_y_scale)


        /* highlands */

        val highland_shape_fractal = ModuleFractal()
        highland_shape_fractal.setType(ModuleFractal.FractalType.RIDGEMULTI)
        highland_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        highland_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        highland_shape_fractal.setNumOctaves(2)
        highland_shape_fractal.setFrequency(2.0)
        highland_shape_fractal.seed = joiseSeed xor highlandMagic

        val highland_autocorrect = ModuleAutoCorrect()
        highland_autocorrect.setSource(highland_shape_fractal)
        highland_autocorrect.setLow(0.0)
        highland_autocorrect.setHigh(1.0)

        val highland_scale = ModuleScaleOffset()
        highland_scale.setSource(highland_autocorrect)
        highland_scale.setScale(0.45)
        highland_scale.setOffset(0.0)

        val highland_y_scale = ModuleScaleDomain()
        highland_y_scale.setSource(highland_scale)
        highland_y_scale.setScaleY(0.0)

        val highland_terrain = ModuleTranslateDomain()
        highland_terrain.setSource(ground_gradient)
        highland_terrain.setAxisYSource(highland_y_scale)


        /* mountains */

        val mountain_shape_fractal = ModuleFractal()
        mountain_shape_fractal.setType(ModuleFractal.FractalType.BILLOW)
        mountain_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        mountain_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        mountain_shape_fractal.setNumOctaves(4)
        mountain_shape_fractal.setFrequency(1.0)
        mountain_shape_fractal.seed = joiseSeed xor mountainMagic

        val mountain_autocorrect = ModuleAutoCorrect()
        mountain_autocorrect.setSource(mountain_shape_fractal)
        mountain_autocorrect.setLow(0.0)
        mountain_autocorrect.setHigh(1.0)

        val mountain_scale = ModuleScaleOffset()
        mountain_scale.setSource(mountain_autocorrect)
        mountain_scale.setScale(0.75)
        mountain_scale.setOffset(0.25)

        val mountain_y_scale = ModuleScaleDomain()
        mountain_y_scale.setSource(mountain_scale)
        mountain_y_scale.setScaleY(0.1) // controls "quirkiness" of the mountain

        val mountain_terrain = ModuleTranslateDomain()
        mountain_terrain.setSource(ground_gradient)
        mountain_terrain.setAxisYSource(mountain_y_scale)


        /* selection */

        val terrain_type_fractal = ModuleFractal()
        terrain_type_fractal.setType(ModuleFractal.FractalType.FBM)
        terrain_type_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        terrain_type_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        terrain_type_fractal.setNumOctaves(3)
        terrain_type_fractal.setFrequency(0.5)
        terrain_type_fractal.seed = joiseSeed xor selectionMagic

        val terrain_autocorrect = ModuleAutoCorrect()
        terrain_autocorrect.setSource(terrain_type_fractal)
        terrain_autocorrect.setLow(0.0)
        terrain_autocorrect.setHigh(1.0)

        val terrain_type_cache = ModuleCache()
        terrain_type_cache.setSource(terrain_autocorrect)

        val highland_mountain_select = ModuleSelect()
        highland_mountain_select.setLowSource(highland_terrain)
        highland_mountain_select.setHighSource(mountain_terrain)
        highland_mountain_select.setControlSource(terrain_type_cache)
        highland_mountain_select.setThreshold(0.55)
        highland_mountain_select.setFalloff(0.15)

        val highland_lowland_select = ModuleSelect()
        highland_lowland_select.setLowSource(lowland_terrain)
        highland_lowland_select.setHighSource(highland_mountain_select)
        highland_lowland_select.setControlSource(terrain_type_cache)
        highland_lowland_select.setThreshold(0.25)
        highland_lowland_select.setFalloff(0.15)

        val ground_select = ModuleSelect()
        ground_select.setLowSource(0.0)
        ground_select.setHighSource(1.0)
        ground_select.setThreshold(0.5)
        ground_select.setControlSource(highland_lowland_select)


        val joise = Joise(ground_select)
        return joise
    }

    fun generateNoiseImage() {
        val noiseModule = noise(HQRNG().nextLong()) // change noise function here

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
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}"
        + " — M: ${Terrarum.memInUse}M / ${Terrarum.totalVMMem}M")


        if (ThreadParallel.allFinished()) generating = false
    }

    override fun getID() = Terrarum.STATE_ID_TOOL_NOISEGEN

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        g.color = Color.red
        g.drawString("Press SPACE to generate new noise", 8f, 8f)
        g.drawString("CPUs: ${Terrarum.THREADS}", Terrarum.WIDTH - 90f, 8f)

        g.background = Color.cyan
        g.drawImage(noiseImageBuffer.image,//noiseImage,
                Terrarum.WIDTH.minus(imagesize).div(2).toFloat(),
                Terrarum.HEIGHT.minus(imagesize).div(2).toFloat()
        )
    }

    override fun keyPressed(key: Int, c: Char) {
        if (c == ' ' && !generating) {
            println("Generating noise, may take a while")
            generating = true
            generateNoiseImage()
        }
    }


    class ThreadRunNoiseSampling(val startIndex: Int, val endIndex: Int, val joise: Joise) : Runnable {
        /*override fun run() {
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
        }*/

        override fun run() {
            for (sy in startIndex..endIndex) {
                for (sx in 0..imagesize - 1) {
                    val y = sy.toDouble() / imagesize * 1.5 -.6
                    val x = sx.toDouble() / imagesize

                    val sampleOffset = sampleDensity
                    // 4-D toroidal sampling (looped H and V)
                    val sampleTheta1 = x * Math.PI * 2.0
                    val sampleX = Math.sin(sampleTheta1) * sampleDensity + sampleDensity
                    val sampleZ = Math.cos(sampleTheta1) * sampleDensity + sampleDensity
                    val sampleY = y

                    val noise = joise.get(
                            sampleX, sampleY, sampleZ
                    ) // autocorrection REQUIRED!

                    val noiseCol = noise.times(255f).toInt()
                    noiseImageBuffer.setRGBA(sx, sy, noiseCol, noiseCol, noiseCol, 255)

                }
            }
        }
    }
}