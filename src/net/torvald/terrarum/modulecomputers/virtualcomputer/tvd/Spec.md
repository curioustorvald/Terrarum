# Terran Virtual Disk Image Format Specification

current specversion number: 0x03

## Changes

### 0x03
- Option to compress file entry

### 0x02
- 48-Bit filesize and timestamp (Max 256 TiB / 8.9 million years)
- 8 Reserved footer

### 0x01
**Note: this version were never released in public**
- Doubly Linked List instead of Singly


## Specs

* File structure


    Header
    
    IndexNumber
    <entry>
    
    IndexNumber
    <entry>
    
    IndexNumber
    <entry>
    
    ...
    
    Footer


* Order of the indices does not matter. Actual sorting is a job of the application.
* Endianness: Big


##  Header
    Uint8[4]    Magic: TEVd
    Int48       Disk size in bytes (max 256 TiB)
    Uint8[32]   Disk name
    Int32       CRC-32
                1. create list of arrays that contains CRC
                2. put all the CRCs of entries
                3. sort the list (here's the catch -- you will treat CRCs as SIGNED integer)
                4. for elems on list: update crc with the elem (crc = calculateCRC(crc, elem))
    Int8        Version
    
    (Header size: 47 bytes)



##  IndexNumber and Contents
    <Entry Header>
    <Actual Entry>

###  Entry Header
    Int32       EntryID (random Integer). This act as "jump" position for directory listing.
                NOTE: Index 0 must be a root "Directory"; 0xFEFEFEFE is invalid (used as footer marker)
    Int32       EntryID of parent directory
    Int8        Flag for file or directory or symlink (cannot be negative)
                0x01: Normal file, 0x02: Directory list, 0x03: Symlink
                0x11: Compressed normal file
    Uint8[256]  File name in UTF-8
    Int48       Creation date in real-life UNIX timestamp
    Int48       Last modification date in real-life UNIX timestamp
    Int32       CRC-32 of Actual Entry

    (Header size: 281 bytes)

###  Entry of File (Uncompressed)
    Int48       File size in bytes (max 256 TiB)
    <Bytes>     Actual Contents
    
    (Header size: 6 bytes)

###  Entry of File (Compressed)
    Int48       Size of compressed payload (max 256 TiB)
    Int48       Size of uncompressed file (max 256 TiB)
    <Bytes>     Actual Contents, DEFLATEd payload
    
    (Header size: 12 bytes)

###  Entry of Directory
    Uint16      Number of entries (normal files, other directories, symlinks)
    <Int32s>    Entry listing, contains IndexNumber
    
    (Header size: 2 bytes)

###  Entry of Symlink
    Int32       Target IndexNumber
    
    (Content size: 4 bytes)




## Footer
    Uint8[4]    0xFE 0xFE 0xFE 0xFE (footer marker)
    Int8        Disk properties flag 1
                0b 7 6 5 4 3 2 1 0
                
                0th bit: Readonly
                
    Int8[7]     Reserved, should be filled with zero
    <optional footer if present>
    Uint8[2]    0xFF 0x19 (EOF mark)
