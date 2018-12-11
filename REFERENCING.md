|Range|Description|
|-----|-----------|
|0..4095|Tiles (4096 possible)|
|4096..8191|Walls (4096 possible)|
|8192..8447|Wires (256 possible)|
|8448..32767|Items (static) (24320 possible)|
|32768..0x0FFF_FFFF|Items (dynamic\*) (268M possible)|
|0x1000_0000..0x7FFF_FFFF|Actors (1879M possible)|
|-2147483648..-1 (all negative numbers)|Faction (2147M possible)|

* dynamic items have own properties that will persist through savegame.

Actors range in-depth

|Range|Description|
|-----|-----------|
|0x1000_0000..0x1FFF_FFFF|Rendered behind (e.g. tapestries)|
|0x2000_0000..0x4FFF_FFFF|Regular actors (e.g. almost all of them)|
|0x5000_0000..0x5FFF_FFFF|Special (e.g. weapon swung, bullets, dropped item, particles)|
|0x6000_0000..0x6FFF_FFFF|Rendered front (e.g. fake tile)|
|0x7000_0000..0x7FFF_FFFF|Rendered as screen overlay, not affected by light nor environment overlays|