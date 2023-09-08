package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.Yaml



object UITitleRemoConYaml {

    /**
     * YAML indent with a space, separate label and class with " : " (\x20\x3A\x20)
     *
     * The class must be the UICanvas
     */
    val menuBase = """
- MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadSavegame
- MENU_OPTIONS
 - MENU_LABEL_GRAPHICS : net.torvald.terrarum.modulebasegame.ui.UIGraphicsControlPanel
 - MENU_OPTIONS_CONTROLS : net.torvald.terrarum.modulebasegame.ui.UIKeyboardControlPanel
 - MENU_LABEL_IME : net.torvald.terrarum.modulebasegame.ui.UIIMEConfig
 - MENU_LABEL_SOUND : net.torvald.terrarum.modulebasegame.ui.UISoundControlPanel
 - MENU_LABEL_LANGUAGE : net.torvald.terrarum.modulebasegame.ui.UITitleLanguage
 - GAME_GENRE_MISC : net.torvald.terrarum.modulebasegame.ui.UIPerformanceControlPanel
 - MENU_MODULES : net.torvald.terrarum.ModOptionsHost
 - MENU_LABEL_RETURN+WRITETOCONFIG
- MENU_MODULES : net.torvald.terrarum.modulebasegame.ui.UITitleModules
 - MENU_LABEL_RETURN
- MENU_LABEL_CREDITS
 - MENU_LABEL_COPYRIGHT : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
 - MENU_CREDIT_GPL_DNT : net.torvald.terrarum.modulebasegame.ui.UITitleGPL3
 - MENU_LABEL_SYSTEM_INFO : net.torvald.terrarum.modulebasegame.ui.UISystemInfo
 - MENU_LABEL_RETURN
- MENU_LABEL_QUIT
"""

    val menuWithSavefile = """
- MENU_LABEL_CONTINUE : net.torvald.terrarum.modulebasegame.ui.UIProxyLoadLatestSave
"""

    val menuNewGame = """
"""

    // todo add MENU_IO_IMPORT
    val injectedMenuSingleCharSel = """
- MENU_IO_IMPORT : net.torvald.terrarum.modulebasegame.ui.UIImportAvatar
- CONTEXT_CHARACTER_NEW : net.torvald.terrarum.modulebasegame.ui.UINewCharacter
- MENU_LABEL_RETURN
"""

    val injectedMenuSingleWorldSel = """
- CONTEXT_WORLD_NEW : net.torvald.terrarum.modulebasegame.ui.UINewWorld
- MENU_LABEL_RETURN
"""

    val injectedMenuSingleSaveManage = """
- MENU_LABEL_RETURN     
"""

    operator fun invoke(hasSave: Boolean) =
//            Yaml((if (hasSave) menuWithSavefile else menuNewGame) + menuBase).parse()
            Yaml(menuBase).parse()
}

