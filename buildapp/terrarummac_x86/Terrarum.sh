#!/bin/bash
cd "${0%/*}"
./runtime-osx-x86/bin/java -Xms1G -Xmx6G -jar ./assets/TerrarumBuild.jar
