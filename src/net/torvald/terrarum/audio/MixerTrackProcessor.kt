package net.torvald.terrarum.audio

import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE

/**
 * Created by minjaesong on 2023-11-17.
 */
class MixerTrackProcessor(val bufferSize: Int, val track: TerrarumAudioMixerTrack): Runnable {
    @Volatile
    private var running = true

    @Volatile
    private var paused = false
    private val pauseLock = java.lang.Object()




    internal val streamBuf = AudioProcessBuf(bufferSize)
    internal val sideChainBufs = Array(track.sidechainInputs.size) { AudioProcessBuf(bufferSize) }

    private var fout0 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
    private var fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))


    override fun run() {
        w@ while (running) {
            synchronized(pauseLock) {
                if (!running) { // may have changed while waiting to
                    // synchronize on pauseLock
//                    break@w
                }
                if (paused) {
                    try {
                        pauseLock.wait() // will cause this Thread to block until
                        // another thread calls pauseLock.notifyAll()
                        // Note that calling wait() will
                        // relinquish the synchronized lock that this
                        // thread holds on pauseLock so another thread
                        // can acquire the lock to call notifyAll()
                        // (link with explanation below this code)
                    }
                    catch (ex: InterruptedException) {
//                        break@w
                    }
                    if (!running) { // running might have changed since we paused
//                        break@w
                    }
                }
            }
            // Your code here


//            println("AudioMixerTrack ${track.name} (${track.hash}) streamPlaying=${track.streamPlaying}")

            // fetch deviceBufferSize amount of sample from the disk
            if (!track.isMaster && track.streamPlaying) {
                streamBuf.fetchBytes {
                    track.currentTrack?.gdxMusic?.forceInvoke<Int>("read", arrayOf(it))
//                    for (i in it.indices) { it[i] = (Math.random() * 255).toInt().toByte() }
                }
            }

            // also fetch samples from sidechainInputs
            // TODO

            // combine all the inputs
            // TODO this code just uses streamBuf


            var samplesL0: FloatArray
            var samplesR0: FloatArray
            var samplesL1: FloatArray
            var samplesR1: FloatArray

            if (track.isMaster) {
                // TEST CODE must combine all the inputs
                samplesL0 = track.sidechainInputs[0]!!.first.processor.fout0[0]
                samplesR0 = track.sidechainInputs[0]!!.first.processor.fout0[1]
                samplesL1 = track.sidechainInputs[0]!!.first.processor.fout1[0]
                samplesR1 = track.sidechainInputs[0]!!.first.processor.fout1[1]
            }
            else {
                samplesL0 = streamBuf.getL0(track.volume)
                samplesR0 = streamBuf.getR0(track.volume)
                samplesL1 = streamBuf.getL1(track.volume)
                samplesR1 = streamBuf.getR1(track.volume)
            }


            // run the input through the stack of filters
            val filterStack = track.filters.filter { !it.bypass && it !is NullFilter }

            if (filterStack.isEmpty()) {
                fout1 = listOf(samplesL1, samplesR1)
            }
            else {
                var fin0 = listOf(samplesL0, samplesR0)
                var fin1 = listOf(samplesL1, samplesR1)
                fout0 = fout1
                fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))

                filterStack.forEachIndexed { index, it ->
                    it(fin0, fin1, fout0, fout1)
                    fin0 = fout0
                    fin1 = fout1
                    if (index < filterStack.lastIndex) {
                        fout0 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
                        fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
                    }
                }
            }


            // by this time, the output buffer is filled with processed results, pause the execution
            if (!track.isMaster) {
                this.pause()
            }
            else {
                track.adev!!.setVolume(AudioMixer.masterVolume.toFloat())
                val samples = interleave(fout1[0], fout1[1])
                track.adev!!.writeSamples(samples, 0, samples.size)
                Thread.sleep(1)

                track.getSidechains().forEach {
                    it?.processor?.resume()
                }
            }


        }
    }

    private fun interleave(f1: FloatArray, f2: FloatArray) = FloatArray(f1.size + f2.size) {
        if (it % 2 == 0) f1[it / 2] else f2[it / 2]
    }

    fun stop() {
        running = false
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resume()
        // to unblock
    }

    fun pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true
    }

    fun resume() {
        synchronized(pauseLock) {
            paused = false
            pauseLock.notifyAll() // Unblocks thread
        }
    }
}