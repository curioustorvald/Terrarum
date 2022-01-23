### Preparation

Download and unzip the JDK for the appropriate operation systems first! JDKs can be downloaded on https://jdk.java.net/archive/.

Then, on the terminal, run following commands:

jlink --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output /home/installed/Documents/Terrarum/out/runtime-linux --no-header-files --no-man-pages --strip-debug --compress=2

jlink --module-path /home/installed/Documents/openjdk/jdk-11.0.2-windows/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output /home/installed/Documents/Terrarum/out/runtime-windows --no-header-files --no-man-pages --strip-debug --compress=2

jlink --module-path /home/installed/Documents/openjdk/jdk-11.0.2.jdk-mac/Contents/Home/jmods:mods  --add-modules java.base,java.desktop,java.logging,jdk.unsupported --output /home/installed/Documents/Terrarum/out/runtime-osx --no-header-files --no-man-pages --strip-debug --compress=2

This process assumes that the game does NOT use the Java 9+ modules and every single required libraries are fat-jar'd (their contents extracted right into the Jar)

### Packaging

Create an output directory; its contents shall be:

```
+assets
+runtime-linux
+runtime-osx
+runtime-windows
start_game_linux.sh
start_game_mac.sh
start_game_windows.bat
TerrarumBuild.jar
```

whereas `runtime-*` are runtime directories generated from the commands above, `TerrarumBuild.jar` is the artifact built using the TerrarumBuild.

`start_game_*` files are on the root directory of the project; you only need to simply copy them over.