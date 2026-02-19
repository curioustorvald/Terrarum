package net.torvald.terrarum.audio.audiobank

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
import net.torvald.terrarum.audio.AudioBank
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.AudioSystem

class MusicContainer(
    override val name: String,
    val file: File?,
    val fileHandle: FileHandle?,
    val looping: Boolean = false,
    val toRAM: Boolean = false,
    val samplingRateOverride: Float?, // this is FIXED sampling rate
    override var songFinishedHook: (AudioBank) -> Unit = {}
): AudioBank() {
    override var samplingRate: Float
    override var channels: Int
    val codec: String

    // File-based constructors (existing API)
    constructor(
        name: String,
        file: File,
        looping: Boolean = false,
        toRAM: Boolean = false,
        samplingRateOverride: Float?,
        songFinishedHook: (AudioBank) -> Unit = {}
    ) : this(name, file, null, looping, toRAM, samplingRateOverride, songFinishedHook)
    // make Java code shorter
    constructor(
        name: String,
        file: File,
        looping: Boolean = false,
        toRAM: Boolean = false,
        songFinishedHook: (AudioBank) -> Unit = {}
    ) : this(name, file, null, looping, toRAM, null, songFinishedHook)
    // make Java code shorter
    constructor(
        name: String,
        file: File,
        looping: Boolean = false,
        songFinishedHook: (AudioBank) -> Unit = {}
    ) : this(name, file, null, looping, false, null, songFinishedHook)
    // make Java code shorter
    constructor(
        name: String,
        file: File,
        songFinishedHook: (AudioBank) -> Unit = {}
    ) : this(name, file, null, false, false, null, songFinishedHook)

    // FileHandle-based constructor (for TEVD archive support)
    constructor(
        name: String,
        fileHandle: FileHandle,
        looping: Boolean = false,
        toRAM: Boolean = false,
        samplingRateOverride: Float? = null,
        songFinishedHook: (AudioBank) -> Unit = {}
    ) : this(name, null, fileHandle, looping, toRAM, samplingRateOverride, songFinishedHook)


    var samplesReadCount = 0L; internal set
    override var totalSizeInSamples: Long
    private val totalSizeInBytes: Long

    private val gdxMusic: Music = if (file != null) Gdx.audio.newMusic(FileHandle(file)) else Gdx.audio.newMusic(fileHandle!!)

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

        samplingRate = samplingRateOverride ?: when (gdxMusic) {
            is Wav.Music -> {
                val rate = gdxMusic.extortField<Wav.WavInputStream>("input")!!.sampleRate

//                App.printdbg(this, "music $name is WAV; rate = $rate")
                rate.toFloat()
            }
            is Ogg.Music -> {
                val rate = gdxMusic.extortField<OggInputStream>("input")!!.sampleRate

//                App.printdbg(this, "music $name is OGG; rate = $rate")
                rate.toFloat()
            }
            is Mp3.Music -> {
                val tempMusic = if (file == null)
                    Gdx.audio.newMusic(fileHandle)
                else
                    Gdx.audio.newMusic(Gdx.files.absolute(file.absolutePath))
                val bitstream = tempMusic.extortField<Bitstream>("bitstream")!!
                val header = bitstream.readFrame()
                val rate = header.sampleRate
                tempMusic.dispose()

//                val bitstream = gdxMusic.extortField<Bitstream>("bitstream")!!
//                val header = bitstream.readFrame()
//                val rate = header.sampleRate
//                gdxMusic.reset()

//                App.printdbg(this, "music $name is MP3; rate = $rate")
                rate.toFloat()
            }
            else -> {
//                App.printdbg(this, "music $name is ${gdxMusic::class.qualifiedName}; rate = default")
                TerrarumAudioMixerTrack.SAMPLING_RATEF
            }
        }

        codec = gdxMusic::class.qualifiedName!!.split('.').let {
            if (it.last() == "Music") it.dropLast(1).last() else it.last()
        }

        totalSizeInSamples = if (file != null) {
            when (gdxMusic) {
                is Wav.Music -> getWavFileSampleCount(file)
                is Ogg.Music -> getOggFileSampleCount(file)
                is Mp3.Music -> getMp3FileSampleCount(file)
                else -> Long.MAX_VALUE
            }
        } else if (fileHandle != null) {
            when (gdxMusic) {
                is Wav.Music -> getWavFileSampleCountFromHandle(fileHandle)
                is Ogg.Music -> getOggFileSampleCountFromHandle(fileHandle)
                is Mp3.Music -> getMp3FileSampleCountFromHandle(fileHandle)
                else -> Long.MAX_VALUE
            }
        } else {
            Long.MAX_VALUE
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

                UnsafeHelper.memcpyFromArrToPtr(readBuf, 0, soundBuf!!.ptr + readCount, read)

                readCount += read

//                printdbg(this, "read $readCount/$totalSizeInBytes bytes (${readCount/(2*channels)}/$totalSizeInSamples samples)")
            }
        }
    }

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        assert(bufferL.size == bufferR.size)

        val byteSize = bufferL.size * bytesPerSample
        val bytesBuf = ByteArray(byteSize)
        val bytesRead = readBytes(bytesBuf)
        val samplesRead = bytesRead / bytesPerSample

        if (channels == 2) {
            for (i in 0 until samplesRead) {
                val sl = (bytesBuf[i * 4 + 0].toUint() or bytesBuf[i * 4 + 1].toUint().shl(8)).toShort()
                val sr = (bytesBuf[i * 4 + 2].toUint() or bytesBuf[i * 4 + 3].toUint().shl(8)).toShort()
                val fl = sl / 32767f
                val fr = sr / 32767f
                bufferL[i] = fl
                bufferR[i] = fr
            }
        }
        else if (channels == 1) {
            for (i in 0 until samplesRead) {
                val s = (bytesBuf[i * 2 + 0].toUint() or bytesBuf[i * 2 + 1].toUint().shl(8)).toShort()
                val f = s / 32767f
                bufferL[i] = f
                bufferR[i] = f
            }
        }
        else throw IllegalStateException("Unsupported channel count: $channels")

        return samplesRead
    }

    private fun readBytes(buffer: ByteArray): Int {
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

    private fun getWavFileSampleCountFromHandle(fh: FileHandle): Long {
        return try {
            val ais = AudioSystem.getAudioInputStream(BufferedInputStream(fh.read()))
            val r = ais.frameLength
            ais.close()
            r
        }
        catch (_: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getOggFileSampleCountFromHandle(fh: FileHandle): Long {
        return try {
            // VorbisFile requires a file path; use a temp file
            val tempFile = java.io.File.createTempFile("terrarum_ogg_", ".ogg")
            tempFile.deleteOnExit()
            tempFile.writeBytes(fh.readBytes())
            val vorbisFile = VorbisFile(tempFile.absolutePath)
            val r = vorbisFile.pcm_total(0)
            tempFile.delete()
            r
        }
        catch (_: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getMp3FileSampleCountFromHandle(fh: FileHandle): Long {
        return try {
            val input = BufferedInputStream(fh.read())
            val bs = Bitstream(input)

            var header = bs.readFrame()
            val rate = header.frequency()
            var totalSamples = 0L

            while (header != null) {
                totalSamples += (header.ms_per_frame() * rate / 1000).toLong()
                bs.closeFrame()
                header = bs.readFrame()
            }

            bs.close()
            input.close()

            totalSamples
        }
        catch (_: Throwable) {
            Long.MAX_VALUE
        }
    }

    private val identPath: String get() = file?.path ?: fileHandle?.path() ?: name

    override fun toString() = if (name.isEmpty()) (file?.nameWithoutExtension ?: fileHandle?.nameWithoutExtension() ?: "") else name

    override fun equals(other: Any?) = this.identPath == (other as MusicContainer).identPath
    fun equalInstance(other: Any?) = this.identPath == (other as MusicContainer).identPath && this.hash == (other as MusicContainer).hash

    override fun dispose() {
        gdxMusic.dispose()
        soundBuf?.destroy()
    }

    override fun makeCopy(): AudioBank {
        val new = if (file != null)
            MusicContainer(name, file, looping, false, samplingRateOverride, songFinishedHook)
        else
            MusicContainer(name, fileHandle!!, looping, false, samplingRateOverride, songFinishedHook)

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