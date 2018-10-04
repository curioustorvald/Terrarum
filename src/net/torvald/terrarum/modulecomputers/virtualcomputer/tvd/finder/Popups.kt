package net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.finder

import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

/**
 * Created by SKYHi14 on 2017-04-01.
 */
object Popups {
    val okCancel = arrayOf("OK", "Cancel")

}

class OptionDiskNameAndCap {
    val name = JTextField(11)
    val capacity = JSpinner(SpinnerNumberModel(
            368640L.toJavaLong(),
            0L.toJavaLong(),
            (1L shl 38).toJavaLong(),
            1L.toJavaLong()
    )) // default 360 KiB, MAX 256 GiB
    val mainPanel = JPanel()
    val settingPanel = JPanel()

    init {
        mainPanel.layout = BorderLayout()
        settingPanel.layout = GridLayout(2, 2, 2, 0)

        //name.text = "Unnamed"

        settingPanel.add(JLabel("Name (max 32 bytes)"))
        settingPanel.add(name)
        settingPanel.add(JLabel("Capacity (bytes)"))
        settingPanel.add(capacity)

        mainPanel.add(settingPanel, BorderLayout.CENTER)
        mainPanel.add(JLabel("Set capacity to 0 to make the disk read-only"), BorderLayout.SOUTH)
    }

    /**
     * returns either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    fun showDialog(title: String): Int {
        return JOptionPane.showConfirmDialog(null, mainPanel,
                title, JOptionPane.OK_CANCEL_OPTION)
    }
}

fun kotlin.Long.toJavaLong() = java.lang.Long(this)

class OptionFileNameAndCap {
    val name = JTextField(11)
    val capacity = JSpinner(SpinnerNumberModel(
            4096L.toJavaLong(),
            0L.toJavaLong(),
            ((1L shl 48) - 1L).toJavaLong(),
            1L.toJavaLong()
    )) // default 360 KiB, MAX 256 TiB
    val mainPanel = JPanel()
    val settingPanel = JPanel()

    init {
        mainPanel.layout = BorderLayout()
        settingPanel.layout = GridLayout(2, 2, 2, 0)

        //name.text = "Unnamed"

        settingPanel.add(JLabel("Name (max 32 bytes)"))
        settingPanel.add(name)
        settingPanel.add(JLabel("Capacity (bytes)"))
        settingPanel.add(capacity)

        mainPanel.add(settingPanel, BorderLayout.CENTER)
    }

    /**
     * returns either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    fun showDialog(title: String): Int {
        return JOptionPane.showConfirmDialog(null, mainPanel,
                title, JOptionPane.OK_CANCEL_OPTION)
    }
}

class OptionSize {
    val capacity = JSpinner(SpinnerNumberModel(
            368640L.toJavaLong(),
            0L.toJavaLong(),
            (1L shl 38).toJavaLong(),
            1L.toJavaLong()
    )) // default 360 KiB, MAX 256 GiB
    val settingPanel = JPanel()

    init {
        settingPanel.add(JLabel("Size (bytes)"))
        settingPanel.add(capacity)
    }

    /**
     * returns either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    fun showDialog(title: String): Int {
        return JOptionPane.showConfirmDialog(null, settingPanel,
                title, JOptionPane.OK_CANCEL_OPTION)
    }
}
