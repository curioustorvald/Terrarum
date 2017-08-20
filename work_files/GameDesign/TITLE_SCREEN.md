# Main

## Scenery

Simulation of the world, day and night fast, camera follows landscape

## RemoCon
- Singleplayer
- Options
- Modules
- Language
- Credits
- Quit


# Singleplayer

## RemoCon
- New Character     (CONTEXT_CHARACTER_NEW)
- Delete Character  (CONTEXT_CHARACTER_DELETE)
- Rename            (MENU_LABEL_RENAME -- check Polyglot project's Core Editing)
- Return            (MENU_LABEL_RETURN)

## Panel
- Character selection screen, limited to 8 entries
- 8 Cells made of semitransparent dark rects

### Persona Cell
- Icon on the left
- Two rows:
    + Name --------- | Creation date | Last play date | Total play time
    + Name of the current universe | Number of multiverses (if >= 2)

### New Character
Step 1:
<chargen UI TBA>

Step 2:
Select world or create new world
If personaCount > 1, new player can join the existing world or universe (if the universe contains multiple world, one can choose one of the world within the universe)

### New World
Options:
- Size  (MENU_OPTIONS_SIZE)
    + Small (CONTEXT_DESCRIPTION_SMALL)
    + Big   (CONTEXT_DESCRIPTION_BIG)
    + Huge  (CONTEXT_DESCRIPTION_HUGE)
- More  (MENU_LABEL_MORE)
    + SEED (* Do-not-translate)



# Options

## RemoCon
- Controls          (MENU_OPTIONS_CONTROLS)
- Graphics          (MENU_LABEL_GRAPHICS)
- Gameplay Options  (MENU_OPTIONS_GAMEPLAY)
- MIDI (* Do-not-translate)
- Return            (MENU_LABEL_RETURN)

Save configuration to disk upon Return



# Modules

## RemoCon
- Return

## Panel
- List of module info cell

### Module Info Cell
- Loadorder number on the left
- Two rows:
    + Name (propername) ------- | version | Author
    + Description ------------- | releasedate in YYYY-MM-DD


# Languages

## RemoCon
- Return

Save configuration to disk upon Return

## Panel
- List of languages available, click to change immediately


# Credits

## RemoCon
- Credits
- GPL
- Back

## Panel
- Display selected menu, just a wall of text


# Quit

Execute command ```System.exit(0)``` (terminate application immediately)