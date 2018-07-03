Creature raw documentation

## Physical properties ##

* 1 m = 24 px
* mult: Multiplier in pencentage. e.g. 100, 85, 125, ...

|name|unit|description|
|----|----|-----------|
|baseheight|pixels|Base height for hitbox. Also used for attack point bonus calculation|
|basemass|kg|Base mass for creature|
|accel|px per TARGET_FPS^2|Acceleration for walking|
|speed|px per TARGET_FPS|Walk speed|
|jumppower|neg. px per TARGET_FPS^2|Self-explanatory|
|scale|unit|Creature body scale. Mass/strength/accel/etc. will be changed accordingly, hence the prefix “base” for some raw tokens|
|dragcoeff|unit|Drag coefficient|
|speedmult, accelmult, jumppowermult|array of percentiles (Int)|Variability factor|
|scalemult|mult|Breadth variation for mobs|

## Creature properties ##

|name|unit|description|
|----|----|-----------|
|strength|unit|Strength value, no significant unit is given. The value for regular human is fixed to 1 000|
|encumbrance|kg or itemcount|Capacity of carrying|
|basedefence|unit|Base defence value of body. Sterner body composition (material) == higher value|
|armourdefence|unit|Current defence point of armour worn|
|armourdefencemult|mult|Bonus point for armour defence|
|toolsize|kg|Base tool size for the creature. See MECHANICS file for more information|

## Aesthetic properties ##

|name|unit|description|
|----|----|-----------|
|color|30-bit RGB (Int)|Self-glow. Set to 0 for not glowing|
|name|String|Given (perhaps customised) name|
|racename|STRING_ID|Racename token in language CSV|
|racenameplural|STRING_ID|Racename token in language CSV|

* Note: color uses customised 30-bit RGB. The format specifies ```1.0``` color of white (```#FFFFFF```) be ```0000_0011111111_0011111111_0011111111```, and can hold color range of 0.0-4.0

## Flags ##

|name|unit|description|
|----|----|-----------|
|intelligent|Boolean|Whether the creature can speak and talk.|
