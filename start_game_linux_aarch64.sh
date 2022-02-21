#!/bin/bash
cd "${0%/*}"
./runtimes/runtime-linux-aarch64/bin/java -Xms1G -Xmx6G -jar ./runtimes/TerrarumBuild.jar
