package net.torvald.terrarum.modulecomputers.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2025-03-30.
 */
class FixtureComputerConsole : Electric {

    @Transient override val spawnNeedsStableFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_COMPUTER_CONSOLE"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_operator_terminal.tga")
        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 2*TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }
    }
}

/**
 * Created by minjaesong on 2025-03-30.
 */
class FixtureComputerProcessor : Electric {

    @Transient override val spawnNeedsStableFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 3),
        nameFun = { Lang["ITEM_COMPUTER_PROCESSOR"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_cpu.tga")
        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 3*TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }
    }
}