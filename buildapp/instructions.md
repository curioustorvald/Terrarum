### Preparation

Download and unzip the JDKs to ~/Documents/openjdk/* for the appropriate operating systems first! JDKs can be downloaded on https://jdk.java.net/archive/.

The filenames must be:

| Target OS/Arch      | filename           |
|---------------------|--------------------|
| Linux AMD64         | jdk-17.0.1-x86     |
| Linux Aarch64       | jdk-17.0.1-arm     |
| Windows AMD64       | jdk-17.0.1-windows |
| macOS Apple Silicon | jdk-17.0.1.jdk-arm |
| macOS Intel         | jdk-17.0.1.jdk-x86 |

Then, on the terminal, run following commands:

```
jlink --module-path ~/Documents/openjdk/jdk-17.0.1-x86/jmods:mods  --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-linux-x86 --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1-arm/jmods:mods  --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-linux-arm --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1-windows/jmods:mods  --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-windows-x86 --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1.jdk-arm/Contents/Home/jmods:mods  --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-osx-arm --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1.jdk-x86/Contents/Home/jmods:mods  --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-osx-x86 --no-header-files --no-man-pages --strip-debug --compress=2
```

(note: matching the building machine with the target OS is highly recommended -- Use Linux for building linux x86/arm; Mac for building macOS x86/arm; Windows for building Windows Java Runtime)

This process assumes that the game does NOT use the Java 9+ modules and every single required libraries are fat-jar'd (their contents extracted right into the Jar)

### Packaging

Create an output directory if there is none (project root/buildapp/out)

Before running the packaging script make sure:

1. The required runtime must exist on `(project root)/out/runtime-<linux|osx|windows>-<arm|x86>` directory
2. The build scripts are on a subdirectory of the project directory

To build, **cd into the "(project root)/buildapp/", then execute the appropriate script**.

The packaged application can be found on `(project root)/buildapp/out/`

#### OSX .app Packaging

```
Terrarum.*.app
+.icns          /* 512x512 PNG */
+Contents 
`Info.plist
 +MacOS
  `start_game_mac_*.sh    * permission: +x */
```

`assets/TerrarumBuild.jar` is the artifact built using the TerrarumBuild.

`start_game_*` files are on the root directory of the project; use them to build executable apps.

Hide the `.jar` within the subdirectory; users will think this file is the main executable and will try to execute it using whatever JVM they may (or may not) have.

#### Some Notes

- Windows EXE creation is not there yet. Maybe use one of [these?](https://sourceforge.net/projects/batch-compiler/)

### Notes to Terrarum Programmers

By self-containing everything in one file, it is not possible to modify the base game easily. Modloading scheme must be extended to load from mutable directory such as `%APPDATA%/Terrarum/mods`.
