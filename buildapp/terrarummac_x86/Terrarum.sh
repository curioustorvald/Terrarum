#!/bin/bash
cd "${0%/*}"
./runtime-osx-x86/bin/java -XstartOnFirstThread -jar ./out/TerrarumBuild.jar
