The playlists (or albums) are stored under (userdata)/Custom/Music/(album name)

The name of the directory is used as the album title, and the lexicographic sorting of the files is used
as the playing order, so it is advised to name your files as 01.ogg, 02.ogg, 03.ogg, etc.

To actually give titles to the files, you must write `playlist.json` with following format:

```json
{
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


### Limitations

On certain filesystem and platform combination, you cannot use non-ASCII character on the album title
due to an incompatibility with the Java's File implementation. Song titles on `playlist.json` has no
such limitation.