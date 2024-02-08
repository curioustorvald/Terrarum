#!/bin/bash

./build_app_linux_x86.sh
./build_app_mac_arm.sh
./build_app_mac_x86.sh
./build_app_windows_x86.sh

echo "IMPORTANT NOTE:"
echo "ARM executable cannot be created on AMD64 platform (seems to be a bug in the appimagetool. Use actual ARM Linux to build ARM Linux executable."