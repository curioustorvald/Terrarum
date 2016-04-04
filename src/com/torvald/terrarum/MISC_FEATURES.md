## Gadgets ##

### Looms for custom pattern ###

- Players can create their own décors (hang on wall), dresses.
- Two looms (216 colour mode, 4096 colour mode)


### Music making ###

- Automated glockenspiel thingy
- Single tile can hold 48 notes, single track
- Work like Modtracker, incl. physical arrangements

        Arrangements in the map
        Time →→→→ 
        voice 1 → □□□□□□□□□□□□□□□□...
        voice 2 → □□□□□□□□□□□□□□□□...
                  ↑ played simultaneously along the X-axis
                  
- Each tracker head and body are connected by placing tracks adjacent to each other or connecting them with wire.
Connect two or more tracker head to play the array of trackers play simultaneously (multi-tracking)
                  
- Serialisation

        <actorid>.json
        {
            0 = [long],
            1 = [long],
            ...
            47 = [long],
            speed = 120
        }
        
        *long: array of bits that indicates the note is stricken (1) or not (0)
               0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000
               ↑G5     ↑C5            ↑C4            ↑C3            ↑C2            ↑C1     E0↑
               (Assuming C3 (32nd bit) as middle 'C')
               
        *speed: in BPM