## Animation Description Language

This is a text version of my drawing of same name. 2018-01-04 CuriousTorvald

Author's node: yet another non-JSON domain-specific language because why not?

## Objective

* Java .properties-compatible
* Case insensitive

## Example code

```
SPRITESHEET=sprites/test
EXTENSION=.tga.gz

ANIM_RUN=DELAY 0.15;ROW 2
ANIM_RUN_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_FWD_LEFT;ARM_FWD_RIGHT;LEG_LEFT;LEG_RIGHT
ANIM_RUN_1=LEG_RIGHT 1,-1;LEG_LEFT -1,0
ANIM_RUN_2=ALL 0,-1;LEG_RIGHT 0,1;LEG_LEFT 0,-1
ANIM_RUN_3=LEG_RIGHT -1,0;LEG_LEFT 1,-1
ANIM_RUN_4=ALL 0,-1;LEG_RIGHT 0,-1;LEG_LEFT 0,1

ANIM_IDLE=DELAY 2;ROW 1
ANIM_IDLE_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_REST_LEFT;ARM_REST_RIGHT;LEG_LEFT;LEG_RIGHT
ANIM_IDLE_1=
! ANIM_IDLE_1 will not make any transformation
ANIM_IDLE_2=UPPER_TORSO 0,-1

ANIM_CROUCH=DELAY 1;ROW 3
ANIM_CROUCH_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_FWD_LEFT;ARM_FWD_RIGHT;LEG_CROUCH_LEFT;LEG_CROUCH_RIGHT
ANIM_CROUCH_1=
...
```

### In-detail

```
ANIM_RUN=DELAY 0.15;ROW 2
```

Each line defines one property. A property is a field-value pair. In this code, field is ```ANIM_RUN```, and the value is ```DELAY 0.15;ROW 2```

The values are further parsed using ```;``` (semicolon with NO spaces attached) as a separator.

```
In this example, ANIM_RUN contains two variables:

DELAY = 0.15
ROW = 2
```

A value of the field is consisted of zero or more variable-input pairs. Variable and the input are separated with one or more connected spaces.

#### Variables

Variables can have only one of the two types: ```float``` and ```ivec2```. Single integer value ('2' in the ROW) are regarded as a float.

Float and Ivec2 are determined by:

* Ivec2: inputs that are matched by the regex ```-?[0-9]+,-?[0-9]+``` (we call this "ivec2 regex")
* Float: inputs that are matched by the regex ```-?[0-9]+(\.[0-9]*)?```, but not even partially matched by the ivec2 regex.

Any argument to the body parts takes ivec2, to move the parts accordingly.

#### Just one exception: SPRITESHEET and EXTENSION

SPRITESHEET and EXTENSION property is not parsed as a property, it's just a single string like the original .properties

### Naming convention of properties

If a field is recognised as an animation (in this case ANIM_RUN), the assembler will look for the fields named like ANIM_RUN_1, ANIM_RUN_2, ... , ANIM_RUN_9, ANIM_RUN_10 and beyond.

### Naming convention of files

If the animation specifies a "body part" (in this example LEG_LEFT and LEG_RIGHT), the assembler will look for a file ```sprites/test_leg_left.tga.gz``` and ```sprites/test_leg_right.tga.gz``` respectively. Filenames are advised to be kept all lowercase.

#### Reserved keywords

|Name|Type|Meaning|
|---|---|---|
|SPRITESHEET|property/string|base file name of the images|
|EXTENSION|property/string|extension of the base file|
|DELAY|variable: float|delay between frames, in seconds|
|ROW|vareable: float|which row the animation goes in the spritesheet|

### Notes

* All indices are one-based

## Operation

* Each line describes transformation
* Transformation are applied sequentially from left to right. In other words, their order matters. Be wary of the clipping that may occur!
* The Field is an identifier the game code -- sprite assembler -- recognises.
* The Field of animation's name is the name the game code looks for. Example: ```this.setAnim("ANIM_RUN")```
