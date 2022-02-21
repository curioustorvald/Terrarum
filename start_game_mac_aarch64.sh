#!/bin/bash
cd "${0%/*}"
./runtime-osx-aarch64/bin/java -Xms1G -Xmx6G -jar TerrarumBuild.jar
