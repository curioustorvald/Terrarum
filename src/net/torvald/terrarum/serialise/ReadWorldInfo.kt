package net.torvald.terrarum.serialise

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskSkimmer.Companion.read
import java.io.File
import java.io.FileInputStream
import java.util.*


object ReadWorldInfo {

    // FIXME UNTESTED

    internal operator fun invoke(file: File): SaveMetaData {

        val magic = ByteArray(4)
        val worldNameUTF8 = ArrayList<Byte>()

        val fis = FileInputStream(file)

        fis.read(magic)
        if (!Arrays.equals(magic, WriteWorldInfo.META_MAGIC)) {
            throw IllegalArgumentException("File not a Save Meta")
        }


        val descVersion = fis.read(1) // 0-127
        val numberOfHashes = fis.read() // 0-127


        var byteRead = fis.read()
        while (byteRead != 0) {
            if (byteRead == -1)
                throw InternalError("Unexpected EOF")

            worldNameUTF8.add(byteRead.toByte())
            byteRead = fis.read()
        }

        return SaveMetaData(
                String(worldNameUTF8.toByteArray(), Charsets.UTF_8),
                fis.read(8).toLittleLong(), // terrain seed
                fis.read(8).toLittleLong(), // rng s0
                fis.read(8).toLittleLong(), // rng s1
                fis.read(8).toLittleLong(), // weather s0
                fis.read(8).toLittleLong(), // weather s1
                fis.read(4).toLittleInt(), // player id
                fis.read(8).toLittleLong(), // world TIME_T
                fis.read(6).toLittleLong(), // creation time
                fis.read(6).toLittleLong(), // last play time
                fis.read(4).toLittleInt(), // total time wasted
                fis.read(32), // sha256sum worldinfo1
                fis.read(32), // sha256sum worldinfo2
                fis.read(32) // sha256sum worldinfo3
        )
    }


    data class SaveMetaData(
            val worldName: String,
            val terrainSeed: Long,
            val rngS0: Long,
            val rngS1: Long,
            val weatherS0: Long,
            val weatherS1: Long,
            val playerID: Int,
            val timeNow: Long,
            val creationTime: Long,
            val lastPlayTime: Long,
            val totalPlayTime: Int,
            val worldinfo1Hash: ByteArray,
            val worldInfo2Hash: ByteArray,
            val worldInfo3Hash: ByteArray,

            // gzipped TGA in meta
            val thumbnail: Texture = Texture(2, 2, Pixmap.Format.RGBA8888),
            // skim through the virtualdisk entries
            val worldCount: Int = 1,
            // read from the entry file
            val playerName: String = "Savegame",
            val playerWallet: Int = 0
    )
}