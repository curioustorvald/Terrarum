package net.torvald.terrarum.virtualcomputer.tvd.finder

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.charset.Charset
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.ListSelectionModel
import javax.swing.text.DefaultCaret


/**
 * Created by SKYHi14 on 2017-04-01.
 */
class VirtualDiskCracker(val sysCharset: Charset = Charsets.UTF_8) : JFrame() {


    private val annoyHackers = true // Jar build settings. Intended for Terrarum proj.


    private val PREVIEW_MAX_BYTES = 4L * 1024 // 4 kBytes

    private val appName = "TerranVirtualDiskCracker"
    private val copyright = "Copyright 2017 Torvald (minjaesong). Distributed under MIT license."

    private val magicOpen = "I solemnly swear that I am up to no good."
    private val magicSave = "Mischief managed."
    private val annoyWhenLaunchMsg = "Type in following to get started:\n$magicOpen"
    private val annoyWhenSaveMsg = "Type in following to save:\n$magicSave"

    private val panelMain = JPanel()
    private val menuBar = JMenuBar()
    private val tableFiles: JTable
    private val fileDesc = JTextArea()
    private val diskInfo = JTextArea()
    private val statBar = JLabel("Open a disk or create new to get started")

    private var vdisk: VirtualDisk? = null
    private var clipboard: DiskEntry? = null

    private val labelPath = JLabel("(root)")
    private var currentDirectoryEntries: Array<DiskEntry>? = null
    private val directoryHierarchy = Stack<EntryID>(); init { directoryHierarchy.push(0) }

    private fun gotoSubDirectory(id: EntryID) {
        directoryHierarchy.push(id)
        labelPath.text = vdisk!!.entries[id]!!.getFilenameString(sysCharset)
        selectedFile = null
        fileDesc.text = ""
        updateDiskInfo()
    }
    val currentDirectory: EntryID
        get() = directoryHierarchy.peek()
    val upperDirectory: EntryID
        get() = if (directoryHierarchy.lastIndex == 0) 0
                else directoryHierarchy[directoryHierarchy.lastIndex - 1]
    private fun gotoRoot() {
        directoryHierarchy.removeAllElements()
        directoryHierarchy.push(0)
        selectedFile = null
        fileDesc.text = ""
        updateDiskInfo()
    }
    private fun gotoParent() {
        if (directoryHierarchy.size > 1)
            directoryHierarchy.pop()
        selectedFile = null
        fileDesc.text = ""
        updateDiskInfo()
    }



    private var selectedFile: EntryID? = null

    val tableColumns = arrayOf("Name", "Date Modified", "Size")
    val tableParentRecord = arrayOf(arrayOf("..", "", ""))

