## Introduction

On the main game, any player can access any generated worlds, and thus players data and worlds are saved separately.

The main game directory is composed of following directories:

```
.Terrarum
+ Players
  - "${UUID}", TVDA {
      [-1] player JSON,
      [-2] spritedef,
      [-3] !optional! spritedef-glow,
      [-4] loadOrder.txt
      [-1025] !optional! sprite-bodypart-name-to-entry-number-map.properties,
      [-1026] !optional! spriteglow-bodypart-name-to-entry-number-map.properties,
      [1+] !optional! bodyparts tga.gz
    }
    *if file -1025 is not there, read bodyparts from assets directory
    *optionally encrypt the files other than -1
    *disk name is player's name encoded in UTF-8
+ Shared
  - <e.g. Disk GUID>, TEVD { * }
  - <this directory can have anything>
+ Worlds
  - "${UUID}", TVDA {
      [-1] world JSON with Player Data,
      [actorID] actors (mainly fixtures) JSON,
      [0x1_0000_0000L or (layerNumber shl 24) or chunkNumber] chunk data,
      [-2] screenshot.tga.gz taken by the last player
      [-4] loadOrder.txt
    }
    *disk name is world's name encoded in UTF-8
```

(TEVD stands for Terrarum Virtual Disk spec version 3, TVDA stands for spec version 254; both have MAGIC header of `TEVd`)

Do not rely on filename to look for a world; players can change the filename

## Handling The Player Data

Some of the "player assets" are stored to the world, such assets include:
- Physical Status (last position and size as in scale)
- Inventory (instance of ActorInventory)
- Actorvalues (only on Multiplayer)

### Loading Procedure

1. Load the Actor completely first
2. Load the World
3. Overwrite player data with the World's 
   
If the World has the Actorvalue, World's value will be used; otherwise use incoming Player's

Multiplayer world will initialise Actorvalue pool using incoming value -- or they may choose to use
their own Actorvalue called "gamerules" to either implement their own "gamemode" or prevent cheating)

For Singleplayer, only the xy-position is saved to the world for later load.

Worlds must overwrite new Actor's position to make them spawn in right place.

### Remarks

Making `inventory` transient is impossible as it would render Storage Chests unusable.

## Prerequisites

1. Player ID must not be strictly 9545698 (0x91A7E2)
    1. Use classname `net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer` to check
2. Each World and Player has to be uniquely identifiable via GUID
3. `ActorNowPlaying` must be drawn on top of other actors of same RenderOrder

## To-dos After the Initial Implementation

1. Modify Savegame Crackers and Disk Crackers to work with the new scheme
2. Create Player Creator Tool for avatar-makers

## Goals

1. Allow multiple players share the same world
2. Make multiplayer possible
3. Make Players distributable (like VRChat avatars)

## Quirks with Terrarum's custom TVDA implementation

- Subdirectory is not allowed -- every file must be on the root directory
- The entry for the root directory is there (only to satisfy the format constraints), but it's basically meaningless -- contents of the entry are undefined
- Programmers are encouraged to scan the entire disk to get the file listings
