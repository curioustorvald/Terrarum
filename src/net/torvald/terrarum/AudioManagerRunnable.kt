package net.torvald.terrarum

import net.torvald.terrarum.audio.AudioMixer

/**
 * Created by minjaesong on 2023-11-08.
 */
class AudioManagerRunnable : Runnable {

    var oldT = System.nanoTime()
    var dT = 0f

    override fun run() {
        while (!Thread.interrupted()) {
            try {
                val T = System.nanoTime()
                dT = (T - oldT) / 1000000000f
                oldT = T;
                AudioMixer.update(dT)
//                println("AudioManagerRunnable dT = ${dT * 1000f} ms")
                Thread.sleep(30L)
            }
            catch (e: InterruptedException) {
                break
            }
        }
    }
}