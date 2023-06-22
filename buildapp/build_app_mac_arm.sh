#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
SRCFILES="terrarummac_arm"
DESTDIR="out/TerrarumMac.arm.app"
RUNTIME="runtime-osx-arm"

if [ ! -d "../assets_release" ]; then
    echo "'assets_release' does not exist; prepare the assets for the release and put them into the assets_release directory, exiting now." >&2
    exit 1
fi

# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR
mkdir $DESTDIR/Contents
mkdir $DESTDIR/Contents/MacOS
mkdir $DESTDIR/Contents/Resources

# Prepare an application
cp AppIcon.icns $DESTDIR/Contents/Resources/AppIcon.icns
cp $SRCFILES/Info.plist $DESTDIR/Contents/
cp $SRCFILES/Terrarum.sh $DESTDIR/Contents/MacOS/
chmod +x $DESTDIR/Contents/MacOS/Terrarum.sh

# Copy over a Java runtime
cp -r "../out/$RUNTIME" $DESTDIR/Contents/MacOS/

# Copy over all the assets and a jarfile
cp -r "../assets_release" $DESTDIR/Contents/MacOS/
mv $DESTDIR/Contents/MacOS/assets_release $DESTDIR/Contents/MacOS/assets
mkdir $DESTDIR/Contents/MacOS/out
cp "../out/TerrarumBuild.jar" $DESTDIR/Contents/MacOS/out/

echo "Build successful: $DESTDIR"
