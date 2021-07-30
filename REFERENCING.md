## Prefix-ID Referencing

Every blocks and items have Prefix-ID Referencing scheme, which is defined as follows:

```<Prefix>@<Modname>:<Integer ID>```

where Prefix is predefined (see below), Integer ID is arbitrarily chosen within a domain.

### Prefixes
|Name|Description|
|----|-----------|
|wall|Wall, only used by the Inventory to differentiate walls from blocks (therefore wall shares same "ID Space" with blocks)|
|item|Item (Static)|
|wire|Wires|

Notes: 
- BlockCodex and ItemCodex will not store prefix part of the ID, as blocks and walls are identical in properties
- Wires and Fluids use the same "ID Space" as the tiles; they just happened to exclusive to their own layers.
  This simplifies many things e.g. only one TileID-to-AtlasTileNumber map is needed and the renderer will
  greatly benefit from it.

### Predefined Modnames

|Name|Description|
|----|-----------|
|dyn|Dynamic Item|
|actor|Actor As an Item. Integer ID is identical to the actor's Reference ID|
|virt|Virtual Tile Number|

### Integer ID Domains

|Range|Description|
|-----|-----------|
|1..2147483647|Integer ID for dynamic items|
|0x1000_0000..0x7FFF_FFFF|Reference ID for Actors (1879M possible)|
|1..2147483647|Integer ID for virtual tiles|

* dynamic items have own properties that will persist through savegame.

Actor range in-depth

|Range|Description|
|-----|-----------|
|0x1000_0000..0x1FFF_FFFF|Rendered behind (e.g. tapestries)|
|0x2000_0000..0x4FFF_FFFF|Regular actors (e.g. almost all of them)|
|0x5000_0000..0x5FFF_FFFF|Special (e.g. weapon swung, bullets, dropped item, particles)|
|0x6000_0000..0x6EFF_FFFF|Rendered front (e.g. fake tile)|
|0x6F00_0000..0x6FFE_FFFF|unassigned|
|0x6FFF_0000..0x6FFF_FFFF|Rendered front--wires|
|0x7000_0000..0x7FFF_FFFF|Rendered as screen overlay, not affected by light nor environment overlays|

Actor IDs are assigned in 256 groups, single actor can have 256 sub-actors
