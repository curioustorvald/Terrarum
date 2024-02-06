package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.colourutil.HUSLColorConverter
import net.torvald.colourutil.OKLCh
import net.torvald.random.HQRNG
import net.torvald.random.XXHash32
import net.torvald.random.XXHash64
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.savegame.toHex
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.worlddrawer.toRGBA
import net.torvald.unicode.EMDASH

/**
 * Created by minjaesong on 2024-01-13.
 */
data class MusicDiscMetadata(val title: String, val author: String, val album: String)

object MusicDiscHelper {
    fun getMetadata(musicFile: FileHandle): MusicDiscMetadata {
        val musicdbFile = musicFile.sibling("_musicdb.json")
        val musicdb = JsonFetcher.invoke(musicdbFile.file())
        val propForThisFile = musicdb.get(musicFile.name())

        val artist = propForThisFile.get("artist").asString()
        val title = propForThisFile.get("title").asString()
        val album = propForThisFile.get("album").asString()

        return MusicDiscMetadata(title, artist, album)
    }
}

open class MusicDiscPrototype(originalID: ItemID, module: String, path: String) : ItemFileRef(originalID) {
    override var refPath = path
    override var refModuleName = module
    override val isDynamic = false
    @Transient override var ref = ModMgr.getFile(refModuleName, refPath)
    override var mediumIdentifier = "music_disc"

    init {
        val meta = MusicDiscHelper.getMetadata(getAsGdxFile())
        originalName = meta.title
        author = meta.author
        collection = meta.album
        name = meta.title
        nameSecondary = if (meta.author.isNotBlank() && meta.album.isNotBlank())
            "${meta.author} $EMDASH ${meta.album}"
        else
            "${meta.author.trim()}${meta.album.trim()}"
    }

    @Transient override val itemImage: TextureRegion = generateSprite()

    /**
     * Reads a channel-wise black and white image and tints it using HSLuv colour space
     */
    private fun generateSprite(): TextureRegion {
        val authorHash = XXHash64.hash(author.encodeToByteArray(), 54)
        val albumHash = XXHash64.hash(collection.encodeToByteArray(), 32)
        val nameHash = XXHash64.hash(name.encodeToByteArray(), 10)

        val authorRand = HQRNG(authorHash)
        val albumRand = HQRNG(albumHash)
        val nameRand = HQRNG(nameHash)

        val discLumSats = listOf(
            (20f to 0f), (20f to 1f),
            (50f to 90f),
            (35f to 60f), (30f to 80f),
        )

        val (discLum, discSat) = discLumSats.get(albumRand.nextInt(discLumSats.size))

        val discColour = floatArrayOf(
            albumRand.nextFloat() * 360f,
            discSat,
            discLum
        ) // HSLuv

        val labelColour = floatArrayOf(
            nameRand.nextFloat() * 360f,
            nameRand.nextFloat() * 20f + 75f,
            nameRand.nextFloat() * 30f + 60f
        ) // HSLuv

        val pixmap = Pixmap(ModMgr.getGdxFile("basegame", "items/record_sprite_base.tga"))

        // tint the pixmap
        for (y in 0 until pixmap.height) {
            for (x in 0 until pixmap.width) {
                val pixel = pixmap.getPixel(x, y) // RGBA

                if (pixel and 0xFF == 0xFF) {
                    // red part
                    if (pixel and 0xFF000000.toInt() != 0) {
                        val b = pixel.ushr(24).and(255).toFloat() / 255f

                        val B = discColour.copyOf().also {
                            it[2] *= b
                        }

                        val outCol = HUSLColorConverter.hsluvToRgb(B).let {
                            Color(it[0], it[1], it[2], 1f)
                        }

                        pixmap.drawPixel(x, y, outCol.toRGBA())
                    }
                    // green part
                    else if (pixel and 0x00FF0000.toInt() != 0) {
                        val b = pixel.ushr(16).and(255).toFloat() / 255f

                        val B = labelColour.copyOf().also {
                            it[2] *= b
                        }

                        val outCol = HUSLColorConverter.hsluvToRgb(B).let {
                            Color(it[0], it[1], it[2], 1f)
                        }

                        pixmap.drawPixel(x, y, outCol.toRGBA())
                    }

                }
            }
        }

        val ret = TextureRegion(Texture(pixmap))
        pixmap.dispose()

        return ret
    }
}

class MusicDisc01(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/3_over_4.ogg")
class MusicDisc02(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/fog.ogg")
class MusicDisc03(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/geometry.ogg")
class MusicDisc04(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/hdma.ogg")
class MusicDisc05(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/lucid_dream.ogg")
class MusicDisc06(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/railway.ogg")
class MusicDisc07(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/cyllindrical.ogg")
class MusicDisc08(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/welded.ogg")
class MusicDisc09(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/hangdrum.ogg")
