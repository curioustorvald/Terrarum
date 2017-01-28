## Gadgets ##

### Looms for custom pattern ###

- Players can create their own décors (hang on wall), dresses.
- Two looms (16 palette mode, 64 palette mode)
- __IMPLEMENTED__ — see net.torvald.terrarum.gameactors.DecodeTapestry


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
            notes = [arr<int>, fixed size of 48],
            speed = 120
        }
        
        *int: (0-63) number of the note pitch that is struck. 32: Middle C (C3). 
        'A' just above of Middle C (A3) has base pitch of 440 Hz.
        *speed: in BPM
        
        
## Aimhack ##

- Include a valid way of obtaining Aimhack (possessed weapon shit?)
- Implement it on ```<item>.primaryUse(gc, delta)```
