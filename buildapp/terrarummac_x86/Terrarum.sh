#!/bin/bash
cd "${0%/*}"
./runtime-osx-x86/bin/java -XstartOnFirstThread -Xms1G -Xmx6G -jar ./assets/TerrarumBuild.jar
