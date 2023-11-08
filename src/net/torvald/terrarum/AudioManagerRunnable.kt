package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.gamecontroller.InputStrober

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
                AudioManager.update(dT)
                Thread.sleep(20L)
            }
            catch (e: InterruptedException) {
                break
            }
        }
    }
}