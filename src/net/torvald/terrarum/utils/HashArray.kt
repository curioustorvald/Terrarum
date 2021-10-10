package net.torvald.terrarum.utils


import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PhysicalStatus
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import java.util.*

/**
 * Created by minjaesong on 2021-08-26.
 */
class HashArray<R>: HashMap<Long, R>() // primitives are working just fine tho

// Oh for the fucks sake fuck you everyone; json shit won't work with generics
class WiringGraphMap: HashMap<ItemID, GameWorld.WiringSimCell>()
class HashedFluidType: HashMap<BlockAddress, FluidType>()
class HashedWirings: HashMap<BlockAddress, GameWorld.WiringNode>()
class HashedWiringGraph: HashMap<BlockAddress, WiringGraphMap>()
class MetaModuleCSVPair: HashMap<String, ZipCodedStr>()
class PlayersLastStatus: HashMap<UUID, PlayerLastStatus>()
class PlayerLastStatus() {
    var physics = PhysicalStatus(); private set
    var inventory = ActorInventory(); private set
    var actorValue = ActorValue(); private set

    constructor(player: IngamePlayer) : this() {
        physics = PhysicalStatus(player)
        inventory = player.inventory
        actorValue = player.actorValue
    }
}
/**
 * @param doc plaintext
 *
 * Note: the content of the class is only encoded on serialisation; when the class is deserialised, this
 * class always holds plaintext.
 */
@JvmInline value class ZipCodedStr(val doc: String = "") {
    override fun toString() = doc
}
