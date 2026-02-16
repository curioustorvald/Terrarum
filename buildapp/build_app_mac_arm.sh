#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
SRCFILES="terrarummac_arm"
APPDIR="./TerrarumMac.arm.app"
DESTDIR="out/$APPDIR"
RUNTIME="runtime-osx-arm"
PLISTFILE="../out/build_autogen_macos_Info.plist"
JARNAME="TerrarumBuild.jar"

if [ ! -f "out/assets.tar.zst" ] || [ ! -f "out/assets.manifest" ]; then
    echo "'assets.tar.zst' or 'assets.manifest' not found in out/; run 'make assets' first." >&2
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
mv $DESTDIR/Contents/MacOS/out/$RUNTIME/bin/java $DESTDIR/Contents/MacOS/out/$RUNTIME/bin/java

# Copy over the asset archive, manifest, and jarfile
cp "out/assets.tar.zst" $DESTDIR/Contents/MacOS/
cp "out/assets.manifest" $DESTDIR/Contents/MacOS/
cp "../out/$JARNAME" $DESTDIR/Contents/MacOS/out/

# zip everything
cd "out"
rm $APPDIR.zip
7z a -tzip $APPDIR.zip $APPDIR

echo "Build successful: $DESTDIR"
