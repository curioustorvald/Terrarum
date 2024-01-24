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
rm $DESTDIR/clut/skybox*.tga
rm $DESTDIR/graphics/*.bat
rm $DESTDIR/keylayout/*.not_ime
rm $DESTDIR/mods/basegame/blocks/*.gz
rm $DESTDIR/mods/basegame/blocks/*.txt
rm $DESTDIR/mods/basegame/weathers/*.txt
rm $DESTDIR/mods/basegame/weathers/*.md
rm $DESTDIR/mods/basegame/weathers/*.kra
rm -r $DESTDIR/mods/basegame/audio/music/
rm -r $DESTDIR/mods/dwarventech
rm -r $DESTDIR/mods/myawesomemod
rm -r $DESTDIR/mods/musicplayer

for s in .directory .DS_Store Thumbs.db thumbs.db; do
  rm $DESTDIR/$s
  rm $DESTDIR/*/$s
  rm $DESTDIR/*/*/$s
  rm $DESTDIR/*/*/*/$s
  rm $DESTDIR/*/*/*/*/$s
  rm $DESTDIR/*/*/*/*/*/$s
  rm $DESTDIR/*/*/*/*/*/*/$s
  rm $DESTDIR/*/*/*/*/*/*/*/$s
done
