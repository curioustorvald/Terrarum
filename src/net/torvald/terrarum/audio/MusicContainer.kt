package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.backends.lwjgl3.audio.Mp3
import com.badlogic.gdx.backends.lwjgl3.audio.Ogg
import com.badlogic.gdx.backends.lwjgl3.audio.OggInputStream
import com.badlogic.gdx.backends.lwjgl3.audio.Wav
import com.badlogic.gdx.files.FileHandle
import com.jcraft.jorbis.VorbisFile
import javazoom.jl.decoder.Bitstream
import net.torvald.reflection.extortField
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.App.printdbg
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.AudioSystem

class MusicContainer(
    override val name: String,
    val file: File,
    val looping: Boolean = false,
    val toRAM: Boolean = false,
    override var songFinishedHook: (AudioBank) -> Unit = {}
): AudioBank() {
    override val samplingRate: Int
    override val channels: Int
    val codec: String

    var samplesReadCount = 0L; internal set
    override val totalSizeInSamples: Long
    private val totalSizeInBytes: Long

    private val gdxMusic: Music = Gdx.audio.newMusic(FileHandle(file))

    private var soundBuf: UnsafePtr? = null; private set

    private val bytesPerSample: Int
    
    init {
        gdxMusic.isLooping = looping

//        gdxMusic.setOnCompletionListener(songFinishedHook)

        channels = when (gdxMusic) {
            is Wav.Music -> gdxMusic.extortField<Wav.WavInputStream>("input")!!.channels
            is Ogg.Music -> gdxMusic.extortField<OggInputStream>("input")!!.channels
            else -> 2
        }
        
        bytesPerSample = 2 * channels

        samplingRate = when (gdxMusic) {
            is Wav.Music -> {
                val rate = gdxMusic.extortField<Wav.WavInputStream>("input")!!.sampleRate

//                App.printdbg(this, "music $name is WAV; rate = $rate")
                rate
            }
            is Ogg.Music -> {
                val rate = gdxMusic.extortField<OggInputStream>("input")!!.sampleRate

//                App.printdbg(this, "music $name is OGG; rate = $rate")
                rate
            }
            is Mp3.Music -> {
                val tempMusic = Gdx.audio.newMusic(Gdx.files.absolute(file.absolutePath))
                val bitstream = tempMusic.extortField<Bitstream>("bitstream")!!
                val header = bitstream.readFrame()
                val rate = header.sampleRate
                tempMusic.dispose()

//                val bitstream = gdxMusic.extortField<Bitstream>("bitstream")!!
//                val header = bitstream.readFrame()
//                val rate = header.sampleRate
//                gdxMusic.reset()

//                App.printdbg(this, "music $name is MP3; rate = $rate")
                rate
            }
            else -> {
//                App.printdbg(this, "music $name is ${gdxMusic::class.qualifiedName}; rate = default")
                TerrarumAudioMixerTrack.SAMPLING_RATE
            }
        }

        codec = gdxMusic::class.qualifiedName!!.split('.').let {
            if (it.last() == "Music") it.dropLast(1).last() else it.last()
        }

        totalSizeInSamples = when (gdxMusic) {
            is Wav.Music -> getWavFileSampleCount(file)
            is Ogg.Music -> getOggFileSampleCount(file)
            is Mp3.Music -> getMp3FileSampleCount(file)
            else -> Long.MAX_VALUE
        }
        totalSizeInBytes = totalSizeInSamples * 2 * channels


        if (toRAM) {
            if (totalSizeInSamples == Long.MAX_VALUE) throw IllegalStateException("Could not read sample count")

            val readSize = 8192
            var readCount = 0L
            val readBuf = ByteArray(readSize)

            soundBuf = UnsafeHelper.allocate(totalSizeInBytes)

            while (readCount < totalSizeInBytes) {
                gdxMusic.forceInvoke<Int>("read", arrayOf(readBuf))!!.toLong() // its return value will be useless for looping=true
                val read = minOf(readSize.toLong(), (totalSizeInBytes - readCount))

                UnsafeHelper.memcpyRaw(readBuf, UnsafeHelper.getArrayOffset(readBuf), null, soundBuf!!.ptr + readCount, read)

                readCount += read

//                printdbg(this, "read $readCount/$totalSizeInBytes bytes (${readCount/(2*channels)}/$totalSizeInSamples samples)")
            }
        }
    }

    private fun read0(buffer: ByteArray, bytesRead: Int): Int {
        val tmpBuf = ByteArray(buffer.size - bytesRead)
        val newRead = readBytes(tmpBuf)

        System.arraycopy(tmpBuf, 0, buffer, bytesRead, tmpBuf.size)

        return newRead
    }

    override fun readBytes(buffer: ByteArray): Int {
        if (soundBuf == null) {
            val bytesRead = gdxMusic.forceInvoke<Int>("read", arrayOf(buffer)) ?: 0
            samplesReadCount += bytesRead / bytesPerSample

            if (looping && bytesRead < buffer.size) {
                reset()

                val remainder = buffer.size - bytesRead

                val fullCopyCounts = remainder / totalSizeInBytes
                val partialCopyCountsInBytes = (remainder % totalSizeInBytes).toInt()

                var start = bytesRead

                // make full block copies
                if (fullCopyCounts > 0) {
                    val fullbuf = ByteArray(totalSizeInBytes.toInt())
                    // only read ONCE, you silly
                    gdxMusic.forceInvoke<Int>("read", arrayOf(fullbuf))
                    reset()

                    for (i in 0 until fullCopyCounts) {
                        System.arraycopy(fullbuf, 0, buffer, start, fullbuf.size)
                        start += totalSizeInBytes.toInt()
                    }
                }

                // copy the remainders from the start of the samples
                val partialBuf = ByteArray(partialCopyCountsInBytes)
                gdxMusic.forceInvoke<Int>("read", arrayOf(partialBuf))
                System.arraycopy(partialBuf, 0, buffer, start, partialCopyCountsInBytes)

                samplesReadCount += partialCopyCountsInBytes / bytesPerSample
            }

            return bytesRead
        }
        else {
            val bytesToRead = minOf(buffer.size.toLong(), 2 * channels * (totalSizeInSamples - samplesReadCount))

            if (!looping && bytesToRead <= 0) return bytesToRead.toInt()
//            if (looping) printdbg(this, "toRAM music loop (bytes cursor: $samplesReadCount/$totalSizeInSamples, bytesToRead=$bytesToRead, buffer.size=${buffer.size})")

            UnsafeHelper.memcpyRaw(
                null,
                soundBuf!!.ptr + samplesReadCount * bytesPerSample,
                buffer,
                UnsafeHelper.getArrayOffset(buffer),
                bytesToRead
            )

            samplesReadCount += bytesToRead / bytesPerSample


            // reached the end of the "tape"
            if (looping && bytesToRead < buffer.size) {

                val remainder = buffer.size - bytesToRead

                val fullCopyCounts = remainder / totalSizeInBytes
                val partialCopyCountsInBytes = remainder % totalSizeInBytes

                var start = UnsafeHelper.getArrayOffset(buffer) + bytesToRead

                // make full block copies
                for (i in 0 until fullCopyCounts) {
                    UnsafeHelper.memcpyRaw(
                        null,
                        soundBuf!!.ptr,
                        buffer,
                        start,
                        totalSizeInBytes
                    )

                    start += totalSizeInBytes
                }

                // copy the remainders from the start of the "tape"
                UnsafeHelper.memcpyRaw(
                    null,
                    soundBuf!!.ptr,
                    buffer,
                    start,
                    partialCopyCountsInBytes
                )

                samplesReadCount = partialCopyCountsInBytes / bytesPerSample
            }

            if (looping) return buffer.size

            return bytesToRead.toInt()
        }
    }

    override fun reset() {
        samplesReadCount = 0L
        gdxMusic.forceInvoke<Int>("reset", arrayOf())
    }

    override fun currentPositionInSamples() = samplesReadCount

    private fun getWavFileSampleCount(file: File): Long {
        return try {
            val ais = AudioSystem.getAudioInputStream(file)
            val r = ais.frameLength
            ais.close()
            r
        }
        catch (e: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getOggFileSampleCount(file: File): Long {
        return try {
            val vorbisFile = VorbisFile(file.absolutePath)
            vorbisFile.pcm_total(0)
        }
        catch (e: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getMp3FileSampleCount(file: File): Long {
        return try {
            val fis = FileInputStream(file)
            val bs = Bitstream(fis)

            var header = bs.readFrame()
            val rate = header.frequency()
            var totalSamples = 0L

            while (header != null) {
                totalSamples += (header.ms_per_frame() * rate / 1000).toLong()
                bs.closeFrame()
                header = bs.readFrame()
            }

            bs.close()
            fis.close()

            totalSamples
        }
        catch (_: Throwable) {
            Long.MAX_VALUE
        }
    }

    override fun toString() = if (name.isEmpty()) file.nameWithoutExtension else name

    override fun equals(other: Any?) = this.file.path == (other as MusicContainer).file.path
    fun equalInstance(other: Any?) = this.file.path == (other as MusicContainer).file.path && this.hash == (other as MusicContainer).hash

    override fun dispose() {
        gdxMusic.dispose()
        soundBuf?.destroy()
    }

    override fun makeCopy(): AudioBank {
        val new = MusicContainer(name, file, looping, false, songFinishedHook)

        synchronized(this) {
            if (this.toRAM) {
                // perform unsafe memcpy
                new.soundBuf = UnsafeHelper.allocate(this.soundBuf!!.size)
                UnsafeHelper.memcpy(this.soundBuf!!, 0L, new.soundBuf!!, 0L, new.soundBuf!!.size)

                // set toRAM flag
                val toRamField = new.javaClass.getDeclaredField("toRAM")
                UnsafeHelper.unsafe.putBoolean(new, UnsafeHelper.unsafe.objectFieldOffset(toRamField), true)
            }
        }

        return new
    }
}