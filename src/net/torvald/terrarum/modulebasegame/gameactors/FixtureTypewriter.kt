package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2022-08-26.
 */
class FixtureTypewriter : FixtureBase {

    var typewriterKeymapName = "us_qwerty" // used to control the keyboard input behaviour
        private set

    private var carriagePosition = 0

    // constructor used when new typewriter is created
    constructor(keymapName: String) : this() {
        typewriterKeymapName = keymapName
    }

    // constructor used when the game loads from the savefile
    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            nameFun = { Lang["ITEM_TYPEWRITER"] }
    ) {

        density = 2000.0

        setHitboxDimension(16, 16, 8, 0)

        makeNewSprite(FixtureBase.getSpritesheet("basegame", "sprites/fixtures/typewriter.tga", 32, 16)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 3.6
    }

    companion object {
        const val TYPEWRITER_COLUMNS = 64
    }

}