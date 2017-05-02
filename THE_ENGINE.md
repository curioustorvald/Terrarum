DISCLAIMER: this is marketing-team stuffs. Features are still in-development and may not be available.

### TODO: cleanup

The Engine is custom built to suit the needs most. Following list is our aims:

* Reasonable performance on vast world, on JVM
    - At least better than Notch's codes...

* Thread scalability
    - Multithreaded environments are commonplace in this era; even in the Intel Pentium G. We aim to introduce performance boost by supporting multithreads, without the limitation of threads count (hopefully).

* Lightweight physics solver, as light as we need
    - This game is not exactly a physics toy, albeit some could add the fun.
    - Currently implemented: gravity (NOT artificial), friction, buoyancy (WIP), air/fluid density and termination velocity
    - Planned: artificial gravitation, wind, joints

* Cellular Automata fluid simulation
    - It should be enough — period.


Because of this, we just couldn't use solutions out there. For example, Tiled is too slow<sup>[citation needed]</sup> and has large memory footprint<sup>[citation needed]</sup> for our vast world; we can't use JBox2d nor Dyn4j as we don't need any sophisticated physics simulation, and can't use them anyway as we have literally _millions_ of rigid bodies (tiles) and actors. (Trivia: we _do_ use Dyn4j's Vector2 class)



## General-Purpose Side-Scroller Game Maker

The Engines is specialised in side-scrolling platformers with controls specific to 

The Engine is designed with modularity in mind — every game runs upon the Engine is individual module(s), and you can write your own module to alter the original game (module), essentially a game "mod" (put intended).


### Actors

The Engine allows Actors (NPCs) to have AIs


### Sprites

The Engine allows up to 64 sprite layers that are either software- or hardware-blended


### Multilingual Support

The Engine can support any arbitrary language, as long as there is fonts for them. A default font is shipped with the Engine, and it already supports 20+ languages.



### Utilities

The Engine also comes with various utilities to help the game making bit easier. They include:

- Savegame generator/loader
- Password system (old games used password system to load/save the gameplay)
- CSV loader (e.g. Item properties)
- Json loader/saver
- Text cipher

