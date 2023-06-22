#!/bin/bash
cd "${0%/*}"
./runtime-osx-arm/bin/java -XstartOnFirstThread -jar ./out/TerrarumBuild.jar
