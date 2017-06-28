
##  Colouring  ##

Artificial: Use 4096

* Colouring of potion
    - Randomised, roguelike fashion
    - Choose Col(RGB) from set of finite cards:
        255, 255, 128, 128, 0, 0
    - MULTIPLY blend chosen colour with white texture


### Colour keys ###

Things are colour-keyed so that players would get the idea by just a glance.

- Amber: Inactivated, TBA??
- Cyan-blue: Activated, Energy weapon?
- Purple: Magic
    - Application: spell tomes/scrolls written with Magic Description Language
- Phosphor Green: 
- Cherry red (#F04): Health (using this to help with deuterans)


NOTE: cyan is a tricky colour for deuterans; will be inextinguishable between greys!


##  Making sprite  ##

* Layers
    - (Optional) Glow
    - (Optional) Hair foreground
    - Right arm dress
    - Right arm body
    - Dress
    - Boots
    - Body
    - (Optional) Hair accessory
    - Hair
    - Head
    - Left arm dress
    - Left arm body
    - (Optional) SFX

* Size
    - Regular sprite 'height' (hitbox height) : 40 px
        - Apparent height may vary


##  Chargen  ##

* Select hair, colours, then compile them into single spritesheet

* NO gender distinction, but have masculine/neutral/feminine looks (in clothing, hairstyles, etc.)

* Colour: 4096 colours (12-bit 0x000 - 0xFFF)

* Base mass: 60 kg



##  (De)serialisation  ##

see SAVE_FORMAT.md


##  Actor as universal tool  ##

* Utility tiles that have states (e.g. noteblock) are implemented using Actor.


##  NPC Killing  ##

* AI:
    Attacked first: Warn player to not attack
    Attacked second: The NPC becomes hostile until player sheathe the weapon
    Attacked thrice: All the NPCs within the same faction become hostile until the player is away
    
    
## Ownership of lands ##

* Codex of Real Estate → assign owner to the tiles → copy the information to the NPC instance
 
 
## Health and magic point ##

* Works like Ampere in electronics.
    - Health: Regen per time
    - Magic: Out power per time
    - "I somewhat doubt this now..." --Torvald, 2017-03-12
    - "It won't work." --Torvald, 2017-04-22



## Civilisation ##

Based on _Millénaire_ Minecraft mod, they have limited resources, have ruler (one of those: town-ruler, country-ruler). Players can either help them or force their way into the head position, or even eliminating them.

Villages can be either independent or part of larger country (_Dwarf Fortress_)


## Level of technology ##

Anything goes as long as it does not exceed TL11 of GURPS. Although the universe has the existence of traditional sorcery, the laws of physics of the universe itself is same as we know it. Simply put: NO FUCKING PERPETUAL MOTION AND PERFECT PROPHECY

