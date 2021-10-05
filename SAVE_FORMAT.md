## Introduction ##

On the main game, any player can access any generated worlds, and thus players data and worlds are saved separately.

The main game directory is composed of following directories:

```
.Terrarum
+ Players
  - <Player Name Here>, TVDA { actor JSON, optional textures and sprite defs }
+ Shared
  - <e.g. Disk GUID>, TEVD { * }
  - <this directory can have anything>
+ Worlds
  - <World Name Here>, TVDA { WriteWorld, actors JSON, chunk data, screenshot.tga.gz taken by the last player }
```

(TEVD stands for Terrarum Virtual Disk spec version 3, TVDA stands for spec version 254; both have MAGIC header of `TEVd`)

## Prerequisites ##

1. Player ID must not be strictly 9545698 (0x91A7E2)
    1. Use classname `net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer` to check
2. Each world has to be uniquely identifiable
    1. Use GUID, World Name, etc.
3. `ActorNowPlaying` must be drawn on top of other actors of same RenderOrder

## Goals ##

1. Allows multiple players share same world
2. Makes multiplayer possible
