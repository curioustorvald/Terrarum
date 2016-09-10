|Range|Description|
|-----|-----------|
|0..4095|Tiles|
|4096..32767|Items (static)|
|32768..16777215|Items (dynamic\*)|
|16777216..0x7FFFFFFF|Actors|
|0x80000000..0xFFFFFFFF (all negative numbers)|Faction|

* dynamic items can have their own properties that will persist through savegame.