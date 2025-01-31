package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FileRefItemPrimaryUseHandler
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.gameitems.ItemPlainDocument
import net.torvald.terrarum.serialise.Common
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2022-08-26.
 */
class FixtureTypewriter : FixtureBase {

    var typewriterKeymapName = "us_qwerty" // used to control the keyboard input behaviour
        private set

    private var carriagePosition = 0
    private val textBuffer = Array(TYPEWRITER_ROWS) { "" }

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
            it.setRowsAndFrames(12,1)
        }

        actorValue[AVKey.BASEMASS] = 3.6
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        (sprite as SheetSpriteAnimation).currentRow = 1 + (carriagePosition.toFloat() / TYPEWRITER_COLUMNS * 10).roundToInt()
    }

    companion object {
        const val TYPEWRITER_COLUMNS = 64
        const val TYPEWRITER_ROWS = 30
    }

    override fun onInteract(mx: Double, my: Double) {
        printdbg(this, "Typewriter onInteract")
        // test spit out ItemFileRef item

        val textFileContents = """This is a test of creating ItemFileRef item in-game.
            |Caller: ${this.javaClass.canonicalName}
        """.trimMargin()

        val newUUID = UUID.randomUUID()
        Terrarum.getSharedSaveFiledesc(newUUID.toString()).let {
            printdbg(this, "FilePath: ${it.path}")
            it.writeText(textFileContents, Common.CHARSET)
        }

        // DON'T create an anonymous class here: they won't be serialised
        INGAME.actorNowPlaying?.inventory?.let { inventory ->
            val newItem = ItemPlainDocument("item@basegame:33536").makeDynamic(inventory).also { it0 ->
                val it = it0 as ItemFileRef

                it.refIsShared = true
                it.uuid = newUUID
                if (INGAME.actorNowPlaying is IngamePlayer)
                    it.authorUUID = (INGAME.actorNowPlaying as IngamePlayer).uuid
                it.refPath = newUUID.toString()
                it.mediumIdentifier = "text/typewriter"
                it.useItemHandler = "net.torvald.terrarum.modulebasegame.gameactors.TestLeafletPrimaryUseHandler"
                it.name = "Testification"
                it.author = INGAME.actorNowPlaying?.actorValue?.getAsString(AVKey.NAME) ?: ""
            }

            inventory.add(newItem)
        }
    }
}

internal class TestLeafletPrimaryUseHandler : FileRefItemPrimaryUseHandler {
    override fun use(item: ItemFileRef): Long {
        println(item.getAsFile().readText(Common.CHARSET))
        return 0L
    }
}