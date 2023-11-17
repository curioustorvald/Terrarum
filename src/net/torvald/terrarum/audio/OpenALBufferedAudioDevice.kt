package net.torvald.terrarum.audio

import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Created by minjaesong on 2023-01-01.
 */
/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/** @author Nathan Sweet
 */
class OpenALBufferedAudioDevice(
    private val audio: OpenALLwjgl3Audio,
    val rate: Int,
    isMono: Boolean,
    val bufferSize: Int,
    val bufferCount: Int,
    private val fillBufferCallback: () -> Unit
) : AudioDevice {
    private val channels: Int
    private var buffers: IntBuffer? = null
    private var sourceID = -1
    private val format: Int
    private var isPlaying = false
    private var volume = 1f
    private var renderedSeconds = 0f
    private val secondsPerBuffer: Float
    private var bytes: ByteArray? = null
    private var bytesLength = 2
    private val tempBuffer: ByteBuffer

    /**
     * Invoked whenever a buffer is emptied after writing samples
     *
     * Preferably you write 2-3 buffers worth of samples at the beginning of the playback
     */

    init {
        channels = if (isMono) 1 else 2
        format = if (channels > 1) AL10.AL_FORMAT_STEREO16 else AL10.AL_FORMAT_MONO16
        secondsPerBuffer = bufferSize.toFloat() / bytesPerSample / channels / rate
        tempBuffer = BufferUtils.createByteBuffer(bufferSize)
    }

    override fun writeSamples(samples: ShortArray, offset: Int, numSamples: Int) {
        if (bytes == null || bytes!!.size < numSamples * 2) bytes = ByteArray(numSamples * 2)
        val end = Math.min(offset + numSamples, samples.size)
        var i = offset
        var ii = 0
        while (i < end) {
            val sample = samples[i]
            bytes!![ii++] = (sample.toInt() and 0xFF).toByte()
            bytes!![ii++] = (sample.toInt() shr 8 and 0xFF).toByte()
            i++
        }
        bytesLength = ii
        writeSamples(bytes!!, 0, numSamples * 2)
    }

    override fun writeSamples(samples: FloatArray, offset: Int, numSamples: Int) {
        if (bytes == null || bytes!!.size < numSamples * 2) bytes = ByteArray(numSamples * 2)
        val end = Math.min(offset + numSamples, samples.size)
        var i = offset
        var ii = 0
        while (i < end) {
            var floatSample = samples[i]
            floatSample = MathUtils.clamp(floatSample, -1f, 1f)
            val intSample = (floatSample * 32767).toInt()
            bytes!![ii++] = (intSample and 0xFF).toByte()
            bytes!![ii++] = (intSample shr 8 and 0xFF).toByte()
            i++
        }
        bytesLength = ii
        writeSamples(bytes!!, 0, numSamples * 2)
    }

    private fun interleave(f1: FloatArray, f2: FloatArray) = FloatArray(f1.size + f2.size) {
        if (it % 2 == 0) f1[it / 2] else f2[it / 2]
    }

    /**
     * @param samples multitrack
     * @param numSamples number of samples per channel
     */
    fun writeSamples(samples: List<FloatArray>) {
        interleave(samples[0], samples[1]).let {
            writeSamples(it, 0, it.size)
        }
    }

    private fun audioObtainSource(isMusic: Boolean): Int {
        val obtainSourceMethod = OpenALLwjgl3Audio::class.java.getDeclaredMethod("obtainSource", java.lang.Boolean.TYPE)
        obtainSourceMethod.isAccessible = true
        return obtainSourceMethod.invoke(audio, isMusic) as Int
    }
    private fun audioFreeSource(sourceID: Int) {
        val freeSourceMethod = OpenALLwjgl3Audio::class.java.getDeclaredMethod("freeSource", java.lang.Integer.TYPE)
        freeSourceMethod.isAccessible = true
        freeSourceMethod.invoke(audio, sourceID)
    }

    private val alErrors = hashMapOf(
        AL10.AL_INVALID_NAME to "AL_INVALID_NAME",
        AL10.AL_INVALID_ENUM to "AL_INVALID_ENUM",
        AL10.AL_INVALID_VALUE to "AL_INVALID_VALUE",
        AL10.AL_INVALID_OPERATION to "AL_INVALID_OPERATION",
        AL10.AL_OUT_OF_MEMORY to  "AL_OUT_OF_MEMORY"
    )

    fun writeSamples(data: ByteArray, offset: Int, length: Int) {
        var offset = offset
        var length = length
        require(length >= 0) { "length cannot be < 0." }
        if (sourceID == -1) {
            sourceID = audioObtainSource(true)
            if (sourceID == -1) return
            if (buffers == null) {
                buffers = BufferUtils.createIntBuffer(bufferCount)
                AL10.alGetError()
                AL10.alGenBuffers(buffers)
                AL10.alGetError().let {
                    if (it != AL10.AL_NO_ERROR) throw GdxRuntimeException("Unabe to allocate audio buffers: ${alErrors[it]}")
                }
            }
            AL10.alSourcei(sourceID, AL10.AL_LOOPING, AL10.AL_FALSE)
            AL10.alSourcef(sourceID, AL10.AL_GAIN, volume)
            // Fill initial buffers.
            var queuedBuffers = 0
            for (i in 0 until bufferCount) {
                val bufferID = buffers!![i]
                val written = Math.min(bufferSize, length)
                (tempBuffer as Buffer).clear()
                (tempBuffer.put(data, offset, written) as Buffer).flip()
                AL10.alBufferData(bufferID, format, tempBuffer, rate)
                AL10.alSourceQueueBuffers(sourceID, bufferID)
                length -= written
                offset += written
                queuedBuffers++
            }
            // Queue rest of buffers, empty.
            (tempBuffer as Buffer).clear().flip()
            for (i in queuedBuffers until bufferCount) {
                val bufferID = buffers!![i]
                AL10.alBufferData(bufferID, format, tempBuffer, rate)
                AL10.alSourceQueueBuffers(sourceID, bufferID)
            }
            AL10.alSourcePlay(sourceID)
            isPlaying = true
        }
        while (length > 0) {
            val written = fillBuffer(data, offset, length)
            length -= written
            offset += written
        }
    }

    /** Blocks until some of the data could be buffered.  */
    private fun fillBuffer(data: ByteArray, offset: Int, length: Int): Int {
        val written = Math.min(bufferSize, length)
        outer@ while (true) {
            var buffers = AL10.alGetSourcei(sourceID, AL10.AL_BUFFERS_PROCESSED)
            while (buffers-- > 0) {
                val bufferID = AL10.alSourceUnqueueBuffers(sourceID)
                if (bufferID == AL10.AL_INVALID_VALUE) break
                renderedSeconds += secondsPerBuffer
                (tempBuffer as Buffer).clear()
                (tempBuffer.put(data, offset, written) as Buffer).flip()
                AL10.alBufferData(bufferID, format, tempBuffer, rate)
                AL10.alSourceQueueBuffers(sourceID, bufferID)
                break@outer
            }
            // Wait for buffer to be free.
            try {
                Thread.sleep((1000 * secondsPerBuffer).toLong())
                fillBufferCallback()
            }
            catch (ignored: InterruptedException) {
            }
        }

        // A buffer underflow will cause the source to stop.
        if (!isPlaying || AL10.alGetSourcei(sourceID, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(sourceID)
            isPlaying = true
        }
        return written
    }

    fun stop() {
        if (sourceID == -1) return
        audioFreeSource(sourceID)
        sourceID = -1
        renderedSeconds = 0f
        isPlaying = false
    }

    fun isPlaying(): Boolean {
        return if (sourceID == -1) false else isPlaying
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        if (sourceID != -1) AL10.alSourcef(sourceID, AL10.AL_GAIN, volume)
    }

    var position: Float
        get() = if (sourceID == -1) 0f else renderedSeconds + AL10.alGetSourcef(sourceID, AL11.AL_SEC_OFFSET)
        set(position) {
            renderedSeconds = position
        }

    fun getChannels(): Int {
        return if (format == AL10.AL_FORMAT_STEREO16) 2 else 1
    }

    override fun dispose() {
        if (buffers == null) return
        if (sourceID != -1) {
            audioFreeSource(sourceID)
            sourceID = -1
        }
        AL10.alDeleteBuffers(buffers)
        buffers = null
    }

    override fun isMono(): Boolean {
        return channels == 1
    }

    override fun getLatency(): Int {
        return (secondsPerBuffer * bufferCount * 1000).toInt()
    }

    override fun pause() {
        // A buffer underflow will cause the source to stop.
    }

    override fun resume() {
        // Automatically resumes when samples are written
    }

    companion object {
        private const val bytesPerSample = 2
        private val ui8toI16Hi = ByteArray(256) { (128 + it).toByte() }

    }
}