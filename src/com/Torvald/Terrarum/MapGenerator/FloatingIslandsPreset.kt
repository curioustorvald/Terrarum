package com.Torvald.Terrarum.MapGenerator

import com.Torvald.Rand.HQRNG
import com.sun.javaws.exceptions.InvalidArgumentException

import java.util.Random

object FloatingIslandsPreset {

    val PRESETS = 5

    internal fun generatePreset(random: HQRNG): Array<IntArray> {
        val index = random.nextInt(PRESETS)
        return generatePreset(index, random)
    }

    internal fun generatePreset(index: Int, random: Random): Array<IntArray> {
        if (index == 0) {
            return processPreset(random, FloatingIslePreset01.data, FloatingIslePreset01.w, FloatingIslePreset01.h)
        }
        else if (index == 1) {
            return processPreset(random, FloatingIslePreset02.data, FloatingIslePreset02.w, FloatingIslePreset02.h)
        }
        else if (index == 2) {
            return processPreset(random, FloatingIslePreset03.data, FloatingIslePreset03.w, FloatingIslePreset03.h)
        }
        else if (index == 3) {
            return processPreset(random, FloatingIslePreset04.data, FloatingIslePreset04.w, FloatingIslePreset04.h)
        }
        else {
            return processPreset(random, FloatingIslePreset05.data, FloatingIslePreset05.w, FloatingIslePreset05.h)
        }
    }

    private fun processPreset(random: Random, preset: IntArray, w: Int, h: Int): Array<IntArray> {
        val temp = Array(h) { IntArray(w) }
        var counter = 0
        val mirrored = random.nextBoolean()

        for (i in 0..h - 1) {
            for (j in 0..w - 1) {
                if (!mirrored) {
                    if (counter < preset.size - 1) {
                        temp[i][j] = preset[counter]
                        counter++
                    }
                    else {
                        temp[i][j] = 0
                    }
                }
                else {
                    if (counter < preset.size - 1) {
                        temp[i][w - 1 - j] = preset[counter]
                        counter++
                    }
                    else {
                        temp[i][w - 1 - j] = 0
                    }
                }
            }
        }

        return temp
    }
}
