## Introduction ##

On the main game, any player can access any generated worlds, and thus players data and worlds are saved separately.

The main game directory is composed of following directories:

```
.Terrarum
+ Players
  - <Player Name Here>, TVDA { (-1) player JSON, (-2) optional spritedef, (-3) optional spritedef-glow, (-1025) sprite-bodypart-name-to-entry-number-map.properties, (1+) optional textures and sprite defs }
+ Shared
  - <e.g. Disk GUID>, TEVD { * }
  - <this directory can have anything>
+ Worlds
  - <World Name Here>, TVDA { WriteWorld, actors (mainly fixtures) JSON, chunk data, screenshot.tga.gz taken by the last player }
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
