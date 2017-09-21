The _Resetti_ will discourage players from exploiting the game reset.

For the sake of playability, anti-frustration features also must be in place.

## _Resetti_ Binary File

_Resetti_ will reside within the savegame with disguised filename, to prevent novice-level tampering.

### Binary Format

Total 6 bytes of data, in which:

- First 1 Byte: SAVE/LOAD marker. If sixth bit is zero (0bxx0x_xxxx), it's LOAD marker, otherwise it's SAVE. The rest of bits are totally random.
- 5 Payload Bytes: truncated UNIX timestamp in little-endian, unsigned. Will overflow on the Gregorian year 36812.
