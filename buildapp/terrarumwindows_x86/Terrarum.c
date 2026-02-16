#include <windows.h>

int main() {
    STARTUPINFOA si = {0};
    PROCESS_INFORMATION pi = {0};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;

    char cmd[] = "\".\\out\\runtime-windows-x86\\bin\\java.exe\" -jar \".\\out\\TerrarumBuild.jar\"";

    CreateProcessA(
        NULL,
        cmd,
        NULL,
        NULL,
        FALSE,
        0,
        NULL,
        ".\\out\\runtime-windows-x86\\bin",
        &si,
        &pi
    );

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return 0;
}
