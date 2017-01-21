package net.torvald.terrarum.debuggerapp

import net.torvald.terrarum.gameactors.Actor
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.util.*
import javax.swing.*

/**
 * Created by minjaesong on 2016-12-29.
 */
class ActorsLister(
        val actorContainer: ArrayList<Actor>,
        val actorContainerInactive:  ArrayList<Actor>) : JFrame() {

    private val activeActorArea = JTextArea()
    private val activeActorScroller = JScrollPane(activeActorArea)
    private val inactiveActorArea = JTextArea()
    private val inactiveActorScroller = JScrollPane(inactiveActorArea)

    private val countsLabel = JLabel()

    init {
        title = "Actors list"
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        activeActorArea.highlighter = null // prevent text-drag-crash
        inactiveActorArea.highlighter = null // prevent text-drag-crash


        val divPanel = JPanel()
        divPanel.layout = BorderLayout(0, 2)

        val activeCard = JPanel()
        activeCard.layout = BorderLayout(0, 2)
        activeCard.add(JLabel("Active actors"), BorderLayout.PAGE_START)
        activeCard.add(activeActorScroller, BorderLayout.CENTER)

        val inactiveCard = JPanel()
        inactiveCard.layout = BorderLayout(0, 2)
        inactiveCard.add(JLabel("Dormant actors"), BorderLayout.PAGE_START)
        inactiveCard.add(inactiveActorScroller, BorderLayout.CENTER)

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, activeCard, inactiveCard)
        splitPane.topComponent.minimumSize = Dimension(0, 24)
        splitPane.bottomComponent.minimumSize = Dimension(0, 24)
        splitPane.resizeWeight = 0.5

        divPanel.add(countsLabel, BorderLayout.PAGE_START)
        divPanel.add(splitPane, BorderLayout.CENTER)


        this.add(divPanel)
        this.setSize(300, 300)
        this.isVisible = true
    }

    fun update() {
        countsLabel.text = "Total: ${actorContainer.size + actorContainerInactive.size}, " +
                           "Active: ${actorContainer.size}, Dormant: ${actorContainerInactive.size}"

        val sb = StringBuilder()

        actorContainer.forEach { sb.append("$it\n") }
        sb.deleteCharAt(sb.length - 1) // delete trailing \n
        activeActorArea.text = "$sb"

        sb.setLength(0) // clear stringbuffer

        actorContainerInactive.forEach { sb.append("$it\n") }
        if (sb.length > 1) {
            sb.deleteCharAt(sb.length - 1) // delete trailing \n
        }
        inactiveActorArea.text = "$sb"
    }
}
/*

+-------------------------------------+
| Total: 3, Active: 2, Dormant: 1     | LBL
+-------------------------------------+
| Active actors                       | LBL
++-----------------------------------++
||43232949                           || TAR
||5333533 (Sigrid)                   ||
++-----------------------------------++
|=====================================| SPN
| Dormant actors                      | LBL
++-----------------------------------++
||12345678 (Cynthia)                 || TAR
++===================================++


*/