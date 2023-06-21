#include <stdio.h>
#include <stdlib.h>

int main() {
    return system(".\\runtime-windows-x86\\bin\\java -Xms1G -Xmx6G -jar .\\assets\\TerrarumBuild.jar");
}