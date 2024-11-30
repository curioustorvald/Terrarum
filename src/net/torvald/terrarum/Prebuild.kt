package net.torvald.terrarum

import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime

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

private fun markVersionNumberAndBuildDate(major: Int, minor: Int, patch: Int) {
    val s = """name=${TerrarumAppConfiguration.GAME_NAME}
version=$major.$minor.$patch
builddate=${ZonedDateTime.now(ZoneOffset.UTC).toString().substring(0, 10)}
"""
    val f = File("./out/build_autogen_buildinfo.properties")
    f.delete()
    f.writeText(s)
}

fun makeBasegameMetadata(major: Int, minor: Int, patch: Int) {
    val s = """## this file is auto-generated using Prebuild.kt ##
# The name that will be displayed in-game
propername=${TerrarumAppConfiguration.GAME_NAME}

# The description
description=The game

# Translations to the description
# Two-character language code is interpreted as a superset of four-character code.
# For example, description_pt will be used for both ptPT and ptBR
description_bgBG=\u0418\u0433\u0440\u0430\u0442\u0430
description_csCZ=Hra
description_daDK=Spillet
description_de=Das Spiel
description_elGR=\u03A4\u03BF \u03C0\u03B1\u03B9\u03C7\u03BD\u03AF\u03B4\u03B9
description_es=El juego
description_fiFI=Peli
description_frFR=Le jeu
description_hiIN=\u0916\u0947\u0932
description_huHU=A j\u00E1t\u00E9k
description_isIC=Leikurinn
description_it=Il gioco
description_jaJP=\u57FA\u672C\u30B2\u30FC\u30E0
description_koKR=\uAE30\uBCF8 \uAC8C\uC784
description_nlNL=Het spel
description_noNB=Spillet
description_pt=O jogo
description_roRO=Joculzr
description_ruRU=\u0418\u0433\u0440\u0430
description_svSE=Spelet
description_thTH=\u0E40\u0E01\u0E21
description_trTR=Oyun
description_zhCN=\u57FA\u672C\u6E38\u620F
description_zhTW=\u57FA\u672C\u904A\u6232

# Author of the module
author=CuriousTo\uA75Bvald

# Root package name for the module
# The game will look for certain classes based on this package string, so don't mess up!
package=net.torvald.terrarum.modulebasegame

# Name of the entry script
# Entry script must inherit net.torvald.terrarum.ModuleEntryPoint
entrypoint=net.torvald.terrarum.modulebasegame.EntryPoint

# Release date in YYYY-MM-DD
releasedate=${ZonedDateTime.now(ZoneOffset.UTC).toString().substring(0, 10)}

# The version, must follow Semver 2.0.0 scheme (https://semver.org/)
version=$major.$minor.$patch

# External JAR that the module is compiled. If your module requires yet another library, the JAR must be compiled as a "Fatjar";
# Due to security reasons, loading an arbitrary JAR is not allowed.
jar=

# Sha256sum of the External JAR, if any
# The hash will not be checked if the game is running on the development mode
jarhash=

# Modules that must be pre-installed, separated by semicolons (;)
# Dependency syntax: "module's identification name (aka folder name) spaces allowed versionnumber"
# Version number:
# - a.b.c : the exact version a.b.c
# - a.b.c+ : Any version between a.b.c and a.b.16777215
# - a.b.* : Any version between a.b.0 and a.b.16777215
# - a.b+ : Any version between a.b.0 and a.16777215.16777215
# - a.* : Any version between a.0.0 and a.16777215.16777215
# - * : Any version between 0.0.0 and 65535.16777215.16777215
dependency=
"""
    val f = File("./assets/mods/basegame/metadata.properties")
    f.delete()
    f.writeText(s)
}

fun main() {
    val major = (App.VERSION_RAW ushr 48).toInt()
    val minor = ((App.VERSION_RAW and 0xffff000000L) ushr 24).toInt()
    val patch = (App.VERSION_RAW and 0xffffffL).toInt()

    markVersionNumberAndBuildDate(major, minor, patch)

    writeLinuxDesktop(major, minor, patch)
    writeOSXPlist(major, minor, patch)
    writeWindowsRC(major, minor, patch)
    makeBasegameMetadata(major, minor, patch)
}