|Range|Description|
|-----|-----------|
|0..65535|Tiles (65536 possible)|
|65536..131071|Walls (65536 possible)|
|131072..135167|Wires (4096 possible)|
|135168..0x0F_FFFF|Items (static) (1M possible)|
|0x10_0000..0x0FFF_FFFF|Items (dynamic\*) (267M possible)|
|0x1000_0000..0x7FFF_FFFF|Actors (1879M possible)|
|-2..-1048576|Virtual Tiles|
|-2147483648..-1048577 (all negative numbers)|Faction (2147M possible)|

* dynamic items have own properties that will persist through savegame.

Actors range in-depth

|Range|Description|
|-----|-----------|
|0x1000_0000..0x1FFF_FFFF|Rendered behind (e.g. tapestries)|
|0x2000_0000..0x4FFF_FFFF|Regular actors (e.g. almost all of them)|
|0x5000_0000..0x5FFF_FFFF|Special (e.g. weapon swung, bullets, dropped item, particles)|
|0x6000_0000..0x6FFF_FFFF|Rendered front (e.g. fake tile)|
|0x7000_0000..0x7FFF_FFFF|Rendered as screen overlay, not affected by light nor environment overlays|

Actor IDs are assigned in 256 groups, single actor can have 256 sub-actors