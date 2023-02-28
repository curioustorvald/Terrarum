#!/bin/bash
cd "${0%/*}"
./runtime-osx-arm/bin/java -XstartOnFirstThread -Xms1G -Xmx6G -jar ./assets/TerrarumBuild.jar
