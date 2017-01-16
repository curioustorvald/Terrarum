|Range|Description|
|-----|-----------|
|0..4095|Tiles|
|4096..32767|Items (static)|
|32768..1048575|Items (dynamic\*)|
|1048576..0x7FFF_FFFF|Actors|
|0x8000_0000..0xFFFF_FFFF (all negative numbers)|Faction|

* dynamic items can have their own properties that will persist through savegame.

Actors range in-depth

|Range|Description|
|-----|-----------|
|1048576..0x0FFF_FFFF|Rendered behind (e.g. tapestries)
|0x1000_0000..0x5FFF_FFFF|Regular actors (e.g. almost all of them)
|0x6000_0000..0x6FFF_FFFF|Special (e.g. weapon swung, bullets, dropped item, particles)
|0x7000_0000..0x7FFF_FFFF|Rendered front (e.g. fake tile)