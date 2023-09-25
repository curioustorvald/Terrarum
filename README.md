## Aperçu ##

This project is to create a modular game engine that accommodates a 2D side-scrolling tilemap platformer, and a game that runs on top of it.

The project is divided into two parts: **Terrarum the Game Engine** and **Terrarum the actual game**.

## Terrarum the Game Engine ##

This game engine aims to provide following features:

- Tiled lighting simulation with transmittance sim in full RGB and UV for fluorescence
- Corner Occlusion
- 2D Skeletal Sprite
- Built-in Mod support
- Simple AABB Physics
- Fluid simulation based on Cellular Automata
- Built-in multilingual font — please refer to [its own Repository](https://github.com/curioustorvald/Terrarum-sans-bitmap)

## Terrarum the Actual Game ##

*Terrarum* is a side-view tilemap platformer-adventure-sandbox game.

## Player Setup ##

### System Requirements ###
Requires 64 bit processor and operation system.
| | Minimum | Recommended |
|---|---|---|
|OS|Windows 7/macOS Sierra/Ubuntu 16.04|Windows 10/macOS Big Sur/Linux with Kernel 5.4|
|CPU|AMD Phenom X4 9600/Intel Core 2 Duo E8400|AMD Ryzen 5 1500X/Intel Core i7-4770K/Apple M1|
|Memory|4 GB RAM|8 GB RAM|
|OpenGL|3.3|4.0|
|Graphics|GeForce 9600 GT|Anything that supports OpenGL 4.0|
|Storage|2 GB available|2 GB available but faster|

- Playing the game on the Minimum Requirement is ill advised: framerate will be sub-20 and the world generation will take more than 10 minutes

## Dev Setup ##

- Requirements:
    - JDK 17 or higher
    - IntelliJ IDEA Community Edition

Required libraries are included in the repository.

## Copyright ##

Please refer to [```COPYING.md```](COPYING.md) but it's mostly GPL 3.