    init {

        if (annoyHackers) {
            val mantra = JOptionPane.showInputDialog(annoyWhenLaunchMsg)
            if (mantra != magicOpen) {
                System.exit(1)
            }
        }



        panelMain.layout = BorderLayout()
        this.defaultCloseOperation = JFrame.EXIT_ON_CLOSE


        tableFiles = JTable(tableParentRecord, tableColumns)
        tableFiles.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val table = e.source as JTable
                val row = table.rowAtPoint(e.point)


                selectedFile = if (row > 0)
                    currentDirectoryEntries!![row - 1].entryID
                else
                    null // clicked ".."


                fileDesc.text = if (selectedFile != null) {
                    getFileInfoText(vdisk!!.entries[selectedFile!!]!!)
                }
                else
                    ""

                fileDesc.caretPosition = 0

                // double click
                if (e.clickCount == 2) {
                    if (row == 0) {
                        gotoParent()
                    }
                    else {
                        val record = currentDirectoryEntries!![row - 1]
                        if (record.contents is EntryDirectory) {
                            gotoSubDirectory(record.entryID)
                        }
                    }
                }
            }
        })
        tableFiles.selectionModel = object : DefaultListSelectionModel() {
            init { selectionMode = ListSelectionModel.SINGLE_SELECTION }
            override fun clearSelection() { } // required!
            override fun removeSelectionInterval(index0: Int, index1: Int) { } // required!
            override fun fireValueChanged(isAdjusting: Boolean) { } // required!
        }
        tableFiles.model = object : AbstractTableModel() {
            override fun getRowCount(): Int {
                return if (vdisk != null)
                    1 + (currentDirectoryEntries?.size ?: 0)
                else 1
            }

            override fun getColumnCount() = tableColumns.size

            override fun getColumnName(column: Int) = tableColumns[column]

            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                if (rowIndex == 0) {
                    return tableParentRecord[0][columnIndex]
                }
                else {
                    if (vdisk != null) {
                        val entry = currentDirectoryEntries!![rowIndex - 1]
                        return when(columnIndex) {
                            0 -> entry.getFilenameString(sysCharset)
                            1 -> Instant.ofEpochSecond(entry.modificationDate).
                                    atZone(TimeZone.getDefault().toZoneId()).
                                    format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            2 -> entry.getEffectiveSize()
                            else -> ""
                        }
                    }
                    else {
                        return ""
                    }
                }
            }
        }



        val menuFile = JMenu("File")
        menuFile.mnemonic = KeyEvent.VK_F
        menuFile.add("New Disk…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                try {
                    val makeNewDisk: Boolean
                    if (vdisk != null) {
                        makeNewDisk = confirmedDiscard()
                    }
                    else {
                        makeNewDisk = true
                    }
                    if (makeNewDisk) {
                        // inquire new size
                        val dialogBox = OptionDiskNameAndCap()
                        val confirmNew = JOptionPane.OK_OPTION == dialogBox.showDialog("Set Property of New Disk")

                        if (confirmNew) {
                            vdisk = VDUtil.createNewDisk(
                                    (dialogBox.capacity.value as Long).toLong(),
                                    dialogBox.name.text,
                                    sysCharset
                            )
                            gotoRoot()
                            updateDiskInfo()
                            setStat("Disk created")
                        }
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    popupError(e.toString())
                }
            }
        })
        menuFile.add("Open Disk…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                val makeNewDisk: Boolean
                if (vdisk != null) {
                    makeNewDisk = confirmedDiscard()
                }
                else {
                    makeNewDisk = true
                }
                if (makeNewDisk) {
                    val fileChooser = JFileChooser()
                    fileChooser.showOpenDialog(null)
                    if (fileChooser.selectedFile != null) {
                        try {
                            vdisk = VDUtil.readDiskArchive(fileChooser.selectedFile, Level.WARNING, { popupWarning(it) }, sysCharset)
                            if (vdisk != null) {
                                gotoRoot()
                                updateDiskInfo()
                                setStat("Disk loaded")
                            }
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            popupError(e.toString())
                        }
                    }
                }
            }
        })
        menuFile.addSeparator()
        menuFile.add("Save Disk as…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {


                    if (annoyHackers) {
                        val mantra = JOptionPane.showInputDialog(annoyWhenSaveMsg)
                        if (mantra != magicSave) {
                            popupError("Nope!")
                            return
                        }
                    }



                    val fileChooser = JFileChooser()
                    fileChooser.showSaveDialog(null)
                    if (fileChooser.selectedFile != null) {
                        try {
                            VDUtil.dumpToRealMachine(vdisk!!, fileChooser.selectedFile)
                            setStat("Disk saved")
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            popupError(e.toString())
                        }
                    }
                }
            }
        })
        menuBar.add(menuFile)

        val menuEdit = JMenu("Edit")
        menuEdit.mnemonic = KeyEvent.VK_E
        menuEdit.add("New File…").addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val dialogBox = OptionFileNameAndCap()
                        val confirmNew = JOptionPane.OK_OPTION == dialogBox.showDialog("Set Property of New File")

                        if (confirmNew) {
                            if (VDUtil.nameExists(vdisk!!, dialogBox.name.text, currentDirectory, sysCharset)) {
                                popupError("The name already exists")
                            }
                            else {
                                VDUtil.createNewBlankFile(
                                        vdisk!!,
                                        currentDirectory,
                                        (dialogBox.capacity.value as Long).toLong(),
                                        dialogBox.name.text,
                                        sysCharset
                                )
                                updateDiskInfo()
                                setStat("File created")
                            }
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.add("New Directory…").addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    val newname = JOptionPane.showInputDialog("Enter a new directory name:")
                    if (newname != null) {
                        try {
                            if (VDUtil.nameExists(vdisk!!, newname, currentDirectory, sysCharset)) {
                                popupError("The name already exists")
                            }
                            else {
                                VDUtil.addDir(vdisk!!, currentDirectory, newname.toEntryName(DiskEntry.NAME_LENGTH, sysCharset))
                                updateDiskInfo()
                                setStat("Directory created")
                            }
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            popupError(e.toString())
                        }
                    }
                }
            }
        })
        menuEdit.addSeparator()
        menuEdit.add("Cut").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                // copy
                clipboard = vdisk!!.entries[selectedFile]

                // delete
                if (vdisk != null && selectedFile != null) {
                    try {
                        VDUtil.deleteFile(vdisk!!, selectedFile!!)
                        updateDiskInfo()
                        setStat("File deleted")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.add("Copy").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                clipboard = vdisk!!.entries[selectedFile]
                setStat("File copied")
            }
        })
        menuEdit.add("Paste").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                fun paste1(newname: ByteArray) {
                    try {
                        VDUtil.addFile(vdisk!!, currentDirectory, DiskEntry(// clone
                                vdisk!!.generateUniqueID(),
                                currentDirectory,
                                newname,
                                clipboard!!.creationDate,
                                clipboard!!.modificationDate,
                                clipboard!!.contents
                        ))

                        updateDiskInfo()
                        setStat("File pasted")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
                if (clipboard != null && vdisk != null) {
                    // check name collision. If it is, ask for new one
                    if (VDUtil.nameExists(vdisk!!, clipboard!!.getFilenameString(sysCharset), currentDirectory, sysCharset)) {
                        val newname = JOptionPane.showInputDialog("The name already exists. Enter a new name:")
                        if (newname != null) {
                            paste1(newname.toEntryName(DiskEntry.NAME_LENGTH, sysCharset))
                        }
                    }
                    else {
                        paste1(clipboard!!.filename)
                    }
                }
            }
        })
        menuEdit.add("Paste as Symbolic Link").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                fun pasteSymbolic(newname: ByteArray) {
                    try {
                        // check if the original file is there in the first place
                        if (vdisk!!.entries[clipboard!!.entryID] != null) {
                            val entrySymlink = EntrySymlink(clipboard!!.entryID)
                            VDUtil.addFile(vdisk!!, currentDirectory, DiskEntry(
                                    vdisk!!.generateUniqueID(),
                                    currentDirectory,
                                    newname,
                                    VDUtil.currentUnixtime,
                                    VDUtil.currentUnixtime,
                                    entrySymlink
                            ))

                            updateDiskInfo()
                            setStat("Symbolic link created")
                        }
                        else {
                            popupError("The orignal file is gone")
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
                if (clipboard != null && vdisk != null) {
                    // check name collision. If it is, ask for new one
                    if (VDUtil.nameExists(vdisk!!, clipboard!!.getFilenameString(sysCharset), currentDirectory, sysCharset)) {
                        val newname = JOptionPane.showInputDialog("The name already exists. Enter a new name:")
                        if (newname != null) {
                            pasteSymbolic(newname.toEntryName(DiskEntry.NAME_LENGTH, sysCharset))
                        }
                    }
                    else {
                        pasteSymbolic(clipboard!!.filename)
                    }
                }
            }
        })
        menuEdit.add("Delete").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null && selectedFile != null) {
                    try {
                        VDUtil.deleteFile(vdisk!!, selectedFile!!)
                        updateDiskInfo()
                        setStat("File deleted")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.add("Rename…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (selectedFile != null) {
                    try {
                        val newname = JOptionPane.showInputDialog("Enter a new name:")
                        if (newname != null) {
                            if (VDUtil.nameExists(vdisk!!, newname, currentDirectory, sysCharset)) {
                                popupError("The name already exists")
                            }
                            else {
                                VDUtil.renameFile(vdisk!!, selectedFile!!, newname, sysCharset)
                                updateDiskInfo()
                                setStat("File renamed")
                            }
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.add("Look Clipboard").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                popupMessage(if (clipboard != null)
                    "${clipboard ?: "(bug found)"}"
                else "(nothing)", "Clipboard"
                )
            }

        })
        menuEdit.addSeparator()
        menuEdit.add("Import…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                    fileChooser.isMultiSelectionEnabled = true
                    fileChooser.showOpenDialog(null)
                    if (fileChooser.selectedFiles.isNotEmpty()) {
                        try {
                            fileChooser.selectedFiles.forEach {
                                if (!it.isDirectory) {
                                    val entry = VDUtil.importFile(it, vdisk!!.generateUniqueID(), sysCharset)

                                    val newname: String?
                                    if (VDUtil.nameExists(vdisk!!, entry.filename, currentDirectory)) {
                                        newname = JOptionPane.showInputDialog("The name already exists. Enter a new name:")
                                    }
                                    else {
                                        newname = entry.getFilenameString(sysCharset)
                                    }

                                    if (newname != null) {
                                        entry.filename = newname.toEntryName(DiskEntry.NAME_LENGTH, sysCharset)
                                        VDUtil.addFile(vdisk!!, currentDirectory, entry)
                                    }
                                }
                                else {
                                    val newname: String?
                                    if (VDUtil.nameExists(vdisk!!, it.name.toEntryName(DiskEntry.NAME_LENGTH, sysCharset), currentDirectory)) {
                                        newname = JOptionPane.showInputDialog("The name already exists. Enter a new name:")
                                    }
                                    else {
                                        newname = it.name
                                    }

                                    if (newname != null) {
                                        VDUtil.importDirRecurse(vdisk!!, it, currentDirectory, sysCharset, newname)
                                    }
                                }
                            }
                            updateDiskInfo()
                            setStat("File added")
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            popupError(e.toString())
                        }
                    }

                    fileChooser.isMultiSelectionEnabled = false
                }
            }
        })
        menuEdit.add("Export…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    val file = vdisk!!.entries[selectedFile ?: currentDirectory]!!

                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    fileChooser.isMultiSelectionEnabled = false
                    fileChooser.showSaveDialog(null)
                    if (fileChooser.selectedFile != null) {
                        try {
                            val file = VDUtil.resolveIfSymlink(vdisk!!, file.entryID)
                            if (file.contents is EntryFile) {
                                VDUtil.exportFile(file.contents, fileChooser.selectedFile)
                                setStat("File exported")
                            }
                            else {
                                VDUtil.exportDirRecurse(vdisk!!, file.entryID, fileChooser.selectedFile, sysCharset)
                                setStat("Files exported")
                            }
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            popupError(e.toString())
                        }
                    }
                }
            }
        })
        menuEdit.addSeparator()
        menuEdit.add("Rename Disk…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val newname = JOptionPane.showInputDialog("Enter a new disk name:")
                        if (newname != null) {
                            vdisk!!.diskName = newname.toEntryName(VirtualDisk.NAME_LENGTH, sysCharset)
                            updateDiskInfo()
                            setStat("Disk renamed")
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.add("Resize Disk…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val dialog = OptionSize()
                        val confirmed = dialog.showDialog("Input") == JOptionPane.OK_OPTION
                        if (confirmed) {
                            vdisk!!.capacity = (dialog.capacity.value as Long).toLong()
                            updateDiskInfo()
                            setStat("Disk resized")
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuEdit.addSeparator()
        menuEdit.add("Set/Unset Write Protection").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        vdisk!!.isReadOnly = vdisk!!.isReadOnly.not()
                        updateDiskInfo()
                        setStat("Disk write protection ${if (vdisk!!.isReadOnly) "" else "dis"}engaged")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuBar.add(menuEdit)

        val menuManage = JMenu("Manage")
        menuManage.add("Report Orphans…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val reports = VDUtil.gcSearchOrphan(vdisk!!)
                        val orphansCount = reports.size
                        val orphansSize = reports.map { vdisk!!.entries[it]!!.contents.getSizeEntry() }.sum()
                        val message = "Orphans count: $orphansCount\n" +
                                "Size: ${orphansSize.bytes()}"
                        popupMessage(message, "Orphans Report")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuManage.add("Report Phantoms…").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val reports = VDUtil.gcSearchPhantomBaby(vdisk!!)
                        val phantomsSize = reports.size
                        val message = "Phantoms count: $phantomsSize"
                        popupMessage(message, "Phantoms Report")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuManage.addSeparator()
        menuManage.add("Remove Orphans").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val oldSize = vdisk!!.usedBytes
                        VDUtil.gcDumpOrphans(vdisk!!)
                        val newSize = vdisk!!.usedBytes
                        popupMessage("Saved ${(oldSize - newSize).bytes()}", "GC Report")
                        updateDiskInfo()
                        setStat("Orphan nodes removed")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuManage.add("Full Garbage Collect").addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (vdisk != null) {
                    try {
                        val oldSize = vdisk!!.usedBytes
                        VDUtil.gcDumpAll(vdisk!!)
                        val newSize = vdisk!!.usedBytes
                        popupMessage("Saved ${(oldSize - newSize).bytes()}", "GC Report")
                        updateDiskInfo()
                        setStat("Orphan nodes and null directory pointers removed")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
        menuBar.add(menuManage)

        val menuAbout = JMenu("About")
        menuAbout.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                popupMessage(copyright, "Copyright")
            }
        })
        menuBar.add(menuAbout)



        diskInfo.highlighter = null
        diskInfo.text = "(Disk not loaded)"
        diskInfo.preferredSize = Dimension(-1, 60)

        fileDesc.highlighter = null
        fileDesc.text = ""
        fileDesc.caret.isVisible = false
        (fileDesc.caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE

        val fileDescScroll = JScrollPane(fileDesc)
        val tableFilesScroll = JScrollPane(tableFiles)
        tableFilesScroll.size = Dimension(200, -1)

        val panelFinder = JPanel(BorderLayout())
        panelFinder.add(labelPath, BorderLayout.NORTH)
        panelFinder.add(tableFilesScroll, BorderLayout.CENTER)

        val panelFileDesc = JPanel(BorderLayout())
        panelFileDesc.add(JLabel("Entry Information"), BorderLayout.NORTH)
        panelFileDesc.add(fileDescScroll, BorderLayout.CENTER)

        val filesSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelFinder, panelFileDesc)
        filesSplit.resizeWeight = 0.714285


        val panelDiskOp = JPanel(BorderLayout(2, 2))
        panelDiskOp.add(filesSplit, BorderLayout.CENTER)
        panelDiskOp.add(diskInfo, BorderLayout.SOUTH)


        panelMain.add(menuBar, BorderLayout.NORTH)
        panelMain.add(panelDiskOp, BorderLayout.CENTER)
        panelMain.add(statBar, BorderLayout.SOUTH)


        this.title = appName
        this.add(panelMain)
        this.setSize(700, 700)
        this.isVisible = true
    }

    private fun confirmedDiscard() = 0 == JOptionPane.showOptionDialog(
            null, // parent
            "Any changes to current disk will be discarded. Continue?",
            "Confirm Discard", // window title
            JOptionPane.DEFAULT_OPTION, // option type
            JOptionPane.WARNING_MESSAGE, // message type
            null, // icon
            Popups.okCancel, // options (provided by JOptionPane.OK_CANCEL_OPTION in this case)
            Popups.okCancel[1] // default selection
    )
    private fun popupMessage(message: String, title: String = "") {
        JOptionPane.showOptionDialog(
                null,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, null, null
        )
    }
    private fun popupError(message: String, title: String = "Uh oh…") {
        JOptionPane.showOptionDialog(
                null,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null, null, null
        )
    }
    private fun popupWarning(message: String, title: String = "Careful…") {
        JOptionPane.showOptionDialog(
                null,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, null, null
        )
    }
    private fun updateCurrentDirectory() {
        currentDirectoryEntries = VDUtil.getDirectoryEntries(vdisk!!, currentDirectory)
    }
    private fun updateDiskInfo() {
        val sb = StringBuilder()
        directoryHierarchy.forEach {
            sb.append(vdisk!!.entries[it]!!.getFilenameString(sysCharset))
            sb.append('/')
        }
        sb.dropLast(1)
        labelPath.text = sb.toString()

        diskInfo.text = if (vdisk == null) "(Disk not loaded)" else getDiskInfoText(vdisk!!)
        tableFiles.revalidate()
        tableFiles.repaint()


        updateCurrentDirectory()
    }
    private fun getDiskInfoText(disk: VirtualDisk): String {
        return """Name: ${String(disk.diskName, sysCharset)}
Capacity: ${disk.capacity} bytes (${disk.usedBytes} bytes used, ${disk.capacity - disk.usedBytes} bytes free)
Write protected: ${disk.isReadOnly.toEnglish()}"""
    }


    private fun Boolean.toEnglish() = if (this) "Yes" else "No"


    private fun getFileInfoText(file: DiskEntry): String {
        return """Name: ${file.getFilenameString(sysCharset)}
Size: ${file.getEffectiveSize()}
Type: ${DiskEntry.getTypeString(file.contents)}
CRC: ${file.hashCode().toHex()}
EntryID: ${file.entryID}
ParentID: ${file.parentEntryID}""" + if (file.contents is EntryFile) """

Contents:
${String(file.contents.bytes.sliceArray64(0L..minOf(PREVIEW_MAX_BYTES, file.contents.bytes.size) - 1).toByteArray(), sysCharset)}""" else ""
    }
    private fun Long.bytes() = if (this == 1L) "1 byte" else "$this bytes"
    private fun Int.entries() = if (this == 1) "1 entry" else "$this entries"
    private fun DiskEntry.getEffectiveSize() = if (this.contents is EntryFile)
        this.contents.getSizePure().bytes()
    else if (this.contents is EntryDirectory)
        this.contents.entryCount.entries()
    else if (this.contents is EntrySymlink)
        "(symlink)"
    else
        "n/a"
    private fun setStat(message: String) {
        statBar.text = message
    }
}

fun main(args: Array<String>) {
    VirtualDiskCracker(Charset.forName("CP437"))
}