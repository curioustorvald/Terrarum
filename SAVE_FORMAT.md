##  Format  ##

Contain everything on [TEVD](github.com/minjaesong/TerranVirtualDisk)

*  Save meta
    - binary
    - Filename : world (with no extension)

    |Type      |Mnemonic   |Description                  |
    |----------|-----------|-----------------------------|
    |Byte[4]   |TESV       |Magic                        |
    |Byte[n]   |name       |Savegame name, UTF-8         |
    |Byte      |NULL       |String terminator            |
    |Byte[8]   |terraseed  |Terrain seed                 |
    |Byte[8]   |rogueseed  |Randomiser seed              |
    |Byte[4]   |crc1       |CRC-32 of worldinfo1 entry   |
    |Byte[4]   |crc2       |CRC-32 of worldinfo2 entry   |
    |Byte[4]   |crc3       |CRC-32 of worldinfo3 entry   |
    |Byte[4]   |crc4       |CRC-32 of worldinfo4 entry   |
    |Byte[32]  |hash4321   |SHA-256 of crc4..crc3..crc2..crc1|
    |Int       |refid      |Reference ID of the player   |
    |Long      |time_t     |Current world's time_t       |
    |Byte[6]   |t_create   |Creation time of the savefile in time_t|
    |Byte[6]   |t_lastacc  |Last play time in time_t     |
    |Int       |t_wasted   |Total playtime in time_t     |
    
    Endianness: Big
    
    each entry on the disk contains CRC of its data, we can compare CRC saved in meta && CRC of entry header && CRC of actual content

*  Actor/Faction data
    - GSON
    - Filename : (refid) (with no extension)


*  Prop data
    - CSV
    - Filename : (with no extension)
    worldinfo2 -- tileprop
    worldinfo3 -- itemprop
    worldinfo4 -- materialprop
    worldinfo5 -- modules loadorder


*  Human-readable
    - Tiles_list.txt -- list of tiles in csv
    - Items_list.txt -- list of items in csv
    - Materials_list.txt -- list of materials in csv
    - load_order.txt -- module load order



##  How it works  ##
* If hash discrepancy has detected, (hash of csv in save dir != stored hash || hash of TEMD != stored hash), printout "Save file corrupted. Continue?" with prompt "Yes/No"

Directory:

    +--- <save1.tevd>
     --- 2a93bc5f (item ID) Actor/DynamicItem/Faction/etc. data (JSON)
     --- 423bdc83 (item ID) Actor/DynamicItem/Faction/etc. data (JSON)
     --- Items_list.txt     Human-readable
     --- Materials_list.txt Human-readable
     --- Tiles_list.txt     Human-readable
     --- world              save meta (binary)
     --- worldinfo1         TEMD (binary)
     --- worldinfo2         tileprop (CSV)
     --- worldinfo3         itemprop (CSV)
     --- worldinfo4         materialprop (CSV)
     +--- computers
      --- (UUID)            virtual disk
     +--- tapestries
      --- (random Int)      tapestry

Alongside with save1.tevd (extension should not exist in real game), keep save1.backup.tevd as a last-working save.