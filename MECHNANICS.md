
##  Weapon tier  ##

    Natural / Common  Stone -> Copper -> Iron -> Silver      ->        Titanium
    Forging                                -------------->  Steel  --------^
    Exotic ('elven')                 Glass                     Aurichalcum
    Special (something 'adamant')                                              ??? (Use material spec of CNT, tensile strength 180 GPa)

* Metal graphics
    - Gold: Hue 43, low Saturation
    - Aurichalcum: Hue 43, mid-high Saturation
    - Copper: Hue 33,
    - Copper rust: Hue 160
    - Iron rust: Hue 21


##  Size variation  ##

Race base weapon/tool size <- 10 [kg]
Size tolerance <- (50% * str/1000), or say, 20%

If the size is bigger than tolerable, weapon speed severely slows down, tools become unusable
    if use time >* 0.75 second, the weapon/tool cannot be equipped.
Small weapons/tools gains no (dis)advantage

When drawing: scale by (craftedWeaponSize / baseWeaponSize)

Crafted tool/weapon size is dependent to the baseRaceMass.


##  Gemstone tier  ##

Topaz -> R·G·B -> Diamond·Amethyst

Gemstones go elemental?! (pretty generic but otherwise there are no motivations to willingly collect them...)

- Ruby: grants fire resist
- Sapphire: grants cold resist
- Amethyst or **Topaz**: grants shock resist
- *two left boy!*


##  Colouring  ##

Natural: Use 4096
Magical/Surreal: Use 24 Bits

* Colouring of potion
    - Randomised, roguelike fashion
    - Choose Col(R40, G40, B40) from set of finite cards:
        39, 39, 19, 19, 0, 0
    - MULTIPLY blend chosen colour with white texture


##  Roguelike identity  ##

* Randomised things
    - E.g. potion
        Lime-coloured potion
        First play: "Potion (???)"
        After drank: "Potion (Healing)" is revealed.

        Second (new) play: "Potion (???)"
        After drank: "Potion (Neurotoxin)" is revealed.


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


##  Food/Potion dose  ##

Scale ^ 3 ^ (3/4) == (ThisWgt / TargetWgt) ^ (3/4)


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


## Resource management ##

* Tools/Weapons have durability
    - And NOTHING IS INDESTRUCTIBLE (just cooldown time if they are "endgame" items, but not the weapon with highest damage -- see how Master Sword works in Zelda BotW)
        - Hookshots are the exception!
    - I was against it, but now I think it would make the game more playable --Torvald, 2017-03-12


## Civilisation ##

Based on _Millénaire_ Minecraft mod, they have limited resources, have ruler (one of those: town-ruler, country-ruler). Players can either help them or force their way into the head position, or even eliminating them.

Villages can be either independent or part of larger country (_Dwarf Fortress_)


## Level of technology ##

Anything goes as long as it does not exceed TL11 of GURPS. Although the universe has the existence of traditional sorcery, the laws of physics of the universe itself is same as we know it. Simply put: NO FUCKING PERPETUAL MOTION AND PERFECT PROPHECY

