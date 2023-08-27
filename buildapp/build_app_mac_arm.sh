#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
SRCFILES="terrarummac_arm"
APPDIR="./TerrarumMac.arm.app"
DESTDIR="out/$APPDIR"
RUNTIME="runtime-osx-arm"
PLISTFILE="../out/build_autogen_macos_Info.plist"
JARNAME="TerrarumBuild.jar"

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
cp $SRCFILES/Terrarum.sh $DESTDIR/Contents/MacOS/
chmod +x $DESTDIR/Contents/MacOS/Terrarum.sh
cp $PLISTFILE $DESTDIR/Contents/Info.plist

# Copy over a Java runtime
mkdir $DESTDIR/Contents/MacOS/out
cp -r "../out/$RUNTIME" $DESTDIR/Contents/MacOS/out/
mv $DESTDIR/Contents/MacOS/out/$RUNTIME/bin/java $DESTDIR/Contents/MacOS/out/$RUNTIME/bin/Terrarum

# Copy over all the assets and a jarfile
cp -r "../assets_release" $DESTDIR/Contents/MacOS/
mv $DESTDIR/Contents/MacOS/assets_release $DESTDIR/Contents/MacOS/assets
cp "../out/$JARNAME" $DESTDIR/Contents/MacOS/out/

# zip everything
cd "out"
rm $APPDIR.zip
7z a -tzip $APPDIR.zip $APPDIR

echo "Build successful: $DESTDIR"
