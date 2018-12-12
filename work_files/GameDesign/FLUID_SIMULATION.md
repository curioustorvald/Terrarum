#### Before we begin, please read following articles:

- Celluar Automata https://w-shadow.com/blog/2009/09/01/simple-fluid-simulation/
- Pressure sim in DF http://www.gamasutra.com/view/feature/131954/interview_the_making_of_dwarf_.php

### Serialisation
- FluidType: Hashed list of BlockAddress, Int
- FluidFill: Hashed list of BlockAddress, Float32

### Notes
- By doing this, you don’t need 16 blocks to represent a water; you only need 1 block for all kinds of fluids
- Storing the overfill (treat water as compressible) is important, will work as a temporary storage until the overfill is resolved, and rim of the update area will always be overfilled before they get updated
