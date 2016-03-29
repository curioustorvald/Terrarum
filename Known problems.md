# Unresolved #

### Character ###
  
* Arm behind the body seems unnatural


### Phys ###

* Actor stick to wall and not get off
* Actor with mass <2 behaves erratically


# Resolved #

* Cannot fit into single-tile width pit
  Cause: Player/NPC looks slim enough to fit, but don't fit in the game
  Solution: Draw them wider, allow them to fit into the pit (Phys resolver will glitch?)
  __Solved__ — Player/NPC hitbox now have a width of 15 pixels.
