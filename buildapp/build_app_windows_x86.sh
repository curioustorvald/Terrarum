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
if ! command -v x86_64-w64-mingw32-gcc  &> /dev/null
then
    echo 'Mingw32 not found; please install mingw64-cross-gcc (or similar) to your system' >&2; exit 1;
fi

x86_64-w64-mingw32-gcc -Os -s -o $DESTDIR/Terrarum.exe $SRCFILES/Terrarum.c || { echo 'Building EXE failed' >&2; exit 1; }
# TODO add icon to the exe

# Copy over a Java runtime
mkdir $DESTDIR/out
cp -r "../out/$RUNTIME" $DESTDIR/out/

# Copy over all the assets and a jarfile
cp -r "../assets_release" $DESTDIR/
mv $DESTDIR/assets_release $DESTDIR/assets
cp  "../out/TerrarumBuild.jar" $DESTDIR/out/

# Temporary solution: zip everything
zip -r -9 -l "out/TerrarumWindows.x86.zip" $DESTDIR
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"
