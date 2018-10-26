package net.torvald.terrarum.serialise

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilder
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import org.apache.commons.codec.digest.DigestUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object WriteWorldInfo {

    val META_MAGIC = "TESV".toByteArray(Charsets.UTF_8)
    val NULL = 0.toByte()

    val VERSION = 1
    val HASHED_FILES_COUNT = 3

    /**
     * TODO currently it'll dump the temporary file (tmp_worldinfo1) onto the disk and will return the temp file.
     *
     * @return File on success; `null` on failure
     */
    internal operator fun invoke(): List<File>? {
        val world = (Terrarum.ingame!!.world)

        val path = "${Terrarum.defaultSaveDir}/tmp_worldinfo"

        val infileList = arrayOf(
                ModMgr.getGdxFilesFromEveryMod("blocks/blocks.csv"),
                ModMgr.getGdxFilesFromEveryMod("items/items.csv"),
                ModMgr.getGdxFilesFromEveryMod("materials/materials.csv")
        )

        val metaFile = File(path + "0")

        val outFiles = ArrayList<File>()
        outFiles.add(metaFile)

        val worldInfoHash = ArrayList<ByteArray>() // hash of worldinfo1-3
        // try to write worldinfo1-3

        for (filenum in 1..HASHED_FILES_COUNT) {
            val outFile = File(path + filenum.toString())
            if (outFile.exists()) outFile.delete()
            outFile.createNewFile()

            val outputStream = BufferedOutputStream(FileOutputStream(outFile), 256)
            val infile = infileList[filenum - 1]

            infile.forEach {
                val readBytes = it.readBytes()
                outputStream.write(readBytes)
            }

            outputStream.flush()
            outputStream.close()


            outFiles.add(outFile)


            worldInfoHash.add(DigestUtils.sha256(FileInputStream(outFile)))
        }


        // compose save meta (actual writing part)
        val metaOut = BufferedOutputStream(FileOutputStream(metaFile), 256)


        metaOut.write(META_MAGIC)
        metaOut.write(VERSION)
        metaOut.write(HASHED_FILES_COUNT)

        // world name
        val worldNameBytes = world.worldName.toByteArray(Charsets.UTF_8)
        metaOut.write(worldNameBytes)
        if (worldNameBytes.last() != NULL) metaOut.write(NULL.toInt())

        // terrain seed
        metaOut.write(world.generatorSeed.toLittle())

        // randomiser seed
        metaOut.write(RoguelikeRandomiser.RNG.state0.toLittle())
        metaOut.write(RoguelikeRandomiser.RNG.state1.toLittle())

        // weather seed
        metaOut.write(WeatherMixer.RNG.state0.toLittle())
        metaOut.write(WeatherMixer.RNG.state1.toLittle())

        // reference ID of the player
        metaOut.write(Terrarum.PLAYER_REF_ID.toLittle())

        // ingame time_t
        metaOut.write((world as GameWorldExtension).time.TIME_T.toLittle())

        // creation time (real world time)
        metaOut.write(world.creationTime.toLittle48())

        // time at save (real world time)
        val timeNow = System.currentTimeMillis() / 1000L
        metaOut.write(timeNow.toLittle48())

        // get playtime and save it
        val timeToAdd = (timeNow - world.loadTime).toInt()
        metaOut.write((world.totalPlayTime + timeToAdd).toLittle())
        world.lastPlayTime = timeNow
        world.totalPlayTime += timeToAdd

        // SHA256SUM of worldinfo1-3
        worldInfoHash.forEach {
            metaOut.write(it)
        }


        // more data goes here //


        metaOut.flush()
        metaOut.close()



        return outFiles.toList()
    }

}