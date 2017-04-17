## Aperçu ##

This project is to create a side-view flatformer game, an attempt to create friendlier *Dwarf Fortress* adventurer mode, with more rogue-like stuff such as permanent death, randomness and *lots of fun*.

Backend used is LWJGL/Slick2D, frontend — the Engine — is custom-built. The repository contains both frontend and the actual game, as well as documentation for the backends.

This project mainly uses Kotlin as a main language and Python/Lua for tools.

Documentations and resources for work (such as .psd) are also included in the repository. You will need Mac computer to read and edit documentations in .gcx and .numbers format. (we're planning to change them)

Any contribution in this project must be made sorely in English, Korean part is just there for my Korean friend; so be sure to use English in codes, comments, etc.

## Setup ##

* Requirements:
    - JDK 8 or higher
    - Working copy of IntelliJ IDEA from JetBrains s.r.o., community edition is okay to use.
  
* Required libraries are included in the repository.


## The Engine ##

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


## Contribution guidelines ##

Please refer to [```CONTRIBUTING.md```]().

## Who do I talk to? ##

* Repo owner or admin
* Other community or team contact


## Tags ##

* Rogue-like
* Adventure
* Procedural Generation
* Open World
* Sandbox
* Survival
* (Crafting)
* 2D
* Singleplayer
* Platformer
* (Atmospheric)
* Indie
* Pixel Graphics


## Disclaim ##

Just in case, use this software at your own risk.


## Copyright ##

Copyright 2015-2016 Torvald (skyhi14 _at_ icloud _dot_ com). All rights reserved. This game is proprietary until things are completed enough to be playable.


## 개요 ##

이 변변한 이름 없는 프로젝트는 사이드뷰 발판 게임 형식으로 더 친절한 〈드워프 포트리스〉의 모험가 모드를 지향하는 게임 제작 프로젝트입니다. 영구 사망, 무작위성, __넘쳐나는 재미__와 같이 ‘로그라이크’스러운 요소를 지닙니다.

이 프로젝트는 주 언어로 코틀린을 사용하며 파이선·루아 등으로 작성된 툴을 이용합니다.

개발 문서와 작업용 리소스(psd 등) 또한 이 저장소에 포함되어 있습니다. gcx와 numbers 형식으로 된 문서를 읽고 수정하기 위해 맥 컴퓨터가 필요할 수 있습니다.

이 프로젝트에 대한 기여는 영어로 이루어져야 합니다. 따라서 코드나 주석 등을 작성할 때는 영어를 사용해 주십시오.
