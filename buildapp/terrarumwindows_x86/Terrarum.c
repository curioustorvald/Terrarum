#include <windows.h>
#include <stdio.h>
#include <tchar.h>

int main() {

    ShellExecute(NULL, "open", "\".\\out\\runtime-windows-x86\\bin\\Terrarum.exe\"", "-jar \".\\out\\TerrarumBuild.jar\"", NULL, SW_HIDE);
    return 0;

    //return system(".\\out\\runtime-windows-x86\\bin\\Terrarum.exe -jar .\\out\\TerrarumBuild.jar");
}