#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
APPIMAGETOOL="appimagetool-aarch64.AppImage" # Note to self: run this on Asahi
SRCFILES="terrarumlinux_arm"
DESTDIR="TerrarumLinux.arm"
RUNTIME="runtime-linux-arm"
DESKTOPFILE="../out/build_autogen_linux.desktop"
JARNAME="TerrarumBuild.jar"

if [ ! -d "../assets_release" ]; then
    echo "'assets_release' does not exist; prepare the assets for the release and put them into the assets_release directory, exiting now." >&2
    exit 1
fi

# Cleanup
rm -rf $DESTDIR || true
mkdir $DESTDIR

# Prepare an application
cp icns.png $DESTDIR/icns.png
cp $DESKTOPFILE $DESTDIR/
cp $SRCFILES/AppRun $DESTDIR/AppRun
chmod +x $DESTDIR/AppRun

# Copy over a Java runtime
mkdir $DESTDIR/out
cp -r "../out/$RUNTIME" $DESTDIR/out/
mv $DESTDIR/out/$RUNTIME/bin/java $DESTDIR/out/$RUNTIME/bin/Terrarum

# Copy over all the assets and a jarfile
cp -r "../assets_release" $DESTDIR/
mv $DESTDIR/assets_release $DESTDIR/assets
cp "../out/$JARNAME" $DESTDIR/out/

# Pack everything to AppImage
"./$APPIMAGETOOL" $DESTDIR "out/$DESTDIR.AppImage" || { echo 'Building AppImage failed' >&2; exit 1; }
chmod +x "out/$DESTDIR.AppImage"
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"
