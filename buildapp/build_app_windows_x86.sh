#!/bin/bash
cd "${0%/*}"
SRCFILES="terrarumwindows_x86"
DESTDIR="TerrarumWindows.x86"
RUNTIME="runtime-windows-x86"

# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR

# Prepare an application
cp $SRCFILES/Terrarum.bat $DESTDIR/

# Copy over a Java runtime
cp -r "../out/$RUNTIME" $DESTDIR/

# Copy over all the assets and a jarfile
cp -r "../assets" $DESTDIR/
cp -r "../out/TerrarumBuild.jar" $DESTDIR/assets/

# Temporary solution: zip everything
zip -r -9 -l "out/TerrarumWindows.x86.zip" $DESTDIR
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"