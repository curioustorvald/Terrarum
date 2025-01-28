package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.App
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import java.io.File
import java.util.UUID

/**
 * The base class for anything that holds a file like disc/disks
 *
 * Created by minjaesong on 2024-01-13.
 */
open class ItemFileRef(originalID: ItemID) : GameItem(originalID) {

    var author = ""
    var collection = ""

    open var uuid: UUID = UUID(0, 0)

    /**
     * String of path within the module (if not refIsShared), or path under savedir/Shared/
     */
    open var refPath: String = ""

    /**
     * Module name (if not refIsShared), or empty string
     */
    open var refModuleName: String = ""

    /**
     * If the file must be found under `savedir/Shared`
     */
    open var refIsShared: Boolean = false

    @Transient open lateinit var ref: File

    /**
     * Application-defined.
     *
     * For Terrarum:
     * - "music_disc" for music discs used by Jukebox
     *
     * For dwarventech:
     * - "floppy_oem" (oem: for "commercial" software)
     * - "floppy_small"
     * - "floppy_mid"
     * - "floppy_large"
     * - "floppy_debug"
     * - "floppy_homebrew"
     * - "harddisk_small"
     * - "harddisk_mid"
     * - "harddisk_large"
     * - "harddisk_debug"
     * - "eeprom_bios" (bios chips only)
     * - "eeprom_64k" (extra rom for custom motherboards)
     *
     */
    open var mediumIdentifier = ""

    /**
     * How this item should look like on inventory/in world. Used when creation of subclass is not possible.
     */
    open var morphItem = ""

    /**
     * Fully-qualified classname. Class to be called when this item is used by hitting "interaction" key.
     * Used when creation of subclass is not possible.
     *
     * If specified, the class must implement FileRefItemPrimaryUseHandler
     */
    open var useItemHandler = ""


    override var baseMass = 1.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.MISC
    override val canBeDynamic = false
    override val materialId = ""
    override var equipPosition = EquipPosition.HAND_GRIP

    fun getAsGdxFile() = if (refIsShared)
        Gdx.files.external(App.saveSharedDir + "/$refPath")
    else
        ModMgr.getGdxFile(refModuleName, refPath)

    fun getAsFile() = if (refIsShared)
        File(App.saveSharedDir + "/$refPath")
    else
        ModMgr.getFile(refModuleName, refPath)

    @Transient private var classCache: FileRefItemPrimaryUseHandler? = null

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return if (useItemHandler.isNotBlank()) {
            try {
                if (classCache == null) {
                    val newClass = Class.forName(useItemHandler)
                    val newClassConstructor = newClass.getConstructor(/* no args defined */)
                    val newClassInstance = newClassConstructor.newInstance(/* no args defined */)
                    classCache = (newClassInstance as FileRefItemPrimaryUseHandler)
                }
                classCache!!.use(this)
            }
            catch (e: Throwable) {
                e.printStackTrace()
                super.startPrimaryUse(actor, delta)
            }
        }
        else super.startPrimaryUse(actor, delta)
    }
}

interface FileRefItemPrimaryUseHandler {
    /** If this item must be consumed, return 1; if this item must not be consumed, return 0; if this item
     * was failed to be used (for some reason), return -1. */
    fun use(item: ItemFileRef): Long
}