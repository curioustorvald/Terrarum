package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.backends.lwjgl3.audio.Mp3
import com.badlogic.gdx.backends.lwjgl3.audio.Ogg
import com.badlogic.gdx.backends.lwjgl3.audio.OggInputStream
import com.badlogic.gdx.backends.lwjgl3.audio.Wav
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.jcraft.jorbis.VorbisFile
import javazoom.jl.decoder.Bitstream
import net.torvald.reflection.extortField
import net.torvald.reflection.forceInvoke
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.AudioSystem

data class MusicContainer(
    val name: String,
    val file: File,
    val looping: Boolean = false,
    val toRAM: Boolean = false,
    internal var songFinishedHook: (MusicContainer) -> Unit = {}
): Disposable {
    val samplingRate: Int
    val codec: String

    var samplesReadCount = 0L; internal set
    var samplesTotal: Long

    private val gdxMusic: Music = Gdx.audio.newMusic(FileHandle(file))

    private var soundBuf: UnsafePtr? = null; private set

    private val hash = System.nanoTime()

    init {
        gdxMusic.isLooping = looping

//        gdxMusic.setOnCompletionListener(songFinishedHook)

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

        samplesTotal = when (gdxMusic) {
            is Wav.Music -> getWavFileSampleCount(file)
            is Ogg.Music -> getOggFileSampleCount(file)
            is Mp3.Music -> getMp3FileSampleCount(file)
            else -> Long.MAX_VALUE
        }


        if (toRAM) {
            if (samplesTotal == Long.MAX_VALUE) throw IllegalStateException("Could not read sample count")

            val readSize = 8192
            var readCount = 0L
            val readBuf = ByteArray(readSize)

            soundBuf = UnsafeHelper.allocate(4L * samplesTotal)

            while (readCount < samplesTotal) {
                val read = gdxMusic.forceInvoke<Int>("read", arrayOf(readBuf))!!.toLong()

                UnsafeHelper.memcpyRaw(readBuf, UnsafeHelper.getArrayOffset(readBuf), null, soundBuf!!.ptr + readCount, read)

                readCount += read
            }
        }
    }

    fun readBytes(buffer: ByteArray): Int {
        if (soundBuf == null) {
            val bytesRead = gdxMusic.forceInvoke<Int>("read", arrayOf(buffer)) ?: 0
            samplesReadCount += bytesRead / 4
            return bytesRead
        }
        else {
            val bytesToRead = minOf(buffer.size.toLong(), 4 * (samplesTotal - samplesReadCount))
            if (bytesToRead <= 0) return bytesToRead.toInt()

            UnsafeHelper.memcpyRaw(null, soundBuf!!.ptr + samplesReadCount * 4, buffer, UnsafeHelper.getArrayOffset(buffer), bytesToRead)

            samplesReadCount += bytesToRead / 4
            return bytesToRead.toInt()
        }
    }

    fun reset() {
        samplesReadCount = 0L
        gdxMusic.forceInvoke<Int>("reset", arrayOf())
    }

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

    fun makeCopy(): MusicContainer {
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