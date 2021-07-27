package net.torvald.terrarum.modulebasegame.ui

/*class UITitleRemoConRoot : UICanvas() {

    companion object {
        val remoConWidth = 240
        fun getRemoConHeight(menu: ArrayList<String>) = 36 * menu.size.plus(1)
        fun getRemoConHeight(menu: Array<String>) = 36 * menu.size.plus(1)
        val menubarOffY: Int; get() = AppLoader.terrarumAppConfig.screenH / 2 - (AppLoader.fontGame.lineHeight * 1.5).toInt()
    }


    /** Contains STRING_IDs */
    val menuLabels = arrayListOf(
            "MENU_MODE_SINGLEPLAYER",
            "MENU_OPTIONS",
            "MENU_MODULES",
            "MENU_LABEL_LANGUAGE",
            "MENU_LABEL_CREDITS",
            "MENU_LABEL_QUIT"
    )

    init {
        if (AppLoader.getConfigBoolean("__debug")) {
            menuLabels.addAll(arrayOf(
                    " Development Tools $",
                    "Building Maker"
            ))
        }
    }


    override var width: Int = remoConWidth
    override var height: Int = getRemoConHeight(menuLabels)
    override var openCloseTime = 0f


    private val menubar = UIItemTextButtonList(
            this,
            menuLabels.toTypedArray(),
            0, menubarOffY,
            this.width, this.height,
            textAreaWidth = this.width,
            readFromLang = true,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )


    private val remoConCredits = UITitleRemoConCredits(this)
    private val remoConLanguage = UITitleLanguage(this)
    private val remoConModules = UITitleModules(this)

    init {
        remoConLanguage.setPosition(0, 0)
        remoConCredits.setPosition(0, 0)
        remoConModules.setPosition(0, 0)



        addSubUI(remoConLanguage)
        addSubUI(remoConCredits)
        addSubUI(remoConModules)


        ////////////////////////////

        uiItems.add(menubar)



        // attach listeners

        // TEST TEST TEST
        menubar.buttons[menuLabels.indexOf("MENU_MODE_SINGLEPLAYER")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)

            Terrarum.ingame = Ingame(Terrarum.batch)
            (Terrarum.ingame!! as Ingame).gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
            (Terrarum.ingame!! as Ingame).gameLoadMode = Ingame.GameLoadMode.CREATE_NEW
            LoadScreen.screenToLoad = (Terrarum.ingame!! as Ingame)
            AppLoader.setScreen(LoadScreen)
        }



        menubar.buttons[menuLabels.indexOf("MENU_MODULES")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            remoConModules.setAsOpen()
        }
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_LANGUAGE")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            remoConLanguage.setAsOpen()
        }
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_CREDITS")].clickOnceListener = { _, _, _ ->
            this.setAsClose()
            Thread.sleep(50)
            remoConCredits.setAsOpen()
        }
        menubar.buttons[menuLabels.indexOf("MENU_LABEL_QUIT")].clickOnceListener = { _, _, _ -> Thread.sleep(50); System.exit(0) }


        if (AppLoader.getConfigBoolean("__debug")) {

            menubar.buttons[menuLabels.indexOf("Building Maker")].clickOnceListener = { _, _, _ ->
                this.setAsClose()
                Thread.sleep(50)

                val maker = BuildingMaker(Terrarum.batch)
                Terrarum.ingame = maker
                AppLoader.setScreen(maker)
            }
        }
    }

    override fun updateUI(delta: Float) {
        menubar.update(delta)
        //println("UITitleRemoConRoot bro u even updatez")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        menubar.render(batch, camera)
    }

    override fun doOpening(delta: Float) {

    }

    override fun doClosing(delta: Float) {

    }

    override fun endOpening(delta: Float) {

    }

    override fun endClosing(delta: Float) {

    }

    override fun dispose() {

    }

}*/