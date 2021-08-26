package net.torvald.terrarum.utils


import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2021-08-26.
 */
class HashArray<R>: HashMap<Long, R>() // primitives are working just fine tho

// Oh for the fucks sake fuck you everyone; json shit won't work with generics
class WiringGraphMap: HashMap<ItemID, GameWorld.WiringSimCell>()
class HashedFluidType: HashMap<BlockAddress, FluidType>()
class HashedWirings: HashMap<BlockAddress, GameWorld.WiringNode>()
class HashedWiringGraph: HashMap<BlockAddress, WiringGraphMap>()
