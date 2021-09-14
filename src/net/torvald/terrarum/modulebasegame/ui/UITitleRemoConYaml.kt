package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.Yaml



object UITitleRemoConYaml {

    /**
     * YAML indent with a space, separate label and class with " : " (\x20\x3A\x20)
     *
     * The class must be the UICanvas
     */
    private val menuBase = """
- MENU_OPTIONS
 - MENU_OPTIONS_GRAPHICS
 - MENU_OPTIONS_CONTROLS
  - MENU_CONTROLS_KEYBOARD
  - MENU_CONTROLS_GAMEPAD
  - MENU_LABEL_RETURN
 - MENU_OPTIONS_SOUND
 - MENU_LABEL_LANGUAGE : net.torvald.terrarum.modulebasegame.ui.UITitleLanguage
 - MENU_MODULES : net.torvald.terrarum.ModOptionsHost
 - MENU_LABEL_RETURN
- MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
 - MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
 - MENU_CREDIT_GPL_DNT : net.torvald.terrarum.modulebasegame.ui.UITitleGPL3
 - MENU_LABEL_RETURN
- MENU_LABEL_QUIT
"""

    private val menuWithSavefile = """
- MENU_LABEL_CONTINUE : net.torvald.terrarum.modulebasegame.ui.UIProxyLoadLatestSave
- MENU_LABEL_NEW_GAME : net.torvald.terrarum.modulebasegame.ui.UIProxyNewRandomGame
- MENU_IO_LOAD : net.torvald.terrarum.modulebasegame.ui.UILoadDemoSavefiles
 - MENU_LABEL_RETURN
"""

    private val menuNewGame = """
- MENU_LABEL_NEW_GAME : net.torvald.terrarum.modulebasegame.ui.UIProxyNewRandomGame
"""


    operator fun invoke(hasSave: Boolean) =
            Yaml((if (hasSave) menuWithSavefile else menuNewGame) + menuBase).parse()
}

