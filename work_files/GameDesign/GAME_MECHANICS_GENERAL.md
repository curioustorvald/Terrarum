
## Using items

## Roguelike identity

### Randomised things

#### Potion
    Lime-coloured potion
    First play: "Potion (???)"
    After drank: "Potion (Healing)" is revealed.

    Second (new) play: "Potion (???)"
    After drank: "Potion (Neurotoxin)" is revealed.


### size variation of tools/weapons/etc.

Race base weapon/tool size <- 10 [kg]
Size tolerance <- (50% * str/1000), or say, 20%

If the size is bigger than tolerable, weapon speed severely slows down, tools become unusable
    if use time >* 0.75 second, the weapon/tool cannot be equipped.
Small weapons/tools gains no (dis)advantage

When drawing: scale by (craftedWeaponSize / baseWeaponSize)

Crafted tool/weapon size is dependent to the baseRaceMass.