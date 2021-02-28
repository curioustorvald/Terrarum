Following code is an example savegame JSON files.

#### savegame.json
```
{
    savename: "Test World 1",
    genver: 4, /* generator version in integer */
    terrseed: "84088805e145b555",
    randseed: "19b25856e1c150ca834cffc8b59b23ad",
    weatseed: "e5e72beb4e3c6926d3dc9e3e2ef7833b",
    playerid: 9545698,
    creation_t: <creation time in real-world unix time>,
    lastplay_t: <creation time in real-world unix time>,
    creation_t: <creation time in real-world unix time>,
    thumb: <Ascii85-encoded gzipped thumbnail image in TGA>,
    
    blocks: <BlockCodex serialised>,
    items: <ItemCodex serialised>,
    itemd: <DynamicItems serialised>,
    wires: <WireCodex serialised>,
    fluids: <FluidCodex serialised>,
    materials: <MaterialCodex serialised>,
    loadorder: <LoadOrder serialised>,
    worlds: [1,2,6,7]
}
```

#### world1.json

File is named as `"world"+world_index+".json"`

```
{
    worldname: "New World",
    comp: <0 for uncompressed, 1 for GZip, 2 for LZMA>,
    width: 8192,
    height: 2048,
    spawnx: 4096,
    spawny: 248,
    genver: 4, /* generator version in integer */
    time_t: <in-game TIME_T of this world>,
    terr: {
        s: 33554432,
        h: "a441b15fe9a3cf56661190a0b93b9dec7d04127288cc87250967cf3b52894d11",
        b: <Ascii85-encoded gzipped terrain layerdata>
    },
    wall: {
        s: 33554432,
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped wall layerdata>
    },
    tdmg: {
        s: 8795,
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped terrain damage in JSON>
    },
    wdmg: {
        s: 2,
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped wall damage in JSON>
    },
    flui: {
        s: 15734
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped fluids in JSON>
    },
    wire: {
        s: 2,
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped wiring nodes in JSON>
    },
    tmap: {
        s: 4316,
        h: <SHA-256 hash of 'b'>,
        b: <Ascii85-encoded gzipped tilenumber-to-tilename map in JSON>
    }
}
```

#### actors.json

```
{
    <actor id>: { actor serialised in JSON },
    ...
}
```