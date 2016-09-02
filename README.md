## Aperçu ##

This unnamed project is to create a side-view flatformer game, an attempt to create friendlier *Dwarf Fortress* adventurer mode, with more rogue-like stuff such as permanent death, randomness and *lots of fun*.

This project mainly uses Kotlin and Python/Lua/etc. for tools.

Documentations and resources for work (such as .psd) are also included in the repository. You will need Mac computer to read and edit documentations in .gcx and .numbers format.

Any contribution in this project must be made sorely in English, so be sure to use English in codes, comments, etc.

## Setup ##

* Requirements:
    - JDK 8 or higher
    - Working copy of IntelliJ IDEA from JetBrains s.r.o., community edition is okay to use.
  
* Required libraries are included in the repository.


## Contribution guidelines ##

### Contributing code ###

* Writing tests
* Code review
* Guidelines
    - Well-documented. (comments, people, comments!)


### Contributing translations ###

* Writing text
  You will need to fiddle with .json files in ./res/locales/<Language code>
* Languagus with apparent grammatical gender
  Any gender discrimination should *not* exist in this game, so please choose vocabularies that is gender-neutral. If such behaviour is not possible in the target language, please use male gender, but try your best to avoid the situation.

Note: Right-to-left languages (arabic, hebrew, etc.) are not supported.


### Contributing artworks ###

* RGB we mean is always sRGB, _pure white_ we say is always D65 (6 500 K). If you have a monitor calibration device, this is our desired target.
* Master material for picture (e.g. psd) can be either RGB/8 or Lab/16.
* To comply with the game's art style, only the 12-bit colours must be used (each channel: 0x00, 0x11, 0x22 .. 0xEE, 0xFF), though there is some exception.
* Every final audio must be in OGG/Vorbis, q 10.


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

Copyright 2015-2016 Torvald (skyhi14 _at_ icloud _dot_ com). All rights reserved.
This game is proprietary (yet).


## 개요 ##

이 변변한 이름 없는 프로젝트는 사이드뷰 발판 게임 형식으로 더 친절한 〈드워프 포트리스〉의 모험가 모드를 지향하는 게임 제작 프로젝트입니다. 영구 사망, 무작위성, __넘쳐나는 재미__와 같이 ‘로그라이크’스러운 요소를 지닙니다.

이 프로젝트는 주 언어로 코틀린을 사용하며 파이선·루아 등으로 작성된 툴을 이용합니다.

개발 문서와 작업용 리소스(psd 등) 또한 이 저장소에 포함되어 있습니다. gcx와 numbers 형식으로 된 문서를 읽고 수정하기 위해 맥 컴퓨터가 필요할 수 있습니다.

이 프로젝트에 대한 기여는 영어로 이루어져야 합니다. 따라서 코드나 주석 등을 작성할 때는 영어를 사용해 주십시오.