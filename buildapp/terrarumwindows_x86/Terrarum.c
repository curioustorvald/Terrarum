#include <windows.h>
#include <stdio.h>
#include <tchar.h>

int main() {
    STARTUPINFOW si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    if (CreateProcessW(
            ".\\out\\runtime-windows-x86\\bin\\Terrarum.exe -jar .\\out\\TerrarumBuild.jar",
            NULL, NULL, NULL, FALSE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {

        WaitForSingleObject(pi.hProcess, INFINITE);
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
    }

    return 0;
    //return system(".\\out\\runtime-windows-x86\\bin\\Terrarum.exe -jar .\\out\\TerrarumBuild.jar");
}