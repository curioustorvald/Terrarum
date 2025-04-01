package net.torvald.terrarum.modulecomputers.gameitems

import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase

/**
 * Created by minjaesong on 2025-03-30.
 */
class ItemComputerConsole(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureComputerConsole") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 80.0
    override val canBeDynamic = false
    override val materialId = "STAL"
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_operator_terminal.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_COMPUTER_CONSOLE"

}

/**
 * Created by minjaesong on 2025-03-30.
 */
class ItemComputerProcessor(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureComputerProcessor") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 200.0
    override val canBeDynamic = false
    override val materialId = "STAL"
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_cpu.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_COMPUTER_PROCESSOR"

}

/**
 * Created by minjaesong on 2025-04-01.
 */
class ItemNetworkInterface(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureNetworkInterface") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 200.0
    override val canBeDynamic = false
    override val materialId = "STAL"
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_network_interface.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_NETWORK_INTERFACE"

}

/**
 * Created by minjaesong on 2025-04-01.
 */
class ItemNetworkBridge(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureNetworkBridge") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 200.0
    override val canBeDynamic = false
    override val materialId = "STAL"
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_network_bridge.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_NETWORK_BRIDGE"

}

/**
 * Created by minjaesong on 2025-04-01.
 */
class ItemMemoryCabinet(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureMemoryCabinet") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 80.0
    override val canBeDynamic = false
    override val materialId = "STAL"
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("dwarventech", "sprites/fixtures/computer_memory_stack_1.tga")
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_MEMORY_CABINET"

}