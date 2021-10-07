## Introduction ##

On the main game, any player can access any generated worlds, and thus players data and worlds are saved separately.

The main game directory is composed of following directories:

```
.Terrarum
+ Players
  - <Player Name Here>, TVDA { (-1) player JSON, (-2) spritedef, (-3) optional spritedef-glow, (-1025) sprite-bodypart-name-to-entry-number-map.properties, (1+) optional bodyparts tga.gz }
    if file -1025 is not there, read bodyparts from assets directory
    optionally encrypt the files other than -1
+ Shared
  - <e.g. Disk GUID>, TEVD { * }
  - <this directory can have anything>
+ Worlds
  - <World Name Here>, TVDA { (-1) WriteWorld, (actorID) actors (mainly fixtures) JSON, (0x1_0000_0000 + (layerNumber << 24) + chunkNumber) chunk data, (-2) screenshot.tga.gz taken by the last player }
```

(TEVD stands for Terrarum Virtual Disk spec version 3, TVDA stands for spec version 254; both have MAGIC header of `TEVd`)

## Prerequisites ##

1. Player ID must not be strictly 9545698 (0x91A7E2)
    1. Use classname `net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer` to check
2. Each World and Player has to be uniquely identifiable via GUID
3. `ActorNowPlaying` must be drawn on top of other actors of same RenderOrder

## To-dos After the Initial Implementation ##

1. Modify Savegame Crackers and Disk Crackers to work with the new scheme
2. Create Player Creator Tool for avatar-makers

## Goals ##

1. Allow multiple players share the same world
2. Make multiplayer possible
3. Make Players distributable (like VRChat avatars)
