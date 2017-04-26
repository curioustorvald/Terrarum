package net.torvald.terrarum.worldgenerator

import net.torvald.random.HQRNG
import com.jme3.math.FastMath

class SimplexNoise
/**
 * @param largestFeature
 * *
 * @param persistence    higher the value, rougher the output
 * *
 * @param seed
 */
(internal var largestFeature: Int, internal var persistence: Float, internal var seed: Long) {

    internal var octaves: Array<SimplexNoise_octave>
    internal var frequencys: FloatArray
    internal var amplitudes: FloatArray

    init {

        //receives a number (e.g. 128) and calculates what power of 2 it is (e.g. 2^7)
        val numberOfOctaves = FastMath.intLog2(largestFeature)
        val rnd = HQRNG(seed)

        octaves = Array<SimplexNoise_octave>(numberOfOctaves, {i -> SimplexNoise_octave(rnd.nextInt())})
        frequencys = FloatArray(numberOfOctaves)
        amplitudes = FloatArray(numberOfOctaves)

        for (i in 0..numberOfOctaves - 1) {
            octaves[i] = SimplexNoise_octave(rnd.nextInt())

            frequencys[i] = FastMath.pow(2f, i.toFloat())
            amplitudes[i] = FastMath.pow(persistence, (octaves.size - i).toFloat())


        }

    }


    fun getNoise(x: Int, y: Int): Float {

        var result = 0f

        for (i in octaves.indices) {
            //float frequency = FastMath.pow(2,i);
            //float amplitude = FastMath.pow(persistence,octaves.length-i);

            result += (octaves[i].noise((x / frequencys[i]).toDouble(), (y / frequencys[i]).toDouble()) * amplitudes[i]).toFloat()
        }


        return result

    }

    fun getNoise(x: Int, y: Int, z: Int): Float {

        var result = 0f

        for (i in octaves.indices) {
            val frequency = FastMath.pow(2f, i.toFloat())
            val amplitude = FastMath.pow(persistence, (octaves.size - i).toFloat())

            result += (octaves[i].noise((x / frequency).toDouble(), (y / frequency).toDouble(), (z / frequency).toDouble()) * amplitude).toFloat()
        }


        return result

    }
} 
