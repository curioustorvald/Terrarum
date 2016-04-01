##  Format  ##

*  Save meta
    - GZip'd binary (for more security)
    - Filename : world (with no extension)

    |Type        |Mnemonic    |Description                            |
    |------------|------------|---------------------------------------|
    |Byte[4]     |TESV        |Magic                                  |
    |Byte[n]     |name        |Savegame name, UTF-8                   |
    |Byte        |NULL        |String terminator                      |
    |Byte[8]     |terraseed   |Terrain seed                           |
    |Byte[8]     |rogueseed   |Randomiser seed                        |
    |Byte[32]    |hash1       |SHA-256 hash of worldinfo1 being stored (when not zipped)|
    |Byte[32]    |hash2       |SHA-256 hash of worldinfo2 being stored (when not zipped)|
    |Byte[32]    |hash3       |SHA-256 hash of worldinfo3 being stored (when not zipped)|
    |Byte[32]    |hash4       |SHA-256 hash of worldinfo4 being stored (when not zipped)|
    
    Endianness: Big

*  Actor/Faction data
    - GZip'd GSON
    - Filename : (refid) (with no extension)


*  Prop data
    - GZip'd CSV
    - Filename : (with no extension)
    worldinfo2 -- tileprop
    worldinfo3 -- itemprop
    worldinfo4 -- materialprop


*  Human-readable
    - Tiles_list.txt -- list of tiles in csv
    - Items_list.txt -- list of items in csv
    - Materials_list.txt -- list of materials in csv



##  How it works  ##
* If hash discrepancy has detected, (hash of csv in save dir != stored hash || hash of TEMD != stored hash), printout "Save file corrupted. Continue?" with prompt "Yes/No"

Directory:

    +--- <save1>
     --- 2a93bc5fd...f823   Actor/Faction/etc. data
     --- 423bdc838...93bd   Actor/Faction/etc. data
     --- Items_list.txt     Human-readable
     --- Materials_list.txt Human-readable
     --- Tiles_list.txt     Human-readable
     --- world              save meta (binary, GZip)
     --- worldinfo1         TEMD (binary, GZip)
     --- worldinfo2         tileprop (GZip)
     --- worldinfo3         itemprop (GZip)
     --- worldinfo4         materialprop (GZip)
