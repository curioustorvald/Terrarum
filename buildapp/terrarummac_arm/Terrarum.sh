#!/bin/bash
cd "${0%/*}"
./runtime-arm-x86/bin/java -XstartOnFirstThread -Xms1G -Xmx6G -jar ./assets/TerrarumBuild.jar
