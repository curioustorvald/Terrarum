package com.Torvald.Terrarum.MapGenerator;

import com.Torvald.Rand.HighQualityRandom;
import com.jme3.math.FastMath;

public class SimplexNoise {

    SimplexNoise_octave[] octaves;
    float[] frequencys;
    float[] amplitudes;

    int largestFeature;
    float persistence;
    long seed;

    /**
     * @param largestFeature
     * @param persistence    higher the value, rougher the output
     * @param seed
     */
    public SimplexNoise(int largestFeature, float persistence, long seed) {
        this.largestFeature = largestFeature;
        this.persistence = persistence;
        this.seed = seed;

        //receives a number (e.g. 128) and calculates what power of 2 it is (e.g. 2^7)
        int numberOfOctaves = FastMath.intLog2(largestFeature);

        octaves = new SimplexNoise_octave[numberOfOctaves];
        frequencys = new float[numberOfOctaves];
        amplitudes = new float[numberOfOctaves];

        HighQualityRandom rnd = new HighQualityRandom(seed);

        for (int i = 0; i < numberOfOctaves; i++) {
            octaves[i] = new SimplexNoise_octave(rnd.nextInt());

            frequencys[i] = FastMath.pow(2, i);
            amplitudes[i] = FastMath.pow(persistence, octaves.length - i);


        }

    }


    public float getNoise(int x, int y) {

        float result = 0;

        for (int i = 0; i < octaves.length; i++) {
            //float frequency = FastMath.pow(2,i);
            //float amplitude = FastMath.pow(persistence,octaves.length-i);

            result = result + (float) (octaves[i].noise(x / frequencys[i], y / frequencys[i]) * amplitudes[i]);
        }


        return result;

    }

    public float getNoise(int x, int y, int z) {

        float result = 0;

        for (int i = 0; i < octaves.length; i++) {
            float frequency = FastMath.pow(2, i);
            float amplitude = FastMath.pow(persistence, octaves.length - i);

            result = result + (float) (octaves[i].noise(x / frequency, y / frequency, z / frequency) * amplitude);
        }


        return result;

    }
} 
