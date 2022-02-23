#!/bin/bash
cd "${0%/*}"
APPIMAGETOOL="appimagetool-x86_64.AppImage"
SRCFILES="terrarumlinux_x86"
DESTDIR="TerrarumLinux.x86"
RUNTIME="runtime-linux-x86"

# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR

# Prepare an application
cp icns.png $DESTDIR/icns.png
cp $SRCFILES/Terrarum.desktop $DESTDIR/
cp $SRCFILES/AppRun $DESTDIR/AppRun
chmod +x $DESTDIR/AppRun

# Copy over a Java runtime
cp -r "../out/$RUNTIME" $DESTDIR/

# Copy over all the assets and a jarfile
cp -r "../assets" $DESTDIR/
cp -r "../out/TerrarumBuild.jar" $DESTDIR/assets/

# Pack everything to AppImage
"./$APPIMAGETOOL" $DESTDIR "out/$DESTDIR.AppImage" || { echo 'Building AppImage failed' >&2; exit 1; }
chmod +x "out/$DESTDIR.AppImage"
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"