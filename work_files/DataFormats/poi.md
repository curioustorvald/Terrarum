
POI (Point of Interest) is a placement of blocks that can be used by the world generator.

POIs are serialised as following:

```json
{
  "genver": 1234567890, /* game version in Int64 */
  "id": "test_struct_with_enumerable_variations",
  "wlen": 8, /* word length of the tile number. Can be 8 or 16 */
  "lut": {"0": "basegame:0", "1": "basegame:48"},
  "w": 7, "h": 4,
  "layers": [ /* order matters! */
    {"name": "base", "dat": [
      "...layer_0.gz.b85", /* each byte matches what's on the LUT. Endianness: little */
      "...layer_1.gz.b85"  /* byte of 0 is NOT guaranteed to be an air tile, nor the byte of -1 is NOT guaranteed to be a null tile */
    ]},
    {"name": "varianceA", "dat": [
      "...layer_0.gz.b85",
      "...layer_1.gz.b85"
    ]},
    {"name": "varianceB", "dat": [
      "...layer_0.gz.b85",
      "...layer_1.gz.b85"
    ]},
    {"name": "varianceC", "dat": [
      "...layer_0.gz.b85",
      "...layer_1.gz.b85"
    ]},
    {"name": "overlay", "dat": [
      "...layer_0.gz.b85",
      "...layer_1.gz.b85"
    ]}
  ]
}
```