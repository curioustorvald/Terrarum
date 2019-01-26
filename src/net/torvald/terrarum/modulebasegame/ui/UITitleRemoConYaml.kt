package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Yaml



object UITitleRemoConYaml {

    /**
     * YAML indent with a space, separate label and class with " : " (\x20\x3A\x20)
     *
     * The class must be the UICanvas
     */
    val menus = """
        - MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UITitleCharactersList
         - CONTEXT_CHARACTER_NEW
         - CONTEXT_CHARACTER_DELETE
         - MENU_LABEL_RETURN
        - MENU_MODE_MULTIPLAYER
         - MENU_LABEL_RETURN
        - MENU_OPTIONS
         - MENU_OPTIONS_GRAPHICS
         - MENU_OPTIONS_CONTROLS
          - MENU_CONTROLS_KEYBOARD
          - MENU_CONTROLS_GAMEPAD
          - MENU_LABEL_RETURN
         - MENU_OPTIONS_SOUND
         - MENU_LABEL_RETURN
        - MENU_MODULES : net.torvald.terrarum.modulebasegame.ui.UITitleModules
         - MENU_LABEL_RETURN
        - MENU_LABEL_LANGUAGE : net.torvald.terrarum.modulebasegame.ui.UITitleLanguage
         - MENU_LABEL_RETURN
        - MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
         - MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
         - MENU_CREDIT_GPL_DNT : net.torvald.terrarum.modulebasegame.ui.UITitleGPL3
         - MENU_LABEL_RETURN
        - MENU_LABEL_QUIT
        """.trimIndent()

    val debugTools = """
        -  Development Tools $
         - Building Maker : net.torvald.terrarum.modulebasegame.ui.UIProxyNewBuildingMaker
         - Start New Random Game : net.torvald.terrarum.modulebasegame.ui.UIProxyNewRandomGame
         - MENU_LABEL_RETURN
        """.trimIndent()

    operator fun invoke() = if (AppLoader.IS_DEVELOPMENT_BUILD)
        Yaml(menus + "\n" + debugTools).parse()
    else
        Yaml(menus).parse()
}

