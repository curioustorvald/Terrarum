The playlists (or albums) are stored under (userdata)/Custom/Music/(album name)

The lexicographic sorting of the files is used as the playing order, so it is advised to name your
files as 01.ogg, 02.ogg, 03.ogg, etc.

To actually give titles to the files, you must write `playlist.json` with following format:

```json
{
    "albumName": "Totally Awesome Playlist 2024",
    "diskJockeyingMode": "continuous", /* "continuous" allows the Gapless Playback, "intermittent" will put random length of pause (in a range of 30 to 60 seconds) between tracks */
    "shuffled": false, /* self-explanatory, often used with "diskJockeyingMode": "intermittent" */
    "titles": {
        "01.ogg": "Lorem Ipsum",
        "02.ogg": "Dolor Sit Amet",
        "03.ogg": "Consectetur Adipiscing",
        "04.ogg": "Sed Do Tempor"
        /* these are the filename-to-song-title lookup table the music player actually looks for */
    }
}
```

- `albumName` may be omitted, in which the name of the directory will be substituted as the album title.