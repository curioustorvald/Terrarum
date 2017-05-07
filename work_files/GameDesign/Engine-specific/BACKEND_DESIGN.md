The extension of [```THE_ENGINE.md```](THE_ENGINE.md)

The game has elements that are:

- Actors
    - Controller
        - AI
        - JInput (Provided by LWJGL)
    - Factioning
    - Scheduler
    - Physics solver (simple, AABB-oriented)
    - _TODO: sprite assembler (character--avatar--maker)_
- Imagefont
    - Font drawer
        - Hangul character assembler (aka JOHAB)
- Sprite animator (really simple one)
- De/serialiser
- Concurrency helper (really simple one)
- Tiles
    - Tile property database
- Items
    - Item property database
        - _TODO: Material system_
- Map drawer
    - Map camera
    - Map drawer
    - Lightmap renderer
- Map generator
    - Utilises Joise Modular Noise Generator
    - Additional noise filters
- Real estate
    - The registry (a book that records the current owner of tiles on the map. Owner can be a single NPC or faction)
    - Utility
- Internationalisation
    - Language pack

The elements are connected like:

- Actors
    - Controller ← AI, Player Input
    - AI ← Factioning, Scheduler
    - class "Actor"
        - class "ActorWithBody"
            with properties like (AIControlled, Factionable, Luminous, Visible, etc.)
            - class "NPCIntelligentBase"
                - (NPCs that has AI and can be interacted)
            - (fixtures, player)
        - (any of the invisible actors)
        
        