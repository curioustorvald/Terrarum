### Preparation

Download and unzip the JDK for the appropriate operation systems first! JDKs can be downloaded on https://jdk.java.net/archive/.

Then, on the terminal, run following commands:

jlink --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-linux-x86 --no-header-files --no-man-pages --strip-debug --compress=2

jlink --module-path ~/Documents/openjdk/jdk-17.0.1-arm/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-linux-arm --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1-windows/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-windows-x86 --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1.jdk-arm/Contents/Home/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-osx-arm --no-header-files --no-man-pages --strip-debug --compress=2
jlink --module-path ~/Documents/openjdk/jdk-17.0.1.jdk-x86/Contents/Home/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output ~/Documents/Terrarum/out/runtime-osx-x86 --no-header-files --no-man-pages --strip-debug --compress=2

This process assumes that the game does NOT use the Java 9+ modules and every single required libraries are fat-jar'd (their contents extracted right into the Jar)

### Packaging

Create an output directory; its contents shall be:

```
+assets
`(regular "assets")
`TerrarumBuild.jar
Terrarum.arm.app      /* uses start_game_mac_aarch64.sh and contains runtime-osx-arm */
Terrarum.x86.app      /* uses start_game_mac_x86_64.sh and contains runtime-osx-x86 */
Terrarum.arm.AppImage /* uses start_game_linux_aarch64.sh and contains runtime-linux-arm */
Terrarum.x86.AppImage /* uses start_game_linux_x86_64.sh and contains runtime-linux-x86 */
Terrarum.exe          /* uses start_game_windows.bat and contains runtime-windows-x86 */
```

#### Packaging Script

Before running the packaging script make sure:

1. All five runtimes must exist on `<project dir>/out/runtime-<linux|osx|windows>-<arm|x86>` directory
2. The build scripts are on a subdirectory of the project directory

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

### Notes to Terrarum Programmers

By self-containing everything in one file, it is not possible to modify the base game easily. Modloading scheme must be extended to load from mutable directory such as `%APPDATA%/Terrarum/mods`.