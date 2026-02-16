#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
APPIMAGETOOL="appimagetool-x86_64.AppImage" # Note to self: run this on Asahi
SRCFILES="terrarumlinux_arm"
DESTDIR="TerrarumLinux.arm"
RUNTIME="runtime-linux-arm"
DESKTOPFILE="../out/build_autogen_linux.desktop"
JARNAME="TerrarumBuild.jar"

if [ ! -f "out/assets.tar.zst" ] || [ ! -f "out/assets.manifest" ]; then
    echo "'assets.tar.zst' or 'assets.manifest' not found in out/; run 'make assets' first." >&2
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
mv $DESTDIR/out/$RUNTIME/bin/java $DESTDIR/out/$RUNTIME/bin/java

# Copy over the asset archive, manifest, and jarfile
cp "out/assets.tar.zst" $DESTDIR/
cp "out/assets.manifest" $DESTDIR/
cp "../out/$JARNAME" $DESTDIR/out/

# Pack everything to AppImage
ARCH=aarch64 "./$APPIMAGETOOL" $DESTDIR "out/$DESTDIR.AppImage" || { echo 'Building AppImage failed' >&2; exit 1; }
chmod +x "out/$DESTDIR.AppImage"
rm -rf $DESTDIR || true
echo "Build successful: $DESTDIR"
