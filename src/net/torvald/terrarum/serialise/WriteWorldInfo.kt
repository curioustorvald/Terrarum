package net.torvald.terrarum.serialise

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64InputStream
import org.apache.commons.codec.digest.DigestUtils

object WriteWorldInfo {

    val META_MAGIC = "TESV".toByteArray(Charsets.UTF_8)
    val NULL = 0.toByte()

    val VERSION = 1
    val HASHED_FILES_COUNT = 3

    /**
     * TODO currently it'll dump the temporary file (tmp_worldinfo1) onto the disk and will return the temp file.
     *
     * @return List of ByteArray64, worldinfo0..worldinfo3; `null` on failure
     */
    internal operator fun invoke(world: GameWorld): List<ByteArray64>? {

        //val path = "${AppLoader.defaultSaveDir}/tmp_worldinfo"

        val infileList = arrayOf(
                ModMgr.getGdxFilesFromEveryMod("blocks/blocks.csv"),
                ModMgr.getGdxFilesFromEveryMod("items/items.csv"),
                ModMgr.getGdxFilesFromEveryMod("materials/materials.csv")
        )

        val outFiles = ArrayList<ByteArray64>() // for worldinfo1-3 only
        val worldInfoHash = ArrayList<ByteArray>() // hash of worldinfo1-3
        // try to write worldinfo1-3

        for (filenum in 1..HASHED_FILES_COUNT) {
            val outputStream = ByteArray64GrowableOutputStream()
            val infile = infileList[filenum - 1]

            infile.forEach {
                outputStream.write("## from file: ${it.nameWithoutExtension()} ##############################\n".toByteArray())
                val readBytes = it.readBytes()
                outputStream.write(readBytes)
                outputStream.write("\n".toByteArray())
            }

            outputStream.flush()
            outputStream.close()


            outFiles.add(outputStream.toByteArray64())


            worldInfoHash.add(DigestUtils.sha256(ByteArray64InputStream(outputStream.toByteArray64())))
        }


        // compose save meta (actual writing part)
        val metaOut = ByteArray64GrowableOutputStream()


        metaOut.write(META_MAGIC)
        metaOut.write(VERSION)
        metaOut.write(HASHED_FILES_COUNT)

        // world name
        val worldNameBytes = world.worldName.toByteArray(Charsets.UTF_8)
        //metaOut.write(worldNameBytes)
        worldNameBytes.forEach {
            if (it != 0.toByte()) metaOut.write(it.toInt())
        }
        metaOut.write(NULL.toInt())

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
        metaOut.write(world.creationTime.toULittle48())

        // time at save (real world time)
        val timeNow = System.currentTimeMillis() / 1000L
        metaOut.write(timeNow.toULittle48())

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



        return listOf(metaOut.toByteArray64()) + outFiles.toList()
    }

}