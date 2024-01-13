package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-01-13.
 */
class MusicDisc01(originalID: ItemID) : ItemFileRef(originalID) {
    init {
        name = "Thousands of Shards"
        author = "Orstphone"
    }

    override var refPath = "audio/music/discs/01 Thousands of Shards.ogg"
    override var refModuleName = "basegame"
    override val isDynamic = false
    @Transient override var ref = ModMgr.getFile(refModuleName, refPath)
    override var mediumIdentifier = "music_disc"
}