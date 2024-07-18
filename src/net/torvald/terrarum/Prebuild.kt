package net.torvald.terrarum

import java.io.File

/**
 * Created by minjaesong on 2023-08-25.
 */

private fun writeWindowsRC(major: Int, minor: Int, patch: Int) {
    val s = """1 ICON "icns.ico"
VS_VERSION_INFO VERSIONINFO
    FILEVERSION    $major,$minor,$patch,0
    PRODUCTVERSION $major,$minor,$patch,0
{
    BLOCK "StringFileInfo"
    {
        BLOCK "040904b0"
        {
            VALUE "FileDescription",    "${TerrarumAppConfiguration.GAME_NAME}\0"
            VALUE "FileVersion",        "$major.$minor.$patch\0"
            VALUE "LegalCopyright",     "${TerrarumAppConfiguration.COPYRIGHT_DATE_NAME}\0"
            VALUE "OriginalFilename",   "${TerrarumAppConfiguration.GAME_NAME}.exe\0"
            VALUE "ProductName",        "${TerrarumAppConfiguration.GAME_NAME}\0"
            VALUE "ProductVersion",     "$major.$minor.$patch\0"
        }
    }
    BLOCK "VarFileInfo"
    {
        VALUE "Translation", 0x409, 1200
    }
}"""
    val f = File("./out/build_autogen_windows.rc")
    f.delete()
    f.writeText(s)
}

private fun writeOSXPlist(major: Int, minor: Int, patch: Int) {
    val s = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleExecutable</key><string>${TerrarumAppConfiguration.GAME_NAME}.sh</string>
<key>CFBundleDisplayName</key><string>${TerrarumAppConfiguration.GAME_NAME}</string>
<key>CFBundleName</key><string>${TerrarumAppConfiguration.GAME_NAME}</string>
<key>CFBundleIconFile</key><string>AppIcon.icns</string>
<key>CFBundleVersion</key><string>$major.$minor.$patch</string>
<key>LSApplicationCategoryType</key><string>public.app-category.games</string>
</dict></plist>"""
    val f = File("./out/build_autogen_macos_Info.plist")
    f.delete()
    f.writeText(s)
}

private fun writeLinuxDesktop(major: Int, minor: Int, patch: Int) {
    val s = """[Desktop Entry]
Name=${TerrarumAppConfiguration.GAME_NAME}
Exec=AppRun
Icon=icns
Type=Application
Version=1.0
Categories=Game;"""
    val f = File("./out/build_autogen_linux.desktop")
    f.delete()
    f.writeText(s)
}

fun main() {
    val major = (App.VERSION_RAW ushr 48).toInt()
    val minor = ((App.VERSION_RAW and 0xffff000000L) ushr 24).toInt()
    val patch = (App.VERSION_RAW and 0xffffffL).toInt()

    writeLinuxDesktop(major, minor, patch)
    writeOSXPlist(major, minor, patch)
    writeWindowsRC(major, minor, patch)
}