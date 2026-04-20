#!/bin/bash
cd "${0%/*}"
GRAAL_MODULE_PATH=compiler-23.1.10.jar:compiler-management-23.1.10.jar:truffle-compiler-23.1.10.jar:truffle-api-23.1.10.jar:truffle-runtime-23.1.10.jar:polyglot-23.1.10.jar:collections-23.1.10.jar:word-23.1.10.jar:nativeimage-23.1.10.jar:jniutils-23.1.10.jar
./out/runtime-osx-arm/bin/java --upgrade-module-path=$GRAAL_MODULE_PATH -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --add-exports=java.base/jdk.internal.misc=jdk.internal.vm.compiler -jar ./out/TerrarumBuild.jar
