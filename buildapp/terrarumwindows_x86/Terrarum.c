#include <windows.h>

int main() {
    STARTUPINFOA si = {0};
    PROCESS_INFORMATION pi = {0};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;

    char cmd[] = "\".\\out\\runtime-windows-x86\\bin\\java.exe\" "
                 "--upgrade-module-path=compiler-23.1.10.jar;compiler-management-23.1.10.jar;truffle-compiler-23.1.10.jar;truffle-api-23.1.10.jar;truffle-runtime-23.1.10.jar;polyglot-23.1.10.jar;collections-23.1.10.jar;word-23.1.10.jar;nativeimage-23.1.10.jar;jniutils-23.1.10.jar "
                 "-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI "
                 "--add-exports=java.base/jdk.internal.misc=jdk.internal.vm.compiler "
                 "-jar \".\\out\\TerrarumBuild.jar\"";

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
