# Unresolved #

### Character ###
  
* Arm behind the body seems unnatural


### Physics ###



# Resolved #

* Cannot fit into single-tile width pit
  Cause: Player/NPC looks slim enough to fit, but don't fit in the game
  Solution: Player/NPC hitbox now have a width of 15 pixels.

* Actor stick to wall and not get off
  Cause: Unknown
  Solution: Changed collision detection method to CCD (continuous collision detection)

* Actor with mass <2 behaves erratically
  Details: Velocity becomes NaN when jumped
  Cause: Floating-point precision error?
  Solution: Changed every physics variable that uses Float to Double