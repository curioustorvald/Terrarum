package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.JsonFetcher

/**
 * Created by minjaesong on 2024-01-13.
 */
data class MusicDiscMetadata(val title: String, val author: String)

object MusicDiscHelper {
    fun getMetadata(musicFile: FileHandle): MusicDiscMetadata {
        val musicdbFile = musicFile.sibling("_musicdb.json")
        val musicdb = JsonFetcher.invoke(musicdbFile.file())
        val propForThisFile = musicdb.get(musicFile.name())

        val artist = propForThisFile.get("artist").asString()
        val title = propForThisFile.get("title").asString()

        return MusicDiscMetadata(title, artist)
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
        name = meta.title
        author = meta.author
    }
}

class MusicDisc01(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/01 Thousands of Shards.ogg")
class MusicDisc02(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/02 Glitter.ogg")
class MusicDisc03(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/03 Digital Foliage.ogg")
class MusicDisc04(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/04 HDMA.ogg")
class MusicDisc05(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/05 Welded.ogg")
class MusicDisc06(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/06 Cyllindrical.ogg")
class MusicDisc07(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/07 Plastic Pop.ogg")
class MusicDisc08(originalID: ItemID) : MusicDiscPrototype(originalID, "basegame", "audio/music/discs/08 Gateway 509.ogg")
