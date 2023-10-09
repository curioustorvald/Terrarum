package net.torvald.terrarum.utils


import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
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
class HashedFluidType: HashMap<BlockAddress, ItemID>()
class HashedWirings: HashMap<BlockAddress, GameWorld.WiringNode>()
class HashedWiringGraph: HashMap<BlockAddress, WiringGraphMap>()
class MetaModuleCSVPair: HashMap<String, ZipCodedStr>()
class PlayersLastStatus: HashMap<String, PlayerLastStatus>() {
    operator fun get(uuid: UUID) = this[uuid.toString()]
    operator fun set(uuid: UUID, value: PlayerLastStatus) = this.set(uuid.toString(), value)
}
class PlayerLastStatus() {
    var physics = PhysicalStatus(); private set // mandatory
    var inventory: ActorInventory? = null; private set // optional (multiplayer only)
    var actorValue: ActorValue? = null; private set // optional (multiplayer only)

    constructor(player: IngamePlayer, isMultiplayer: Boolean) : this() {
        physics = PhysicalStatus(player)
        if (isMultiplayer) {
            inventory = player.inventory
            actorValue = player.actorValue
        }
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
