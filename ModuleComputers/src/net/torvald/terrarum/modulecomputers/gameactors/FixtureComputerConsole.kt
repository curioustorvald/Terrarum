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

        setWireSinkAt(0, 1, "io_bus")
        setWireSinkAt(1, 1, "power_low")
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

        setWireSinkAt(0, 1, "serial")
        setWireSinkAt(1, 1, "serial")
        setWireSinkAt(0, 2, "io_bus")
        setWireSinkAt(1, 2, "power_low")
    }
}

/**
 * Created by minjaesong on 2025-04-01.
 */
class FixtureNetworkInterface : Electric {

    @Transient override val spawnNeedsStableFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 3),
        nameFun = { Lang["ITEM_NETWORK_INTERFACE"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_network_interface.tga")
        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 3*TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        setWireSinkAt(0, 0, "token_ring")
        setWireSinkAt(1, 0, "token_ring")
        setWireSinkAt(0, 2, "serial")
        setWireSinkAt(1, 2, "power_low")
    }
}


/**
 * Created by minjaesong on 2025-04-01.
 */
class FixtureNetworkBridge : Electric {

    @Transient override val spawnNeedsStableFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 3),
        nameFun = { Lang["ITEM_NETWORK_BRIDGE"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_network_bridge.tga")
        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TILE_SIZE, 3*TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        setWireSinkAt(0, 0, "token_ring")
        setWireSinkAt(1, 0, "token_ring")
        setWireSinkAt(0, 1, "token_ring")
        setWireSinkAt(1, 1, "token_ring")
        setWireSinkAt(1, 2, "power_low")
    }
}