#!/bin/bash
if (( $EUID == 0 )); then echo "The build process is not meant to be run with root privilege, exiting now." >&2; exit 1; fi

cd "${0%/*}"
SRCDIR="../assets_release"
OUTDIR="out"
JARNAME="TerrarumBuild.jar"
TVDJAR="../lib/TerranVirtualDisk.jar"

if [ ! -d "$SRCDIR" ]; then
    echo "Error: $SRCDIR does not exist. Run make_assets_release.sh first." >&2
    exit 1
fi

if [ ! -f "../out/$JARNAME" ]; then
    echo "Error: ../out/$JARNAME not found. Build the project first." >&2
    exit 1
fi

if [ ! -f "$TVDJAR" ]; then
    echo "Error: $TVDJAR not found." >&2
    exit 1
fi

mkdir -p "$OUTDIR"

echo "Creating assets.tevd from $SRCDIR..."

# Build classpath from project JAR and all library JARs
CP="../out/$JARNAME"
for jar in ../lib/*.jar; do
    CP="$CP:$jar"
done

java -cp "$CP" net.torvald.terrarum.AssetArchiveBuilderKt "$SRCDIR" "$OUTDIR/assets.tevd"

if [ $? -ne 0 ]; then
    echo "Error: Failed to create assets.tevd" >&2
    exit 1
fi

echo "Done. Output:"
echo "  $OUTDIR/assets.tevd ($(du -h "$OUTDIR/assets.tevd" | cut -f1))"
