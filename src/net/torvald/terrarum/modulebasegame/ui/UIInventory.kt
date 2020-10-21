package net.torvald.terrarum.modulebasegame.ui

/**
 * Created by minjaesong on 2017-03-13.
 */
/*class UIInventory(
        var actor: Pocketed?,
        override var width: Int,
        override var height: Int,
        var categoryWidth: Int,

        toggleKeyLiteral: Int? = null, toggleButtonLiteral: Int? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    val inventory: ActorInventory?
        get() = actor?.inventory
    //val actorValue: ActorValue
    //    get() = (actor as Actor).actorValue

    override var openCloseTime: Second = 0.12f

    val catButtonsToCatIdent = HashMap<String, String>()

    val backgroundColour = Color(0x242424_80)
    val defaultTextColour = Color(0xeaeaea_ff.toInt())

    init {
        catButtonsToCatIdent.put("GAME_INVENTORY_WEAPONS", GameItem.Category.WEAPON)
        catButtonsToCatIdent.put("CONTEXT_ITEM_TOOL_PLURAL", GameItem.Category.TOOL)
        catButtonsToCatIdent.put("CONTEXT_ITEM_ARMOR", GameItem.Category.ARMOUR)
        catButtonsToCatIdent.put("GAME_INVENTORY_INGREDIENTS", GameItem.Category.GENERIC)
        catButtonsToCatIdent.put("GAME_INVENTORY_POTIONS", GameItem.Category.POTION)
        catButtonsToCatIdent.put("CONTEXT_ITEM_MAGIC", GameItem.Category.MAGIC)
        catButtonsToCatIdent.put("GAME_INVENTORY_BLOCKS", GameItem.Category.BLOCK)
        catButtonsToCatIdent.put("GAME_INVENTORY_WALLS", GameItem.Category.WALL)
        catButtonsToCatIdent.put("GAME_GENRE_MISC", GameItem.Category.MISC)

        // special filter
        catButtonsToCatIdent.put("MENU_LABEL_ALL", "__all__")

    }

    val itemStripGutterV = 6
    val itemStripGutterH = 8
    val itemInterColGutter = 8

    val controlHelpHeight = AppLoader.fontGame.lineHeight.toInt()

    val pageButtonExtraGap = 32

    val pageButtonRealWidth = pageButtonExtraGap + itemStripGutterH

    private val catButtons = UIItemTextButtonList(
            this,
            arrayOf(
                    "MENU_LABEL_ALL",
                    "GAME_INVENTORY_BLOCKS",
                    "GAME_INVENTORY_WALLS",
                    "CONTEXT_ITEM_TOOL_PLURAL",
                    "GAME_INVENTORY_WEAPONS",
                    "CONTEXT_ITEM_ARMOR",
                    "GAME_INVENTORY_INGREDIENTS",
                    "GAME_INVENTORY_POTIONS",
                    "CONTEXT_ITEM_MAGIC",
                    "GAME_GENRE_MISC"
                    //"GAME_INVENTORY_FAVORITES",
            ),
            posX = 0,
            posY = 0,
            width = categoryWidth,
            height = height - controlHelpHeight,
            verticalGutter = itemStripGutterH,
            readFromLang = true,
            textAreaWidth = 100,
            defaultSelection = 0,
            iconSpriteSheet = TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20),
            iconSpriteSheetIndices = intArrayOf(9,6,7,1,0,2,3,4,5,8),
            iconCol = defaultTextColour,
            highlightBackCol = Color(0xb8b8b8_ff.toInt()),
            highlightBackBlendMode = BlendMode.MULTIPLY,
            backgroundCol = Color(0), // will use custom background colour!
            backgroundBlendMode = BlendMode.NORMAL,
            kinematic = true,
            inactiveCol = defaultTextColour
    )

    val itemsStripWidth = ((width - catButtons.width) - (2 * itemStripGutterH + itemInterColGutter)) / 2 - pageButtonExtraGap
    private val items = Array(
            ((height - controlHelpHeight) / (UIItemInventoryElem.height + itemStripGutterV)) * 2, {
        UIItemInventoryElem(
                parentUI = this,
                posX = pageButtonExtraGap + catButtons.width + if (it % 2 == 0) itemStripGutterH else (itemStripGutterH + itemsStripWidth + itemInterColGutter),
                posY = itemStripGutterH + it / 2 * (UIItemInventoryElem.height + itemStripGutterV),
                width = itemsStripWidth,
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(0x282828_ff),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = Color(0xd4d4d4_ff.toInt()),
                backBlendMode = BlendMode.MULTIPLY,
                drawBackOnNull = true,
                inactiveTextCol = defaultTextColour
        ) })


    private val scrollImageButtonAtlas = TextureRegionPack(
            Gdx.files.internal("assets/graphics/gui/inventory/page_arrow_button.tga"),
            40, 54
    )
    private val scrollLeftButton = UIItemImageButton(this,
            scrollImageButtonAtlas.get(0, 0),
            posX = categoryWidth,
            posY = 0,//(height - controlHelpHeight - scrollImageButtonAtlas.tileH) / 2,
            width = scrollImageButtonAtlas.tileW,
            height = height - controlHelpHeight,
            highlightable = false
    )
    private val scrollRightButton = UIItemImageButton(this,
            scrollImageButtonAtlas.get(1, 0),
            posX = width - scrollImageButtonAtlas.tileW,
            posY = 0,//(height - controlHelpHeight - scrollImageButtonAtlas.tileH) / 2,
            width = scrollImageButtonAtlas.tileW,
            height = height - controlHelpHeight,
            highlightable = false
    )
    var itemPage = 0
    var itemPageCount = 1 // TODO total size of current category / items.size



    var inventorySortList = ArrayList<InventoryPair>()
    private var rebuildList = true

    private val SP = "${0x3000.toChar()}${0x3000.toChar()}"
    val listControlHelp: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}..${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
    else
            "$gamepadLabelWest ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$gamepadLabelEast ${Lang["GAME_INVENTORY_DROP"]}"
    val listControlClose: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe037.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
    else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"

    private var oldCatSelect: Int? = null

    private var encumbrancePerc = 0f
    private var isEncumbered = false


    private val seekLeft: Int;  get() = AppLoader.getConfigInt("keyleft")  // getter used to support in-game keybind changing
    private val seekRight: Int; get() = AppLoader.getConfigInt("keyright") // getter used to support in-game keybind changing
    private val seekUp: Int;    get() = AppLoader.getConfigInt("keyup")    // getter used to support in-game keybind changing
    private val seekDown: Int;  get() = AppLoader.getConfigInt("keydown")  // getter used to support in-game keybind changing


    init {
        // assign actions to the buttons
        scrollLeftButton.clickOnceListener = { mouseX, mouseY, button -> // click once action doesn't work ?!
            if (button == Input.Buttons.LEFT) {
                itemPage = (itemPage - 1) fmod itemPageCount
            }
        }
        scrollRightButton.clickOnceListener = { mouseX, mouseY, button ->
            if (button == Input.Buttons.LEFT) {
                itemPage = (itemPage + 1) fmod itemPageCount
            }
        }


        addItem(scrollLeftButton)
        addItem(scrollRightButton)
    }


    override fun updateUI(delta: Float) {

        catButtons.update(delta)

        scrollLeftButton.update(delta)
        scrollRightButton.update(delta)

        if (actor != null && inventory != null) {
            // monitor and check if category selection has been changed
            // OR UI is being opened from closed state
            if (oldCatSelect != catButtons.selectedIndex ||
                    !rebuildList && handler.openFired) {
                rebuildList = true
            }

            // reset item page to start
            if (oldCatSelect != catButtons.selectedIndex) {
                itemPage = 0
            }

            if (rebuildList) {
                shutUpAndRebuild()
            }
        }


        oldCatSelect = catButtons.selectedIndex
    }

    private val weightBarWidth = 60f

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // background
        blendNormal()
        batch.color = backgroundColour
        batch.fillRect(0f, 0f, width.toFloat(), height.toFloat())


        // cat bar background
        blendMul()
        batch.color = Color(0xcccccc_ff.toInt())
        batch.fillRect(0f, 0f, catButtons.width.toFloat(), height.toFloat())

        catButtons.render(batch, camera)

        // left/right page mover
        scrollLeftButton.render(batch, camera)
        scrollRightButton.render(batch, camera)

        items.forEach {
            it.render(batch, camera)
        }

        // texts
        blendNormal()
        batch.color = defaultTextColour
        // W - close
        AppLoader.fontGame.draw(batch, listControlClose, 4f, height - controlHelpHeight.toFloat())
        // MouseL - Use ; 1.9 - Register ; T - Drop
        AppLoader.fontGame.draw(batch, listControlHelp, catButtons.width + 4f, height - controlHelpHeight.toFloat())
        // encumbrance
        if (inventory != null) {
            val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]

            AppLoader.fontGame.draw(batch,
                    encumbranceText,
                    width - 9 - AppLoader.fontGame.getWidth(encumbranceText) - weightBarWidth,
                    height - controlHelpHeight.toFloat()
            )

            // encumbrance bar background
            blendMul()
            batch.color = Color(0xa0a0a0_ff.toInt())
            batch.fillRect(
                    width - 3 - weightBarWidth,
                    height - controlHelpHeight + 3f,
                    weightBarWidth,
                    controlHelpHeight - 6f
            )
            // encumbrance bar
            blendNormal()
            batch.color = if (isEncumbered) Color(0xff0000_cc.toInt()) else Color(0x00ff00_cc.toInt())
            batch.fillRect(
                    width - 3 - weightBarWidth,
                    height - controlHelpHeight + 3f,
                    if (actor?.inventory?.capacityMode == CAPACITY_MODE_NO_ENCUMBER)
                        1f
                    else // make sure 1px is always be seen
                        minOf(weightBarWidth, maxOf(1f, weightBarWidth * encumbrancePerc)),
                    controlHelpHeight - 5f
            )
        }
    }

    /** Persuade the UI to rebuild its item list */
    fun rebuildList() {
        rebuildList = true
    }


    fun shutUpAndRebuild() {
        if (catButtons.selectedButton != null) {
            val filter = catButtonsToCatIdent[catButtons.selectedButton!!.labelText]

            // encumbrance
            encumbrancePerc = inventory!!.capacity.toFloat() / inventory!!.maxCapacity
            isEncumbered = inventory!!.isEncumbered



            inventorySortList = ArrayList<InventoryPair>()

            // filter items
            inventory?.forEach {
                if (it.item.inventoryCategory == filter || filter == "__all__")
                    inventorySortList.add(it)
            }

            rebuildList = false

            // sort if needed
            // test sort by name
            inventorySortList.sortBy { it.item.name }

            // map sortList to item list
            for (k in items.indices) {
                // we have an item
                try {
                    val sortListItem = inventorySortList[k + itemPage * items.size]
                    items[k].item = sortListItem.item
                    items[k].amount = sortListItem.amount
                    items[k].itemImage = ItemCodex.getItemImage(sortListItem.item)

                    // set quickslot number
                    for (qs in 1..UIQuickslotBar.SLOT_COUNT) {
                        if (sortListItem.item == actor?.inventory?.getQuickslot(qs - 1)?.item) {
                            items[k].quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                            break
                        }
                        else
                            items[k].quickslot = null
                    }

                    // set equippedslot number
                    for (eq in 0..actor!!.inventory.itemEquipped.size - 1) {
                        if (eq < actor!!.inventory.itemEquipped.size) {
                            if (actor!!.inventory.itemEquipped[eq] == items[k].item) {
                                items[k].equippedSlot = eq
                                break
                            }
                            else
                                items[k].equippedSlot = null
                        }
                    }
                }
                // we do not have an item, empty the slot
                catch (e: IndexOutOfBoundsException) {
                    items[k].item = null
                    items[k].amount = 0
                    items[k].itemImage = null
                    items[k].quickslot = null
                }
            }


            itemPageCount = maxOf(1, 1 + (inventorySortList.size.minus(1) / items.size))
        }
    }



    ////////////
    // Inputs //
    ////////////


    override fun doOpening(delta: Float) {
        UICanvas.doOpeningPopOut(this, openCloseTime, UICanvas.Companion.Position.LEFT)
    }

    override fun doClosing(delta: Float) {
        UICanvas.doClosingPopOut(this, openCloseTime, UICanvas.Companion.Position.LEFT)
    }

    override fun endOpening(delta: Float) {
        UICanvas.endOpeningPopOut(this, UICanvas.Companion.Position.LEFT)
    }

    override fun endClosing(delta: Float) {
        UICanvas.endClosingPopOut(this, UICanvas.Companion.Position.LEFT)
    }

    override fun keyDown(keycode: Int): Boolean {
        super.keyDown(keycode)

        items.forEach { if (it.mouseUp) it.keyDown(keycode) }
        shutUpAndRebuild()

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)

        items.forEach { if (it.mouseUp) it.keyUp(keycode) }
        shutUpAndRebuild()

        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        items.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        items.forEach { if (it.mouseUp) it.touchUp(screenX, screenY, pointer, button) }

        return true
    }

    override fun dispose() {
        catButtons.dispose()
        items.forEach { it.dispose() }
        scrollImageButtonAtlas.dispose()
    }
}
*/
