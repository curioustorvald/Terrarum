#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
SRCFILES="terrarumwindows_x86"
DESTDIR="TerrarumWindows.x86"
RUNTIME="runtime-windows-x86"

if [ ! -d "../assets_release" ]; then
    echo "'assets_release' does not exist; prepare the assets for the release and put them into the assets_release directory, exiting now." >&2
    exit 1
fi

# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR

# Prepare an application
cp $SRCFILES/Terrarum.bat $DESTDIR/

# Copy over a Java runtime
cp -r "../out/$RUNTIME" $DESTDIR/

# Copy over all the assets and a jarfile
cp -r "../assets_release" $DESTDIR/
mv $DESTDIR/assets_release $DESTDIR/assets
cp -r "../out/TerrarumBuild.jar" $DESTDIR/assets/

# Temporary solution: zip everything
zip -r -9 -l "out/TerrarumWindows.x86.zip" $DESTDIR
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"
