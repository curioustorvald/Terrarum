#!/bin/bash
cd "${0%/*}"
SRCFILES="terrarummac_x86"
DESTDIR="out/TerrarumMac.x86.app"
RUNTIME="runtime-osx-x86"
# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR
mkdir $DESTDIR/Contents
mkdir $DESTDIR/Contents/MacOS

# Prepare an application
cp icns.png $DESTDIR/.icns
cp $SRCFILES/Info.plist $DESTDIR/Contents/
cp $SRCFILES/Terrarum.sh $DESTDIR/Contents/MacOS/
chmod +x $DESTDIR/Contents/MacOS/Terrarum.sh

# Copy over a Java runtime
cp -r "../out/$RUNTIME" $DESTDIR/Contents/MacOS/

# Copy over all the assets and a jarfile
cp -r "../assets" $DESTDIR/Contents/MacOS/
cp -r "../out/TerrarumBuild.jar" $DESTDIR/Contents/MacOS/assets/

echo "Build successful: $DESTDIR"