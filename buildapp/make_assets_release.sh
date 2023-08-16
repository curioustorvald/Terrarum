#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
DESTDIR="../assets_release"

rm -r $DESTDIR
cp -r "../assets" $DESTDIR

rm $DESTDIR/loopey.wav
rm $DESTDIR/ktGrepExample.kts
rm $DESTDIR/batchtest.txt
rm $DESTDIR/test_texture.tga
rm $DESTDIR/worldbacktest.tga
rm -r $DESTDIR/books
rm $DESTDIR/clut/skybox.tga
rm $DESTDIR/graphics/*.bat
rm $DESTDIR/keylayout/*.not_ime
rm $DESTDIR/mods/basegame/blocks/*.gz
rm $DESTDIR/mods/basegame/blocks/*.txt
rm -r $DESTDIR/mods/basegame/sounds
rm -r $DESTDIR/mods/dwarventech



