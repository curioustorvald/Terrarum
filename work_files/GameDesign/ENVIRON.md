## Atmosphere ##

* Serene (not all serene of course)
* There's living village, abandoned/crumbling settlement and lone shacks on the mountainside
* History, much like one in the _Dwarf Fortress_, decaying of the settlement is simulated by the very method, but 1-2 will be forced. The amount is dependent on the size of the map
* Reference: _Legend of Zelda: Breath of the Wild Trailer_ (the trailer came much after this game but the video will help you to get a grip)

## Colour overlay ##

Colour overlay is set to 6 500 K as untouched. The value must be

* Increased when:
    - Overcast (~ 7 000 K)
    - Freezing area (~ 7 500 K)
    
* Decreased when:
    - Tropical area (~ 5 500 K)
    
    
## Sunlight ##

Sunlight of the midday must have colour temperature of 5 700 K.

When the atmos is dusty, "colours" of sunrise/sunsets are more apparent: when in sunset, global light and the fog-of-war is more orange/red (atmos gets more colour from the sunlight and less from the sky-blue due to the mie scattering)

Colour of the mie scattering DOES follow the colourimeter observation (orange -> red -> wine bordeaux -> (very short term of) grey -> blueish grey)

Overcast/Rainy sky has following colour temperature (measured three times):

```
 Result is XYZ: 809.379324 850.192752 1033.330908, D50 Lab: 220.753889 -4.325124 -56.292269
                           CCT = 7388K (Delta E 3.094551)
 Closest Planckian temperature = 7185K (Delta E 2.752967)
 Closest Daylight temperature  = 7428K (Delta E 0.462706)
 Color Rendering Index (Ra) = 99.1 [ R9 = 92.0 ]
  R1  = 99.3  R2  = 99.9  R3  = 99.4  R4  = 99.2  R5  = 99.4  R6  = 99.9  R7  = 98.8
  R8  = 97.2  R9  = 92.0  R10 = 99.7  R11 = 99.4  R12 = 99.1  R13 = 99.7  R14 = 99.6
 Television Lighting Consistency Index 2012 (Qa) = 99.9

 Result is XYZ: 799.049876 839.524181 1015.426500, D50 Lab: 219.759423 -4.379982 -55.308508
                           CCT = 7344K (Delta E 3.137397)
 Closest Planckian temperature = 7142K (Delta E 2.790319)
 Closest Daylight temperature  = 7381K (Delta E 0.430938)
 Color Rendering Index (Ra) = 99.2 [ R9 = 92.1 ]
  R1  = 99.4  R2  = 99.9  R3  = 99.4  R4  = 99.2  R5  = 99.4  R6  = 99.9  R7  = 98.8
  R8  = 97.3  R9  = 92.1  R10 = 99.7  R11 = 99.4  R12 = 99.2  R13 = 99.7  R14 = 99.6
 Television Lighting Consistency Index 2012 (Qa) = 99.9

 Result is XYZ: 787.300466 827.265267 1011.192183, D50 Lab: 218.606255 -4.393304 -56.653947
                           CCT = 7448K (Delta E 3.152185)
 Closest Planckian temperature = 7236K (Delta E 2.805598)
 Closest Daylight temperature  = 7483K (Delta E 0.402978)
 Color Rendering Index (Ra) = 99.2 [ R9 = 92.5 ]
  R1  = 99.4  R2  = 99.9  R3  = 99.3  R4  = 99.2  R5  = 99.5  R6  = 99.8  R7  = 98.8
  R8  = 97.4  R9  = 92.5  R10 = 99.7  R11 = 99.5  R12 = 99.3  R13 = 99.8  R14 = 99.5
 Television Lighting Consistency Index 2012 (Qa) = 99.9
```

## Weather effects ##

* Wind
    - Blows away sands/snows
    - Tilts raindrops/snowfall
    
* Precipitation
    - Rain, snow